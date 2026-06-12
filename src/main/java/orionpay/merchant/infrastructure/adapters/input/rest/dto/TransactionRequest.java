package orionpay.merchant.infrastructure.adapters.input.rest.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.CreditCardNumber;
import orionpay.merchant.domain.model.enums.ProductType;
import orionpay.merchant.infrastructure.adapters.input.rest.validation.ValidTransactionRequest;

import java.math.BigDecimal;
import java.util.UUID;

@ValidTransactionRequest
public record TransactionRequest(
        @NotNull(message = "ID do lojista é obrigatório")
        UUID merchantId,

        @NotNull(message = "Valor da transação é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor mínimo da transação é 0.01")
        @Digits(integer = 12, fraction = 2, message = "Formato de valor inválido")
        BigDecimal amount,

        @NotNull(message = "Tipo de produto é obrigatório")
        ProductType productType,

        @Min(value = 1, message = "Mínimo de 1 parcela")
        @Max(value = 12, message = "Máximo de 12 parcelas")
        Integer installments, // Novo Campo

        @NotBlank(message = "Serial do terminal é obrigatório")
        String terminalSn,

        String externalReference,

        @NotBlank(message = "Modo de entrada é obrigatório")
        String entryMode,

        @NotBlank(message = "A bandeira do cartão é obrigatória")
        String cardBrand,

        @NotBlank(message = "O nome do titular é obrigatório")
        String cardHolderName,

        @NotBlank(message = "O número do cartão é obrigatório")
        @CreditCardNumber(message = "Número de cartão inválido")
        String cardNumber,

        @NotBlank(message = "A data de expiração é obrigatória")
        String expirationDate,

        String cvv,
        String applicationCryptogram,
        String atc
) {
        public String cardBin() {
                if (cardNumber == null || cardNumber.length() < 6) return null;
                return cardNumber.substring(0, 6);
        }

        public String cardLastFour() {
                if (cardNumber == null || cardNumber.length() < 4) return null;
                return cardNumber.substring(cardNumber.length() - 4);
        }
}
