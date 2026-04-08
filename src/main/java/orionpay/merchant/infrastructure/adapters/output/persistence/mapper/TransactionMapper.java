package orionpay.merchant.infrastructure.adapters.output.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import orionpay.merchant.domain.model.ExtratoTransaction;
import orionpay.merchant.domain.model.ExtratoTransactionDetail;
import orionpay.merchant.domain.model.Transaction;
import orionpay.merchant.domain.model.TransactionSource;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.TransactionEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.vo.TransactionSourceVo;

@Mapper(componentModel = "spring", uses = {MerchantMapper.class})
public interface TransactionMapper {

    // 1. DA ENTIDADE (Banco) PARA O DOMÍNIO (Objeto Rico)
    @Mapping(source = "merchant", target = "merchant", qualifiedByName = "toDomainSummary")
    @Mapping(source = "authCode", target = "authorizationCode")
    Transaction toDomain(TransactionEntity entity);

    // 2. DO DOMÍNIO PARA A ENTIDADE
    @Mapping(target = "merchant", ignore = true)
    @Mapping(target = "terminal", ignore = true)
    @Mapping(source = "source", target = "source")
    @Mapping(source = "authorizationCode", target = "authCode")
    @Mapping(source = "authorizationCode", target = "authorizationCode")
    TransactionEntity toEntity(Transaction domain);

    // Mapeamento explícito para o Value Object da Origem
    TransactionSourceVo toVo(TransactionSource domain);

    @Mapping(target = "terminalSerialNumber", source = "terminalSerialNumber")
    @Mapping(target = "entryMode", source = "entryMode")
    TransactionSource toDomain(TransactionSourceVo vo);

    // Mapeamento p/ Extrato (Sincronizado com os novos campos)
    @Mapping(target = "cardBrand", source = "cardBrand")
    @Mapping(target = "cardLastFour", source = "cardLastFour")
    ExtratoTransaction toExtratoDomain(TransactionEntity transactionEntity);

    // Mapeamento p/ Detalhe
    @Mapping(target = "brand", source = "cardBrand") 
    @Mapping(target = "lastFour", source = "cardLastFour")
    @Mapping(target = "holderName", source = "cardHolderName")
    @Mapping(target = "externalId", expression = "java(\"optr_\" + transaction.getNsu())")
    ExtratoTransactionDetail toDetailDomain(Transaction transaction);
}
