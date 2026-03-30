package orionpay.merchant.domain.model;

import lombok.Builder;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.enums.TransactionStatus;
import orionpay.merchant.domain.model.enums.ProductType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Builder
public record TransactionEvent(
        UUID id,
        UUID transactionId,
        UUID merchantId,
        BigDecimal amount,
        ProductType productType,
        TransactionStatus status,
        String description,
        LocalDateTime occurredAt,
        Map<String, String> metadata
) {
    public TransactionEvent {
        if (transactionId == null || status == null) {
            throw new DomainException("Dados do evento incompletos.");
        }
    }
}
