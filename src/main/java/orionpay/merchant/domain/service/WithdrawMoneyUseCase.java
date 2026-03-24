package orionpay.merchant.domain.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import orionpay.merchant.application.ports.output.PaymentServicePort;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.IdempotencyResult;
import orionpay.merchant.domain.model.LedgerAccount;
import orionpay.merchant.domain.model.enums.EntryType;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.WithdrawRequest;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.PayoutEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaPayoutRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.LedgerRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawMoneyUseCase {

    private final LedgerRepository ledgerRepository;
    private final JpaPayoutRepository payoutRepository;
    private final PaymentServicePort paymentService;
    private final IdempotencyService idempotencyService;

    @Transactional
    // Invalida o cache do Dashboard para este lojista após o saque, para refletir o novo saldo
    @CacheEvict(value = "dashboard_summary", key = "#request.merchantId")
    public void execute(WithdrawRequest request, String idempotencyKey) {
        // 1. Checagem de Idempotência
        IdempotencyResult cachedResult = idempotencyService.checkAndLock(idempotencyKey);
        if (cachedResult != null) {
            if ("SUCCESS".equals(cachedResult.getStatus())) {
                log.info("Requisição de saque idempotente (já processada). Chave: {}", idempotencyKey);
                return; // Já foi sucesso, retorna 202 Accepted sem fazer nada
            } else {
                throw new DomainException(cachedResult.getErrorMessage(), "IDEMPOTENCY_ERROR");
            }
        }

        try {
            log.info("Iniciando solicitação de saque para merchantId: {} | Valor: {}", request.merchantId(), request.amount());

            if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new DomainException("O valor do saque deve ser positivo.");
            }

            BigDecimal realAvailableBalance = ledgerRepository.findRealAvailableBalance(request.merchantId());
            
            if (realAvailableBalance.compareTo(request.amount()) < 0) {
                String errorMsg = "Saldo disponível insuficiente para saque imediato.";
                idempotencyService.saveError(idempotencyKey, errorMsg);
                log.warn("Bloqueio de Saque: Saldo insuficiente. Real: {} | Solicitado: {}", realAvailableBalance, request.amount());
                throw new DomainException(errorMsg);
            }

            LedgerAccount account = ledgerRepository.findByMerchantId(request.merchantId())
                    .orElseThrow(() -> new DomainException("Conta contábil não encontrada."));

            PayoutEntity payout = new PayoutEntity();
            payout.setMerchantId(request.merchantId());
            payout.setAmount(request.amount());
            payout.setPixKey(request.pixKey());
            payout.setStatus(PayoutEntity.PayoutStatus.PENDING);
            
            payoutRepository.save(payout);

            account.debit(request.amount()); 
            ledgerRepository.saveAccount(account);

            ledgerRepository.saveEntry(
                    account,
                    request.amount(),
                    EntryType.DEBIT,
                    "Saque Pix - ID: " + payout.getId(),
                    payout.getId(),
                    LocalDateTime.now()
            );

            boolean success = paymentService.processPixPayout(request.pixKey(), request.amount());

            if (success) {
                payout.setStatus(PayoutEntity.PayoutStatus.COMPLETED);
                payout.setCompletedAt(LocalDateTime.now());
                payoutRepository.save(payout);
                idempotencyService.saveSuccess(idempotencyKey, "Saque realizado com sucesso");
                log.info("Saque realizado com sucesso. PayoutId: {}", payout.getId());
            } else {
                String errorMsg = "Falha ao processar pagamento externo. Tente novamente.";
                idempotencyService.releaseLock(idempotencyKey); // Libera para retry
                throw new DomainException(errorMsg);
            }

        } catch (Exception e) {
            // Em caso de erro inesperado (ex: banco fora), libera o lock
            // Se for DomainException lançada acima, já salvamos erro, então talvez não devesse liberar...
            // Mas para simplificar: erro = libera lock ou salva erro.
            if (!(e instanceof DomainException)) {
                 idempotencyService.releaseLock(idempotencyKey);
            }
            log.error("Erro no saque: ", e);
            throw e;
        }
    }
}
