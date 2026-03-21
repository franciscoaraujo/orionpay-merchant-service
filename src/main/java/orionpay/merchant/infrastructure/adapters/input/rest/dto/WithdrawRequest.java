package orionpay.merchant.infrastructure.adapters.input.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawRequest(
        @NotNull(message = "ID do lojista é obrigatório")
        UUID merchantId,

        @NotNull(message = "Valor do saque é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor mínimo para saque é 0.01")
        BigDecimal amount,

        @NotBlank(message = "Chave Pix é obrigatória")
        String pixKey
) {}