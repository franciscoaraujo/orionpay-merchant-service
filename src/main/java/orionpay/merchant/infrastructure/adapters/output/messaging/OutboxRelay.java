package orionpay.merchant.infrastructure.adapters.output.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import orionpay.merchant.domain.model.TransactionEvent;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.OutboxEventEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaOutboxRepository;
import orionpay.merchant.config.RabbitMQConfig; // CORRIGIDO: Novo pacote

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final JpaOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000) // Executa a cada 5 segundos
    @Transactional
    public void processOutbox() {
        List<OutboxEventEntity> pendingEvents = outboxRepository.findByStatus(OutboxEventEntity.OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) return;

        log.info("Processando {} eventos pendentes na Outbox...", pendingEvents.size());

        for (OutboxEventEntity event : pendingEvents) {
            try {
                TransactionEvent domainEvent = objectMapper.readValue(event.getPayload(), TransactionEvent.class);

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.TRANSACTION_AUTHORIZED_EXCHANGE,
                        RabbitMQConfig.SETTLEMENT_ROUTING_KEY,
                        domainEvent
                );

                event.setStatus(OutboxEventEntity.OutboxStatus.PROCESSED);
                event.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(event);

            } catch (Exception e) {
                log.error("Erro ao publicar evento da Outbox {}: {}", event.getId(), e.getMessage());
                event.setStatus(OutboxEventEntity.OutboxStatus.FAILED);
                outboxRepository.save(event);
            }
        }
    }
}
