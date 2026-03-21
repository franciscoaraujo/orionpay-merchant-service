package orionpay.merchant.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import orionpay.merchant.domain.excepion.DomainException;

@Builder
public record Address(
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String zipCode
) {
    public Address {
        if (zipCode == null || !zipCode.matches("\\d{8}")) {
            throw new DomainException("CEP inválido. Deve conter 8 dígitos.");
        }
    }

}