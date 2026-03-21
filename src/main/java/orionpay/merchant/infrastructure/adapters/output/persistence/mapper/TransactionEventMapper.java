package orionpay.merchant.infrastructure.adapters.output.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import orionpay.merchant.domain.model.TransactionEvent;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.TransactionEventEntity;

@Mapper(componentModel = "spring")
public interface TransactionEventMapper {
    // Como TransactionEvent é um Record no domínio, o MapStruct lida nativamente
    TransactionEvent toDomain(TransactionEventEntity entity);

    @Mapping(target = "metadata", source = "metadata")
    TransactionEventEntity toEntity(TransactionEvent domain);
}
