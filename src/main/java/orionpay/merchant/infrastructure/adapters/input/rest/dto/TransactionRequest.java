package orionpay.merchant.infrastructure.adapters.input.rest.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.CreditCardNumber;
import orionpay.merchant.domain.model.enums.ProductType;
import orionpay.merchant.infrastructure.adapters.input.rest.validation.ValidTransactionRequest; // Importar a nova anotação

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de Entrada para novas transações.
 * Valida os dados antes mesmo de chegarem ao Use Case.
 */
@ValidTransactionRequest // Aplicar a validação customizada aqui
public record TransactionRequest(

        // --- CONTEXTO DA TRANSAÇÃO ---
        @NotNull(message = "ID do lojista é obrigatório")
        UUID merchantId, // Lembrete: Em produção, o ideal é pegar isso do Token JWT!

        @NotNull(message = "Valor da transação é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor mínimo da transação é 0.01")
        @Digits(integer = 12, fraction = 2, message = "Formato de valor inválido")
        BigDecimal amount,

        @NotNull(message = "Tipo de produto é obrigatório")
        ProductType productType,

        // --- DADOS DO TERMINAL / ORIGEM ---
        @NotBlank(message = "Serial do terminal é obrigatório")
        @Size(min = 5, max = 50, message = "Serial do terminal deve ter entre 5 e 50 caracteres")
        String terminalSn,

        @Size(max = 100, message = "A referência externa deve ter no máximo 100 caracteres")
        String externalReference,

        @NotBlank(message = "Modo de entrada é obrigatório")
        @Pattern(regexp = "^(CHIP|CONTACTLESS|MANUAL)$", message = "Modo de entrada deve ser CHIP, CONTACTLESS ou MANUAL")
        String entryMode,

        // --- DADOS DO CARTÃO (NOVOS CAMPOS) ---
        @NotBlank(message = "A bandeira do cartão é obrigatória")
        String cardBrand, // Ex: MASTERCARD, VISA, ELO

        @NotBlank(message = "O nome do titular é obrigatório")
        @Size(min = 2, max = 50, message = "O nome do titular deve ter entre 2 e 50 caracteres")
        String cardHolderName,

        @NotBlank(message = "O número do cartão é obrigatório")
        @CreditCardNumber(message = "Número de cartão inválido") // Validação nativa do algoritmo de Luhn!
        String cardNumber,

        @NotBlank(message = "A data de expiração é obrigatória")
        @Pattern(regexp = "^(0[1-9]|1[0-2])/([0-9]{2})$", message = "A data de expiração deve estar no formato MM/AA")
        String expirationDate,

        @Pattern(regexp = "^[0-9]{3,4}$", message = "O CVV deve conter 3 ou 4 dígitos")
        String cvv, // Removido @NotBlank

        // Novos campos EMV
        String applicationCryptogram, // Tag 9F26
        String atc // Tag 9F36
) {
        /**
         * MÉTODOS AUXILIARES DO RECORD
         * Estes métodos permitem que o UseCase pegue o BIN e os 4 últimos dígitos
         * sem que o frontend precise enviar esses dados duplicados no JSON.
         */

        public String cardBin() {
                if (cardNumber == null || cardNumber.length() < 6) return null;
                return cardNumber.substring(0, 6);
        }

        public String cardLastFour() {
                if (cardNumber == null || cardNumber.length() < 4) return null;
                return cardNumber.substring(cardNumber.length() - 4);
        }
}