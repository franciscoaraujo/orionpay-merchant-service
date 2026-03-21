package orionpay.merchant.domain.model;

import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.enums.TransactionStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record TransactionEvent(
        UUID id,
        UUID transactionId,
        TransactionStatus type,
        String description,
        LocalDateTime occurredAt,

        Map<String, String> metadata
) {
    public TransactionEvent {
        if (transactionId == null || type == null) {
            throw new DomainException("Dados do evento incompletos.");
        }
    }
}