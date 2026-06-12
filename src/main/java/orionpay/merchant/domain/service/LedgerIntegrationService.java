package orionpay.merchant.domain.service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.LedgerAccount;
import orionpay.merchant.domain.model.TransactionEvent;
import orionpay.merchant.domain.model.enums.EntryType;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.LedgerRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerIntegrationService {

    private final LedgerRepository ledgerRepository;

    @CircuitBreaker(name = "ledgerCircuitBreaker", fallbackMethod = "fallbackLedger")
    @Bulkhead(name = "ledgerBulkhead")
    public void processAccounting(TransactionEvent event, BigDecimal netAmount, LocalDateTime availableAt, int installmentNumber) {
        log.info("Tentando escrituração no Ledger p/ transação {} parcela {}", event.transactionId(), installmentNumber);

        LedgerAccount account = ledgerRepository.findByMerchantId(event.merchantId())
                .orElseGet(() -> {
                    log.info("Criando conta contábil automática p/ lojista: {}", event.merchantId());
                    LedgerAccount newAccount = LedgerAccount.create(
                            UUID.randomUUID(),
                            event.merchantId(),
                            "ACC-" + event.merchantId().toString().substring(0, 8).toUpperCase(),
                            BigDecimal.ZERO
                    );
                    ledgerRepository.saveAccount(newAccount);
                    return newAccount;
                });

        account.applyEntry(netAmount, EntryType.CREDIT);
        ledgerRepository.saveAccount(account);

        ledgerRepository.saveEntry(
                account,
                netAmount,
                EntryType.CREDIT,
                "Liquidação Parcela " + installmentNumber + " - Transação: " + event.transactionId(),
                event.transactionId(),
                availableAt
        );
    }

    /**
     * FALLBACK: Este método é chamado se o Circuito estiver ABERTO ou se ocorrer um erro durante a chamada.
     * Ele lança uma exceção customizada que será capturada pelo SettlementService
     * para manter o status PENDING.
     */
    public void fallbackLedger(TransactionEvent event, BigDecimal netAmount, LocalDateTime availableAt, int installmentNumber, Throwable t) {
        log.error("RESILIÊNCIA: Falha na integração com Ledger. Causa: {}. Registro mantido como PENDING p/ recuperação futura.", t.getMessage());
        throw new DomainException("Circuit Breaker ou Bulkhead ativo para Ledger.", "LEDGER_UNAVAILABLE");
    }
}
