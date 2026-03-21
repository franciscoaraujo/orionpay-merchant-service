package orionpay.merchant.infrastructure.adapters.output.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import orionpay.merchant.domain.model.Address;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.AddressEntity;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    Address toDomain(AddressEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "merchant", ignore = true)
    @Mapping(target = "mainAddress", constant = "true") // Assume true se não especificado
    AddressEntity toEntity(Address domain);
}