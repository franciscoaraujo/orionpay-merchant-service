package orionpay.merchant.application.ports.input.rest.dto;

import orionpay.merchant.domain.model.Merchant;
import orionpay.merchant.domain.model.enums.MerchantStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de Resposta do Lojista para o Frontend.
 * Agrupa informações básicas, endereço e status atual.
 */
public record MerchantResponseDto(
        UUID id,
        String name,
        String document,
        String email,
        MerchantStatus status,
        AddressResponse address,
        LocalDateTime createdAt
) {
    /**
     * Converte o Modelo Rico de Domínio para o DTO de Resposta.
     */
    public static MerchantResponseDto fromDomain(Merchant domain) {
        return new MerchantResponseDto(
                domain.getId(),
                domain.getLegalName(),
                domain.getDocument(),
                domain.getEmail(),
                domain.getStatus(),
                new AddressResponse(
                        domain.getBusinessAddress().street(),
                        domain.getBusinessAddress().number(),
                        domain.getBusinessAddress().neighborhood(),
                        domain.getBusinessAddress().city(),
                        domain.getBusinessAddress().state(),
                        domain.getBusinessAddress().zipCode()
                ),
                domain.getCreatedAt()
        );
    }

    /**
     * Sub-DTO para o endereço, evitando aninhamento excessivo no Record principal.
     */
    public record AddressResponse(
            String street,
            String number,
            String neighborhood,
            String city,
            String state,
            String zipCode
    ) {
    }
}