package orionpay.merchant.domain.service;

import com.rabbitmq.client.Channel;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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
import orionpay.merchant.infrastructure.config.RabbitMQConfig;

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

    private static final String IDEMPOTENCY_PREFIX = "settlement:lock:";
    private static final Duration LOCK_TTL = Duration.ofHours(48);

    @RabbitListener(queues = RabbitMQConfig.SETTLEMENT_PROCESS_QUEUE, ackMode = "MANUAL")
    @Transactional
    @Bulkhead(name = "settlementEngine", type = Bulkhead.Type.SEMAPHORE)
    public void processTransactionSettlement(TransactionEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        String idempotencyKey = IDEMPOTENCY_PREFIX + event.transactionId();

        try {
            log.info("Iniciando orquestração de liquidação | Transação: {}", event.transactionId());

            // 1. Idempotência Distribuída (Redis Lock)
            Boolean isNewRequest = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "PROCESSING", LOCK_TTL);
            if (Boolean.FALSE.equals(isNewRequest)) {
                log.warn("Ignorando processamento: Mensagem já em processamento ou concluída para transação: {}", event.transactionId());
                channel.basicAck(tag, false);
                return;
            }

            // 2. Busca de Precificação
            var pricing = pricingRepository.findCurrentPricing(event.merchantId(), event.productType())
                    .orElseThrow(() -> new DomainException("Precificação não encontrada: " + event.productType()));

            // 3. Explosão das Parcelas
            List<SettlementCalculator.CalculatedInstallment> calculatedInstallments = 
                settlementCalculator.calculate(event, pricing.getMdrPercentage());

            // 4. Processamento Granular por Parcela
            for (SettlementCalculator.CalculatedInstallment installment : calculatedInstallments) {
                processSingleInstallment(event, installment, pricing.getMdrPercentage(), calculatedInstallments.size());
            }

            // 5. Atualização do Dashboard
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
            log.info("Liquidação finalizada com sucesso para transação: {}", event.transactionId());

        } catch (Exception e) {
            log.error("Erro fatal no motor de liquidação: {}. Causa: {}", event.transactionId(), e.getMessage());
            redisTemplate.delete(idempotencyKey);
            channel.basicNack(tag, false, false);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleInstallment(
            TransactionEvent event, 
            SettlementCalculator.CalculatedInstallment installment, 
            BigDecimal mdrPercentage,
            int totalInstallments
    ) {
        // --- 1. CHECK-AND-SKIP (Idempotência Lógica) ---
        Optional<SettlementEntryEntity> existingEntry = settlementRepository.findByTransactionIdAndInstallmentNumber(
                event.transactionId(), installment.getInstallmentNumber());

        if (existingEntry.isPresent()) {
            SettlementEntryEntity entry = existingEntry.get();
            
            // SE status em (SCHEDULED, SETTLED, PAID, ANTICIPATED) -> SKIP
            if (isFinalOrScheduled(entry.getStatus())) {
                log.info("Ignorando processamento: Transação {} Parcela {} já está em estado {}.", 
                        event.transactionId(), installment.getInstallmentNumber(), entry.getStatus());
                return;
            }
            
            // SE status é PENDING -> Tentamos a 'cura' (processamento contábil)
            log.info("Executando cura para registro PENDING: Transação {} Parcela {}.", 
                    event.transactionId(), installment.getInstallmentNumber());
            performAccountingAndPromote(event, entry, installment);
            return;
        }

        // --- 2. CRIAÇÃO DO REGISTRO (Draft PENDING) ---
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
        
        settlementRepository.saveAndFlush(settlementEntry);
        performAccountingAndPromote(event, settlementEntry, installment);
    }

    private void performAccountingAndPromote(TransactionEvent event, SettlementEntryEntity entry, SettlementCalculator.CalculatedInstallment installment) {
        try {
            processAccounting(event, installment.getNetAmount(), installment.getExpectedSettlementDate(), installment.getInstallmentNumber());
            
            entry.setStatus(SettlementStatus.SCHEDULED);
            settlementRepository.save(entry);
            log.info("Parcela {} promovida para SCHEDULED com sucesso.", installment.getInstallmentNumber());

        } catch (Exception e) {
            log.error("Falha no Ledger para parcela {}. Mantendo PENDING p/ reconciliação futura. Erro: {}", 
                      installment.getInstallmentNumber(), e.getMessage());
        }
    }

    private boolean isFinalOrScheduled(SettlementStatus status) {
        return status == SettlementStatus.SCHEDULED || 
               status == SettlementStatus.SETTLED || 
               status == SettlementStatus.PAID || 
               status == SettlementStatus.ANTICIPATED;
    }

    private void processAccounting(TransactionEvent event, BigDecimal netAmount, LocalDateTime availableAt, int installmentNumber) {
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
}
