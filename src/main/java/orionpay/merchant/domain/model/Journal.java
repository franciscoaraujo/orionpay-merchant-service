package orionpay.merchant.domain.model;


import orionpay.merchant.domain.excepion.DomainException;

import java.time.LocalDateTime;
import java.util.UUID;

public record Journal(
        UUID id,
        String referenceType, // Ex: "TRANSACTION", "CHARGEBACK", "SETTLEMENT"
        UUID referenceId,     // ID da transação ou objeto que originou o lançamento
        String description,
        LocalDateTime createdAt
) {
    public Journal {
        if (referenceType == null || referenceId == null) {
            throw new DomainException("Tipo e ID de referência são obrigatórios para o Diário.");
        }
    }

    // Factory method para facilitar a criação
    public static Journal create(String type, UUID refId, String desc) {
        return new Journal(UUID.randomUUID(), type, refId, desc, LocalDateTime.now());
    }
}