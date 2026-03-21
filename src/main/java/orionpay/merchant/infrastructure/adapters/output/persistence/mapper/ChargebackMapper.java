package orionpay.merchant.infrastructure.adapters.output.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import orionpay.merchant.domain.model.Chargeback;
import orionpay.merchant.domain.model.TransactionEvent;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.ChargebackEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.TransactionEventEntity;

@Mapper(componentModel = "spring")
public interface ChargebackMapper {
    Chargeback toDomain(ChargebackEntity entity);
    ChargebackEntity toEntity(Chargeback domain);
}

