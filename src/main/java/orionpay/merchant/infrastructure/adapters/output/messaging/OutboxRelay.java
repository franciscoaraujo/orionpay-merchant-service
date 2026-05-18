package orionpay.merchant.infrastructure.adapters.output.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import orionpay.merchant.application.ports.output.TelemetryPublisherPort;
import orionpay.merchant.domain.model.TransactionEvent;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.OutboxEventEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaOutboxRepository;
import orionpay.merchant.config.RabbitMQConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final JpaOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final TelemetryPublisherPort telemetryPublisher; // 1. INJEÇÃO DA TELEMETRIA

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void processOutbox() {
        List<OutboxEventEntity> pendingEvents = outboxRepository.findByStatus(OutboxEventEntity.OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Processando {} eventos pendentes na Outbox...", pendingEvents.size());

        for (OutboxEventEntity event : pendingEvents) {
            try {
                TransactionEvent domainEvent = objectMapper.readValue(event.getPayload(), TransactionEvent.class);

                // Publica o evento principal para a fila de liquidação
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.TRANSACTION_AUTHORIZED_EXCHANGE,
                        RabbitMQConfig.SETTLEMENT_ROUTING_KEY,
                        domainEvent
                );

                // Marca o evento como processado no banco de dados
                event.setStatus(OutboxEventEntity.OutboxStatus.PROCESSED);
                event.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(event);

                // --- INÍCIO DA INTEGRAÇÃO DA TELEMETRIA ---
                // 2. Dispara o evento de telemetria (fire-and-forget)
                // A implementação do publisher já garante a resiliência (ponto 3)
                telemetryPublisher.publishTelemetry(
                        "OUTBOX-RELAY",
                        "EVENT_PUBLISHED",
                        Map.of("transactionId", domainEvent.transactionId())
                );
                // --- FIM DA INTEGRAÇÃO DA TELEMETRIA ---

            } catch (Exception e) {
                log.error("Erro ao publicar evento da Outbox {}: {}", event.getId(), e.getMessage());
                event.setStatus(OutboxEventEntity.OutboxStatus.FAILED);
                outboxRepository.save(event);
            }
        }
    }
}
