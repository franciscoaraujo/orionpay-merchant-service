package orionpay.merchant.infrastructure.adapters.input.rest.dto;

import jakarta.validation.constraints.*;
import orionpay.merchant.domain.model.enums.AccountType;

import java.util.UUID;

/**
 * DTO de entrada para o Onboarding de um novo Lojista.
 * Valida dados cadastrais, endereço e dados bancários.
 */
public record MerchantRegistrationRequest(

        @NotBlank(message = "Nome/Razão Social é obrigatório")
        @Size(min = 3, max = 150)
        String name,

        @NotBlank(message = "CPF ou CNPJ é obrigatório")
        @Pattern(regexp = "(^\\d{11}$|^\\d{14}$)", message = "Documento deve ter 11 (CPF) ou 14 (CNPJ) dígitos")
        String document,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres")
        String password,

        // --- Dados de Endereço ---
        @NotBlank(message = "CEP é obrigatório")
        @Pattern(regexp = "^\\d{8}$", message = "CEP deve conter 8 dígitos")
        String zipCode,

        @NotBlank(message = "Logradouro é obrigatório")
        String street,

        @NotBlank(message = "Número é obrigatório")
        String number,

        String complement,

        @NotBlank(message = "Bairro é obrigatório")
        String neighborhood,

        @NotBlank(message = "Cidade é obrigatória")
        String city,

        @NotBlank(message = "Estado (UF) é obrigatório")
        @Size(min = 2, max = 2, message = "UF deve ter 2 caracteres")
        String state,

        // --- Dados Bancários (Domicílio Bancário) ---
        @NotBlank(message = "Código do banco é obrigatório")
        @Size(min = 3, max = 3, message = "Código do banco deve ter 3 dígitos (ex: 001)")
        String bankCode,

        @NotBlank(message = "Agência é obrigatória")
        String branch,

        @NotBlank(message = "Número da conta é obrigatório")
        String account,

        @NotBlank(message = "Dígito da conta é obrigatório")
        String accountDigit,

        @NotNull(message = "Tipo de conta (CHECKING/SAVINGS) é obrigatório")
        AccountType accountType
) {}