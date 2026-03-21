package orionpay.merchant.infrastructure.adapters.output.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import orionpay.merchant.domain.model.Terminal;
import orionpay.merchant.domain.model.Transaction;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.TerminalEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.TransactionEntity;

@Mapper(componentModel = "spring")
public interface TerminalMapper {
    Terminal toDomain(TerminalEntity entity);

    @Mapping(target = "merchant", ignore = true)
    TerminalEntity toEntity(Terminal domain);

    @Mapping(source = "netAmount", target = "netAmount") // Certifique-se que o nome é idêntico nos dois lados
    TransactionEntity toEntity(Transaction domain);
}