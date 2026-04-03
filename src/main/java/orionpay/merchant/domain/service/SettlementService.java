package orionpay.merchant.domain.service;

import com.rabbitmq.client.Channel;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import orionpay.merchant.domain.excepion.BusinessResilienceException;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.LedgerAccount;
import orionpay.merchant.domain.model.TransactionEvent;
import orionpay.merchant.domain.model.enums.EntryType;
import orionpay.merchant.domain.model.enums.SettlementStatus;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.SettlementEntryEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaDailySummaryRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaSettlementEntryRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.LedgerRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.PricingRepository;
import orionpay.merchant.config.RabbitMQConfig; // CORRIGIDO: Novo pacote

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final JpaSettlementEntryRepository settlementRepository;
    private final JpaDailySummaryRepository dailySummaryRepository;
    private final PricingRepository pricingRepository;
    private final LedgerRepository ledgerRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final SettlementCalculator settlementCalculator = new SettlementCalculator();
    private final ObjectProvider<SettlementService> selfProvider;

    private static final String IDEMPOTENCY_PREFIX = "settlement:lock:";
    private static final Duration LOCK_TTL = Duration.ofHours(48);

    @RabbitListener(queues = RabbitMQConfig.SETTLEMENT_PROCESS_QUEUE, ackMode = "MANUAL")
    @Transactional(noRollbackFor = BusinessResilienceException.class)
    @Bulkhead(name = "settlementEngine", type = Bulkhead.Type.SEMAPHORE)
    public void processTransactionSettlement(TransactionEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        String idempotencyKey = IDEMPOTENCY_PREFIX + event.transactionId();

        try {
            log.info("Iniciando orquestração de liquidação | Transação: {}", event.transactionId());

            Boolean isNewRequest = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "PROCESSING", LOCK_TTL);
            if (Boolean.FALSE.equals(isNewRequest)) {
                log.info("Idempotência acionada (Redis): Transação {} já processada ou em andamento. Enviando ACK.", event.transactionId());
                channel.basicAck(tag, false);
                return;
            }

            var pricing = pricingRepository.findCurrentPricing(event.merchantId(), event.productType())
                    .orElseThrow(() -> new BusinessResilienceException("Precificação não encontrada p/ " + event.productType(), "CONFIG_PENDING"));

            List<SettlementCalculator.CalculatedInstallment> calculatedInstallments = 
                settlementCalculator.calculate(event, pricing.getMdrPercentage());

            selfProvider.getIfAvailable().ensureLedgerAccountExists(event.merchantId());

            for (SettlementCalculator.CalculatedInstallment installment : calculatedInstallments) {
                selfProvider.getIfAvailable().processSingleInstallment(event, installment, pricing.getMdrPercentage(), calculatedInstallments.size());
            }

            dailySummaryRepository.upsertDailyMetrics(
                    event.merchantId(),
                    event.occurredAt().toLocalDate(),
                    event.amount(),
                    calculatedInstallments.stream().map(SettlementCalculator.CalculatedInstallment::getNetAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                    1,
                    1
            );

            redisTemplate.opsForValue().set(idempotencyKey, "COMPLETED", LOCK_TTL);
            channel.basicAck(tag, false);
            log.info("Orquestração de liquidação finalizada com sucesso para transação: {}", event.transactionId());

        } catch (BusinessResilienceException e) {
            log.error("Configuração Pendente: {}. O registro será mantido como PENDING para intervenção manual.", e.getMessage());
            channel.basicAck(tag, false);
        } catch (DataIntegrityViolationException e) {
            log.info("Idempotência acionada (DB): Transação {} já processada. Ignorando duplicidade e enviando ACK.", event.transactionId());
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Erro fatal na orquestração de liquidação: {}. Causa: {}", event.transactionId(), e.getMessage());
            redisTemplate.delete(idempotencyKey);
            channel.basicNack(tag, false, false);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureLedgerAccountExists(UUID merchantId) {
        if (ledgerRepository.findByMerchantId(merchantId).isPresent()) {
            return;
        }

        try {
            log.info("Tentando criar conta contábil p/ lojista: {}", merchantId);
            LedgerAccount newAccount = LedgerAccount.create(
                    UUID.randomUUID(),
                    merchantId,
                    "ACC-" + merchantId.toString().substring(0, 8).toUpperCase(),
                    BigDecimal.ZERO
            );
            ledgerRepository.saveAccount(newAccount);
        } catch (DataIntegrityViolationException e) {
            log.info("Idempotência acionada (Ledger): Conta já existe para o lojista {}.", merchantId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleInstallment(
            TransactionEvent event, 
            SettlementCalculator.CalculatedInstallment installment, 
            BigDecimal mdrPercentage,
            int totalInstallments
    ) {
        Optional<SettlementEntryEntity> existingEntry = settlementRepository.findByTransactionIdAndInstallmentNumber(
                event.transactionId(), installment.getInstallmentNumber());

        if (existingEntry.isPresent()) {
            SettlementEntryEntity entry = existingEntry.get();
            if (entry.getStatus() != SettlementStatus.PENDING) {
                log.info("Ignorando processamento: Parcela {} já em estado {}.", installment.getInstallmentNumber(), entry.getStatus());
                return;
            }
            selfProvider.getIfAvailable().callAccountingWithResilience(event, entry, installment);
            return;
        }

        SettlementEntryEntity settlementEntry = SettlementEntryEntity.builder()
                .transactionId(event.transactionId())
                .merchantId(event.merchantId())
                .installmentNumber(installment.getInstallmentNumber())
                .totalInstallments(totalInstallments)
                .terminalId(event.terminalId())
                .amount(installment.getGrossAmount())
                .originalAmount(event.amount())
                .mdrPercentage(mdrPercentage)
                .mdrAmount(installment.getMdrAmount())
                .netAmount(installment.getNetAmount())
                .expectedSettlementDate(installment.getExpectedSettlementDate())
                .status(SettlementStatus.PENDING)
                .processedAt(LocalDateTime.now())
                .build();
        
        try {
            settlementRepository.saveAndFlush(settlementEntry);
            selfProvider.getIfAvailable().callAccountingWithResilience(event, settlementEntry, installment);
        } catch (DataIntegrityViolationException e) {
            log.info("Idempotência acionada (DB): Parcela {} da transação {} já processada. Ignorando duplicidade.", 
                    installment.getInstallmentNumber(), event.transactionId());
        }
    }

    @CircuitBreaker(name = "ledgerCircuitBreaker", fallbackMethod = "fallbackLedger")
    @Bulkhead(name = "ledgerBulkhead", fallbackMethod = "fallbackLedger")
    public void callAccountingWithResilience(TransactionEvent event, SettlementEntryEntity entry, SettlementCalculator.CalculatedInstallment installment) {
        processAccounting(event, installment.getNetAmount(), installment.getExpectedSettlementDate(), installment.getInstallmentNumber());
        
        entry.setStatus(SettlementStatus.SCHEDULED);
        settlementRepository.save(entry);
        log.info("Parcela {} promovida para SCHEDULED via Ledger.", installment.getInstallmentNumber());
    }

    public void fallbackLedger(TransactionEvent event, SettlementEntryEntity entry, SettlementCalculator.CalculatedInstallment installment, Throwable t) {
        log.warn("Circuito Aberto ou Falha Temporária para Ledger. Parcela {} mantida PENDING para cura posterior. Motivo: {}", 
                 installment.getInstallmentNumber(), t.getMessage());
    }

    private void processAccounting(TransactionEvent event, BigDecimal netAmount, LocalDateTime availableAt, int installmentNumber) {
        LedgerAccount account = ledgerRepository.findByMerchantId(event.merchantId())
                .orElseThrow(() -> new BusinessResilienceException("Conta contábil não encontrada p/ lojista " + event.merchantId(), "LEDGER_ACCOUNT_MISSING"));

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
}
