package orionpay.merchant.application.ports.input.rest.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TransactionListItemDto {
    private UUID id;
    private String externalId; // tx_123456
    private LocalDateTime createdAt;
    private BigDecimal amount;
    private String brand; // VISA, MASTERCARD
    private String lastFourDigits;
    private String status; // Capturado, Autorizado, Negado
    private String nsu;
}