package orionpay.merchant.domain.service;

import com.rabbitmq.client.Channel;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.LedgerAccount;
import orionpay.merchant.domain.model.TransactionEvent;
import orionpay.merchant.domain.model.enums.EntryType;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.SettlementEntryEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaDailySummaryRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaSettlementEntryRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.LedgerRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.PricingRepository;
import orionpay.merchant.infrastructure.config.RabbitMQConfig;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final JpaSettlementEntryRepository settlementRepository;
    private final JpaDailySummaryRepository dailySummaryRepository;
    private final PricingRepository pricingRepository;
    private final LedgerRepository ledgerRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String IDEMPOTENCY_PREFIX = "settlement:lock:";
    private static final Duration LOCK_TTL = Duration.ofHours(48);

    @RabbitListener(queues = RabbitMQConfig.SETTLEMENT_PROCESS_QUEUE, ackMode = "MANUAL")
    @Transactional
    @Bulkhead(name = "settlementEngine", type = Bulkhead.Type.SEMAPHORE)
    public void processSettlement(TransactionEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        String idempotencyKey = IDEMPOTENCY_PREFIX + event.transactionId();

        try {
            log.info("Iniciando motor de liquidação | Transação: {} | Parcelas: {}", 
                    event.transactionId(), event.installments());

            // 1. Idempotência por Mensagem Inteira (Redis)
            Boolean isNewRequest = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "PROCESSING", LOCK_TTL);
            if (Boolean.FALSE.equals(isNewRequest)) {
                log.warn("Processamento duplicado detectado para transação: {}", event.transactionId());
                channel.basicAck(tag, false);
                return;
            }

            // 2. Busca de Precificação
            var pricing = pricingRepository.findCurrentPricing(event.merchantId(), event.productType())
                    .orElseThrow(() -> new DomainException("Precificação não encontrada: " + event.productType()));

            // 3. Cálculos de Rateio Proporcional (MDR e Valor Líquido)
            BigDecimal mdrRate = pricing.getMdrPercentage().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal totalMdrAmount = event.amount().multiply(mdrRate).setScale(4, RoundingMode.HALF_UP);
            BigDecimal totalNetAmount = event.amount().subtract(totalMdrAmount).setScale(4, RoundingMode.HALF_UP);

            int installmentsCount = (event.installments() != null && event.installments() > 0) ? event.installments() : 1;

            // 4. Explosão das Parcelas (Atomic Loop)
            for (int i = 1; i <= installmentsCount; i++) {
                
                // Idempotência Granular por Parcela (Banco)
                if (settlementRepository.existsByTransactionIdAndInstallmentNumber(event.transactionId(), i)) {
                    log.debug("Parcela {} da transação {} já foi processada. Ignorando iteração.", i, event.transactionId());
                    continue;
                }

                BigDecimal installmentAmount = event.amount().divide(BigDecimal.valueOf(installmentsCount), 4, RoundingMode.HALF_UP);
                BigDecimal installmentNetAmount = totalNetAmount.divide(BigDecimal.valueOf(installmentsCount), 4, RoundingMode.HALF_UP);
                BigDecimal installmentMdrAmount = totalMdrAmount.divide(BigDecimal.valueOf(installmentsCount), 4, RoundingMode.HALF_UP);

                // Cálculo D+30 Progressivo para a Parcela i
                LocalDateTime expectedDate = event.occurredAt().plusDays(30L * i);

                SettlementEntryEntity settlementEntry = SettlementEntryEntity.builder()
                        .transactionId(event.transactionId())
                        .merchantId(event.merchantId())
                        .installmentNumber(i)
                        .terminalId(event.terminalId())
                        .amount(installmentAmount)
                        .originalAmount(event.amount())
                        .mdrPercentage(pricing.getMdrPercentage())
                        .mdrAmount(installmentMdrAmount)
                        .netAmount(installmentNetAmount)
                        .expectedSettlementDate(expectedDate)
                        .status(SettlementEntryEntity.SettlementStatus.SETTLED) // Mapeado como SCHEDULED/AGENDADO no frontend
                        .processedAt(LocalDateTime.now())
                        .build();
                
                settlementRepository.save(settlementEntry);

                // 5. Vínculo Contábil (Ledger) com correlation id para a parcela
                processAccounting(event, installmentNetAmount, expectedDate, i);
            }

            // 6. Atualização do Read Model para Dashboard
            dailySummaryRepository.upsertDailyMetrics(
                    event.merchantId(),
                    event.occurredAt().toLocalDate(),
                    event.amount(),
                    totalNetAmount,
                    1,
                    1
            );

            redisTemplate.opsForValue().set(idempotencyKey, "COMPLETED", LOCK_TTL);
            channel.basicAck(tag, false);
            log.info("Liquidação finalizada com sucesso. Parcelas processadas: {}", installmentsCount);

        } catch (Exception e) {
            log.error("Erro fatal na liquidação parcelada: {}. Causa: {}", event.transactionId(), e.getMessage());
            redisTemplate.delete(idempotencyKey);
            channel.basicNack(tag, false, false);
        }
    }

    private void processAccounting(TransactionEvent event, BigDecimal netAmount, LocalDateTime availableAt, int installmentNumber) {
        LedgerAccount account = ledgerRepository.findByMerchantId(event.merchantId())
                .orElseThrow(() -> new DomainException("Conta contábil não encontrada para o lojista: " + event.merchantId()));

        account.applyEntry(netAmount, EntryType.CREDIT);
        ledgerRepository.saveAccount(account);

        ledgerRepository.saveEntry(
                account,
                netAmount,
                EntryType.CREDIT,
                "Liquidação Parcela " + installmentNumber + " - Transação: " + event.transactionId(),
                event.transactionId(), // Correlation ID
                availableAt
        );
    }
}
