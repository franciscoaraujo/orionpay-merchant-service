package orionpay.merchant.infrastructure.adapters.output.persistence.mapper;

import org.mapstruct.Mapper;
import orionpay.merchant.domain.model.TransactionStatusProjection;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.TransactionStatusProjectionEntity;

@Mapper(componentModel = "spring")
public interface TransactionStatusProjectionMapper {
    TransactionStatusProjection toDomain(TransactionStatusProjectionEntity entity);

    // Mapeia o estado atualizado da projeção para a tabela sales.transaction_status_projection
    TransactionStatusProjectionEntity toEntity(TransactionStatusProjection domain);
}