package orionpay.merchant.infrastructure.adapters.input.rest.dto;


import orionpay.merchant.domain.model.Transaction;
import orionpay.merchant.domain.model.enums.ProductType;
import orionpay.merchant.domain.model.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta para o Frontend Next.js.
 * Usamos Record para garantir imutabilidade e concisão.
 */
public record TransactionResponse(
        UUID id,
        String nsu,
        String authorizationCode,
        BigDecimal amount,
        String currency,
        TransactionStatus status,
        ProductType productType,
        String terminalSerialNumber,
        LocalDateTime createdAt,
        String message // Mensagem amigável para o lojista (ex: "Transação Aprovada")
) {
    /**
     * Factory method para facilitar a criação a partir do modelo de domínio.
     */
    public static TransactionResponse fromDomain(Transaction domain, String message) {
        return new TransactionResponse(
                domain.getId(),
                domain.getNsu(),
                domain.getAuthorizationCode(),
                domain.getAmount(),
                domain.getCurrency(),
                domain.getStatus(),
                domain.getProductType(),
                domain.getSource().terminalSerialNumber(),
                domain.getCreatedAt(),
                message
        );
    }
}