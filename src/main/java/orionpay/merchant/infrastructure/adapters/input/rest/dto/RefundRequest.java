package orionpay.merchant.infrastructure.adapters.input.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RefundRequest(
    @NotNull(message = "ID da transação é obrigatório")
    UUID transactionId,
    
    @NotBlank(message = "O motivo do estorno é obrigatório")
    String reason
) {}
