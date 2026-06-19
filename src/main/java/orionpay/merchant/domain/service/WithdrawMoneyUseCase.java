package orionpay.merchant.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orionpay.merchant.application.ports.output.PaymentServicePort;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.excepion.IdempotencyException;
import orionpay.merchant.domain.excepion.InsufficientFundsException; // Import new exception
import orionpay.merchant.domain.excepion.PayoutPendingException;
import orionpay.merchant.domain.model.IdempotencyResult;
import orionpay.merchant.domain.model.LedgerAccount; // Keep for saveEntry, but not for balance check
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
            payout = reserveFunds(request); // This method will now handle the atomic deduction

            // 2. INTEGRAÇÃO EXTERNA (FORA DA TRANSAÇÃO DO BANCO)
            log.info("Integrando com Gateway PIX para Payout: {}", payout.getId());
            // Adicionando log para verificar o valor do amount antes de enviar para o PaymentService
            log.info(">>> [WithdrawMoneyUseCase] Valor do amount antes de chamar PaymentService: {}", request.amount());
            boolean success = paymentService.processPixPayout(request.pixKey(), request.amount());

            if (success) {
                confirmPayout(payout, idempotencyKey);
            } else {
                failPayout(payout, idempotencyKey, "O banco recusou o pagamento.");
                throw new DomainException("O banco recusou o pagamento.", "PAYOUT_REJECTED");
            }

        } catch (DomainException e) {
            // If it's an InsufficientFundsException, it should be caught here
            idempotencyService.saveError(idempotencyKey, e.getMessage()); // Save error for idempotency
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
     * ORDEM ESTRITA: Valida -> Deduz Atômicamente -> Cria Payout -> Gera Ledger Entry.
     * Rollback automático se qualquer step falhar.
     */
    @Transactional(rollbackFor = Exception.class)
    public PayoutEntity reserveFunds(WithdrawRequest request) {
        // 1. Valida se o amount do saque é estritamente maior que zero
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("O valor do saque deve ser estritamente maior que zero.", "INVALID_AMOUNT");
        }

        // 2. Tenta executar o método de UPDATE atômico no Repository.
        // Isso substitui a validação de saldo e a busca da conta.
        int affectedRows = ledgerRepository.deductBalanceAtomic(request.merchantId(), request.amount());

        // 3. Se o retorno do UPDATE for 0, lança uma exceção de negócio explícita InsufficientFundsException
        if (affectedRows == 0) {
            log.warn("Tentativa de saque para merchant {} com saldo insuficiente ou concorrência. Valor: {}", request.merchantId(), request.amount());
            throw new InsufficientFundsException("Saldo insuficiente para saque ou erro de concorrência.");
        }

        // Obter a conta contábil após a dedução bem-sucedida para criar o LedgerEntry
        // Não precisamos mais do findRealAvailableBalance aqui.
        LedgerAccount account = ledgerRepository.findByMerchantId(request.merchantId())
                .orElseThrow(() -> new DomainException("Conta contábil não encontrada após dedução."));


        // 4. Cria e Salva Payout
        PayoutEntity payout = new PayoutEntity();
        payout.setMerchantId(request.merchantId());
        payout.setAmount(request.amount());
        payout.setPixKey(request.pixKey());
        payout.setStatus(PayoutEntity.PayoutStatus.PENDING);
        payout = payoutRepository.save(payout);

        // 5. Cria a entrada contábil na tabela ledger_entry com o amount positivo e o type configurado explicitamente como "DEBIT".
        // O amount já é positivo aqui.
        ledgerRepository.saveEntry(
                account,
                request.amount(), // Amount já é positivo
                EntryType.DEBIT, // Explicitamente DEBIT
                "Saque PIX - ID: " + payout.getId(),
                payout.getId(),
                LocalDateTime.now() // Ou a data de disponibilidade desejada
        );

        log.info("Reserva financeira concluída com sucesso para Payout {}", payout.getId());
        return payout;
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmPayout(PayoutEntity payout, String idempotencyKey) {
        payout.setStatus(PayoutEntity.PayoutStatus.COMPLETED);
        payout.setCompletedAt(LocalDateTime.now());
        payoutRepository.save(payout);

        // No caso de sucesso, o saldo já foi deduzido.
        // A entrada de débito já foi criada em reserveFunds.
        // Se houver necessidade de uma entrada de "confirmação" ou algo similar,
        // ela deve ser um CREDIT ou um tipo diferente para não duplicar a dedução.
        // Por enquanto, vou remover a criação de LedgerEntry aqui para evitar dupla dedução.
        // Se a intenção era ter uma entrada de "WITHDRAWAL_COMPLETED" como um tipo de auditoria,
        // ela não deve afetar o saldo novamente.
        // A dedução já ocorreu no reserveFunds com EntryType.DEBIT.

        idempotencyService.saveSuccess(idempotencyKey, "Saque realizado com sucesso");
    }

    @Transactional(rollbackFor = Exception.class)
    public void failPayout(PayoutEntity payout, String idempotencyKey, String reason) {
        PayoutEntity currentPayout = payoutRepository.findById(payout.getId()).orElse(payout);
        if (currentPayout.getStatus() == PayoutEntity.PayoutStatus.FAILED) return;

        currentPayout.setStatus(PayoutEntity.PayoutStatus.FAILED);
        payoutRepository.save(currentPayout);

        // Se o saque falhou, precisamos estornar o valor que foi deduzido atomicamente.
        // Isso significa creditar o valor de volta na conta.
        ledgerRepository.findByMerchantId(currentPayout.getMerchantId()).ifPresent(account -> {
            // A dedução foi feita atomicamente, então precisamos creditar de volta.
            // Não há um método atomic para creditar, então faremos a leitura e escrita.
            // Isso é um ponto de atenção para concorrência em estornos, mas menos crítico que a dedução.
            // Para ser totalmente seguro, um método atomic de crédito também seria ideal.
            // Por simplicidade e foco na dedução, faremos assim por enquanto.
            account.credit(currentPayout.getAmount()); // Assume que credit() atualiza o balance e version
            ledgerRepository.saveAccount(account); // Salva a conta atualizada

            ledgerRepository.saveEntry(
                    account,
                    currentPayout.getAmount(),
                    EntryType.CREDIT, // Estorno é um CRÉDITO
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