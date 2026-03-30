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
    private final JpaDailySummaryRepository dailySummaryRepository; // Novo Repositório
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
            log.info("Processando motor de liquidação para transação: {}", event.transactionId());

            // 1. Idempotência no Redis
            Boolean isNewRequest = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "PROCESSING", LOCK_TTL);
            if (Boolean.FALSE.equals(isNewRequest)) {
                log.warn("Tentativa de processamento duplicado para transação: {}", event.transactionId());
                channel.basicAck(tag, false);
                return;
            }

            // 2. Cálculo Financeiro
            var pricing = pricingRepository.findCurrentPricing(event.merchantId(), event.productType())
                    .orElseThrow(() -> new DomainException("Precificação não encontrada para o produto: " + event.productType()));

            BigDecimal mdrRate = pricing.getMdrPercentage().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal mdrAmount = event.amount().multiply(mdrRate).setScale(4, RoundingMode.HALF_UP);
            BigDecimal netAmount = event.amount().subtract(mdrAmount).setScale(4, RoundingMode.HALF_UP);

            LocalDateTime expectedSettlementDate = event.occurredAt().plusDays(event.productType().getSettlementDays());

            // 3. Persistência em ops.settlement_entry
            SettlementEntryEntity settlementEntry = new SettlementEntryEntity();
            settlementEntry.setTransactionId(event.transactionId());
            settlementEntry.setMerchantId(event.merchantId());
            settlementEntry.setAmount(event.amount());
            settlementEntry.setMdrPercentage(pricing.getMdrPercentage());
            settlementEntry.setMdrAmount(mdrAmount);
            settlementEntry.setNetAmount(netAmount);
            settlementEntry.setExpectedSettlementDate(expectedSettlementDate);
            settlementEntry.setStatus(SettlementEntryEntity.SettlementStatus.PENDING);
            settlementRepository.save(settlementEntry);

            // 4. Escrituração Contábil (Ledger)
            processAccounting(event, netAmount, expectedSettlementDate);

            // 5. ATUALIZAÇÃO DO MODELO DE LEITURA (Sprint 1 - Read Model)
            dailySummaryRepository.upsertDailyMetrics(
                    event.merchantId(),
                    event.occurredAt().toLocalDate(),
                    event.amount(),
                    netAmount,
                    1, // approvedInc (Sempre 1 se o evento chegou aqui aprovado)
                    1  // totalInc
            );

            // 6. Finalização
            redisTemplate.opsForValue().set(idempotencyKey, "COMPLETED", LOCK_TTL);
            channel.basicAck(tag, false);
            log.info("Liquidação e Resumo Diário atualizados com sucesso. Transação: {}", event.transactionId());

        } catch (Exception e) {
            log.error("Erro fatal no motor de liquidação: {}. Causa: {}", event.transactionId(), e.getMessage());
            redisTemplate.delete(idempotencyKey);
            channel.basicNack(tag, false, false); 
        }
    }

    private void processAccounting(TransactionEvent event, BigDecimal netAmount, LocalDateTime availableAt) {
        LedgerAccount account = ledgerRepository.findByMerchantId(event.merchantId())
                .orElseThrow(() -> new DomainException("Conta contábil não encontrada para o lojista: " + event.merchantId()));

        account.applyEntry(netAmount, EntryType.CREDIT);
        ledgerRepository.saveAccount(account);

        ledgerRepository.saveEntry(
                account,
                netAmount,
                EntryType.CREDIT,
                "Liquidação de Venda - Transação: " + event.transactionId(),
                event.transactionId(),
                availableAt
        );
    }
}
