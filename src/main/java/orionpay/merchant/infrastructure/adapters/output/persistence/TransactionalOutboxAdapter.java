package orionpay.merchant.infrastructure.adapters.output.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import orionpay.merchant.application.ports.output.EventPublisherPort;
import orionpay.merchant.domain.model.TransactionEvent;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.OutboxEventEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaOutboxRepository;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionalOutboxAdapter implements EventPublisherPort {

    private final JpaOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @SneakyThrows
    public void publish(TransactionEvent event) {
        OutboxEventEntity outboxEvent = OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(event.transactionId())
                .type("TRANSACTION_AUTHORIZED")
                .payload(objectMapper.writeValueAsString(event))
                .status(OutboxEventEntity.OutboxStatus.PENDING)
                .build();

        outboxRepository.save(outboxEvent);
    }
}
