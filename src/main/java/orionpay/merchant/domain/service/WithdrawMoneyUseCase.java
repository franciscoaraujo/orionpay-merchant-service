package orionpay.merchant.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orionpay.merchant.application.ports.output.PaymentServicePort;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.excepion.IdempotencyException;
import orionpay.merchant.domain.excepion.PayoutPendingException;
import orionpay.merchant.domain.model.IdempotencyResult;
import orionpay.merchant.domain.model.LedgerAccount;
import orionpay.merchant.domain.model.enums.EntryType;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.WithdrawRequest;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.PayoutEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaPayoutRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.LedgerRepository;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawMoneyUseCase {

    private final LedgerRepository ledgerRepository;
    private final JpaPayoutRepository payoutRepository;
    private final PaymentServicePort paymentService;
    private final IdempotencyService idempotencyService;

    @CacheEvict(value = "dashboard_summary", key = "#request.merchantId")
    public void execute(WithdrawRequest request, String idempotencyKey) {
        checkActiveWithdrawals(request.merchantId());
        
        IdempotencyResult cachedResult = idempotencyService.checkAndLock(idempotencyKey);
        if (cachedResult != null) {
            if ("SUCCESS".equals(cachedResult.getStatus())) return;
            throw new IdempotencyException(cachedResult.getErrorMessage());
        }

        PayoutEntity payout = null;
        try {
            log.info("Iniciando fluxo de saque: Merchant {} | Valor {}", request.merchantId(), request.amount());

            // 1. FASE DE RESERVA (ATÔMICA)
            // Se algo falhar aqui dentro, o Payout não será criado no banco.
            payout = reserveFunds(request);

            // 2. INTEGRAÇÃO EXTERNA (FORA DA TRANSAÇÃO DO BANCO)
            log.info("Integrando com Gateway PIX para Payout: {}", payout.getId());
            boolean success = paymentService.processPixPayout(request.pixKey(), request.amount());

            if (success) {
                confirmPayout(payout, idempotencyKey);
            } else {
                failPayout(payout, idempotencyKey, "O banco recusou o pagamento.");
                throw new DomainException("O banco recusou o pagamento.", "PAYOUT_REJECTED");
            }

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro crítico no processo de saque para o Merchant {}: {}", request.merchantId(), e.getMessage());
            handleWithdrawalError(payout, idempotencyKey, e);
        }
    }

    private void handleWithdrawalError(PayoutEntity payout, String idempotencyKey, Exception e) {
        String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        boolean isTimeout = e.getCause() instanceof SocketTimeoutException || 
                            errorMsg.contains("timeout") || 
                            errorMsg.contains("504");

        if (isTimeout && payout != null) {
            log.error("TIMEOUT detectado no PIX (Payout {}). Registro p/ Reconciliação.", payout.getId());
            updatePayoutStatus(payout, PayoutEntity.PayoutStatus.WAITING_BANK_CONFIRMATION);
            throw new PayoutPendingException("Seu saque está sendo processado pelo banco.");
        }

        if (payout != null) {
            failPayout(payout, idempotencyKey, e.getMessage());
        } else {
            idempotencyService.releaseLock(idempotencyKey);
        }
        throw new DomainException("Falha no saque: " + e.getMessage(), "PAYOUT_ERROR");
    }

    private void checkActiveWithdrawals(UUID merchantId) {
        boolean hasPending = payoutRepository.existsByMerchantIdAndStatusIn(merchantId, 
                java.util.List.of(PayoutEntity.PayoutStatus.PENDING, PayoutEntity.PayoutStatus.WAITING_BANK_CONFIRMATION));
        if (hasPending) {
            throw new DomainException("Existe um saque em andamento.", "ACTIVE_WITHDRAWAL");
        }
    }

    /**
     * ORDEM ESTRITA: Valida -> Cria Payout -> Gera Ledger.
     * Rollback automático se qualquer step falhar.
     */
    @Transactional(rollbackFor = Exception.class)
    public PayoutEntity reserveFunds(WithdrawRequest request) {
        // 1. Valida Saldo
        BigDecimal available = ledgerRepository.findRealAvailableBalance(request.merchantId());
        if (available.compareTo(request.amount()) < 0) {
            log.warn("Saldo insuficiente para merchant {}: {}", request.merchantId(), available);
            throw new DomainException("Saldo insuficiente para saque.");
        }

        LedgerAccount account = ledgerRepository.findByMerchantId(request.merchantId())
                .orElseThrow(() -> new DomainException("Conta contábil não encontrada."));

        // 2. Cria e Salva Payout (Ainda @Transactional, não commitado no banco físico)
        PayoutEntity payout = new PayoutEntity();
        payout.setMerchantId(request.merchantId());
        payout.setAmount(request.amount());
        payout.setPixKey(request.pixKey());
        payout.setStatus(PayoutEntity.PayoutStatus.PENDING);
        payout = payoutRepository.save(payout);

        // 3. Gera e Salva Ledger Entry
        // Se este step falhar por Constraint (ex: Journal nulo), o Payout acima sofrerá ROLLBACK
        ledgerRepository.saveEntry(
                account,
                request.amount(),
                EntryType.WITHDRAWAL_HOLD,
                "Reserva de Saque PIX - ID: " + payout.getId(),
                payout.getId(),
                LocalDateTime.now()
        );

        log.info("Reserva financeira concluída com sucesso para Payout {}", payout.getId());
        return payout;
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmPayout(PayoutEntity payout, String idempotencyKey) {
        payout.setStatus(PayoutEntity.PayoutStatus.COMPLETED);
        payout.setCompletedAt(LocalDateTime.now());
        payoutRepository.save(payout);

        LedgerAccount account = ledgerRepository.findByMerchantId(payout.getMerchantId()).get();
        ledgerRepository.saveEntry(
                account,
                payout.getAmount(),
                EntryType.WITHDRAWAL_COMPLETED,
                "Saque PIX Confirmado - ID: " + payout.getId(),
                payout.getId(),
                LocalDateTime.now()
        );

        idempotencyService.saveSuccess(idempotencyKey, "Saque realizado com sucesso");
    }

    @Transactional(rollbackFor = Exception.class)
    public void failPayout(PayoutEntity payout, String idempotencyKey, String reason) {
        PayoutEntity currentPayout = payoutRepository.findById(payout.getId()).orElse(payout);
        if (currentPayout.getStatus() == PayoutEntity.PayoutStatus.FAILED) return;

        currentPayout.setStatus(PayoutEntity.PayoutStatus.FAILED);
        payoutRepository.save(currentPayout);

        ledgerRepository.findByMerchantId(currentPayout.getMerchantId()).ifPresent(account -> {
            account.credit(currentPayout.getAmount());
            ledgerRepository.saveAccount(account);

            ledgerRepository.saveEntry(
                    account,
                    currentPayout.getAmount(),
                    EntryType.WITHDRAWAL_REVERSAL,
                    "Estorno de Saque (Falha) - Ref: " + currentPayout.getId(),
                    currentPayout.getId(),
                    LocalDateTime.now()
            );
        });

        idempotencyService.saveError(idempotencyKey, reason);
    }

    @Transactional
    public void updatePayoutStatus(PayoutEntity payout, PayoutEntity.PayoutStatus status) {
        payout.setStatus(status);
        payoutRepository.save(payout);
    }
}
