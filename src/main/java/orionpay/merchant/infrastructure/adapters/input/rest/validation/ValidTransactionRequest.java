package orionpay.merchant.infrastructure.adapters.input.rest.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TransactionRequestValidator.class)
@Documented
public @interface ValidTransactionRequest {
    String message() default "Dados de transação inválidos";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
