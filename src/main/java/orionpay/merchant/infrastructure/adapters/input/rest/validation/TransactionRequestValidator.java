package orionpay.merchant.infrastructure.adapters.input.rest.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionRequest;

public class TransactionRequestValidator implements ConstraintValidator<ValidTransactionRequest, TransactionRequest> {

    @Override
    public boolean isValid(TransactionRequest request, ConstraintValidatorContext context) {
        if (request == null || request.entryMode() == null) {
            return true; // Deixa o @NotNull ou @NotBlank do entryMode tratar
        }

        boolean isValid = true;
        context.disableDefaultConstraintViolation();

        if ("MANUAL".equals(request.entryMode())) {
            if (request.cvv() == null || request.cvv().isBlank()) {
                addViolation(context, "cvv", "CVV é obrigatório para entrada manual");
                isValid = false;
            }
        } else if ("CHIP".equals(request.entryMode()) || "CONTACTLESS".equals(request.entryMode())) {
            if (request.applicationCryptogram() == null || request.applicationCryptogram().isBlank()) {
                addViolation(context, "applicationCryptogram", "Dados do chip (applicationCryptogram) ausentes para venda presencial");
                isValid = false;
            }
            if (request.atc() == null || request.atc().isBlank()) {
                addViolation(context, "atc", "Dados do chip (atc) ausentes para venda presencial");
                isValid = false;
            }
        }

        return isValid;
    }

    private void addViolation(ConstraintValidatorContext context, String field, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}
