package orionpay.merchant.infrastructure.adapters.output.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import orionpay.merchant.domain.model.ExtratoTransaction;
import orionpay.merchant.domain.model.ExtratoTransactionDetail;
import orionpay.merchant.domain.model.Transaction;
import orionpay.merchant.domain.model.TransactionSource;
import orionpay.merchant.domain.model.enums.TransactionStatus;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.TransactionEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.vo.TransactionSourceVo;

@Mapper(componentModel = "spring", uses = {MerchantMapper.class})
public interface TransactionMapper {

    // 1. DA ENTIDADE (Banco) PARA O DOMÍNIO (Objeto Rico)
    // Usamos o mapeamento resumido do Merchant para evitar carregar coleções lazy (pricings/terminals)
    @Mapping(source = "merchant", target = "merchant", qualifiedByName = "toDomainSummary")
    Transaction toDomain(TransactionEntity entity);

    // 2. DO DOMÍNIO PARA A ENTIDADE
    // Ignoramos o 'merchant' aqui para o RepositoryAdapter setar via getReferenceById
    @Mapping(target = "merchant", ignore = true)
    @Mapping(source = "source", target = "source")
    TransactionEntity toEntity(Transaction domain);

    // Mapeamento explícito para o Value Object da Origem
    TransactionSourceVo toVo(TransactionSource domain);

    TransactionSource toDomain(TransactionSourceVo vo);

    @Mapping(target = "status", expression = "java(mapStatus(transactionEntity.getStatus()))")
    @Mapping(target = "externalId", expression = "java(\"optr_\" + transactionEntity.getNsu())")
    @Mapping(target = "brand", source = "cardBrand") // Mapeia a nova coluna card_brand se existir, ou null
    @Mapping(target = "lastFour", expression = "java(formatLastFour(transactionEntity.getCardLastFour()))")
    ExtratoTransaction toExtratoDomain(TransactionEntity transactionEntity);

    // Converte do ENUM para texto legível
    default String mapStatus(TransactionStatus status) {
        if (status == null) return "Pendente";
        return switch (status.name()) {
            case "APPROVED" -> "Capturado";
            case "PENDING" -> "Autorizado";
            case "DECLINED" -> "Negado";
            case "CANCELLED" -> "Cancelado";
            case "REVERSED" -> "Estornado";
            default -> status.name();
        };
    }

    // Formata últimos 4 dígitos
    @Named("formatLastFour")
    default String formatLastFour(String lastFour) {
        if (lastFour == null || lastFour.isEmpty()) {
            return "**** **** **** 0000";
        }
        return "**** **** **** " + lastFour;
    }

    // Nota: TransactionEntity não tem cardBrand/cardHolderName, então serão null
    @Mapping(target = "brand", source = "cardBrand") 
    @Mapping(target = "lastFour", source = "cardLastFour")
    @Mapping(target = "holderName", source = "cardHolderName")
    @Mapping(target = "externalId", expression = "java(\"optr_\" + transaction.getNsu())")
    @Mapping(target = "status", expression = "java(mapStatus(transaction.getStatus()))")
    ExtratoTransactionDetail toDetailDomain(Transaction transaction);

}