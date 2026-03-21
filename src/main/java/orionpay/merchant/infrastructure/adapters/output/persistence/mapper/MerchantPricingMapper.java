package orionpay.merchant.infrastructure.adapters.output.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import orionpay.merchant.domain.model.MerchantPricing;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.PricingEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MerchantPricingMapper {

    // 1. DA ENTIDADE PARA O DOMÍNIO
    // O domínio usa 'merchantId' (UUID), a entidade usa 'merchant.id' (Objeto -> UUID)
    @Mapping(source = "merchant.id", target = "merchantId")
    MerchantPricing toDomain(PricingEntity entity);

    // 2. DO DOMÍNIO PARA A ENTIDADE
    // Precisamos ignorar o objeto 'merchant' completo, pois o Mapper não sabe criá-lo.
    // O ID da entidade PricingEntity é gerado pelo banco (@GeneratedValue), então ignoramos no mapeamento.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "merchant", ignore = true)
    PricingEntity toEntity(MerchantPricing domain);

    // 3. CONVERSÃO DE LISTAS
    List<MerchantPricing> toDomainList(List<PricingEntity> entities);

    List<PricingEntity> toEntityList(List<MerchantPricing> domains);

}