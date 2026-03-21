package orionpay.merchant.domain.model;

import orionpay.merchant.domain.excepion.DomainException;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLog(
        UUID id,
        String userEmail,
        String action,
        String resource,
        String details,
        String ipAddress,

        LocalDateTime timestamp
) {
    public AuditLog {
        if (userEmail == null || action == null) {
            throw new DomainException("Dados de auditoria incompletos.");
        }
    }
}