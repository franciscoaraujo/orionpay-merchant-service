package orionpay.merchant.infrastructure.adapters.output.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import orionpay.merchant.domain.model.LedgerAccount;
import orionpay.merchant.domain.model.enums.EntryType;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.LedgerAccountEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.LedgerEntryEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LedgerMapper {

    // Mapeamento da Conta (LedgerAccount) -> Entity
    @Mapping(target = "id", source = "accountId") // ID da entidade
    @Mapping(target = "ledgerAccountId", source = "accountId") // Coluna 'account_id' que também é obrigatória
    @Mapping(target = "accountCode", source = "accountNumber")
    @Mapping(target = "merchantId", source = "merchantId")
    @Mapping(target = "balance", source = "balance")
    @Mapping(target = "version", source = "version") // Mapeia a versão para controle de concorrência
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "active", constant = "true")
    LedgerAccountEntity toEntity(LedgerAccount domain);

    // Método para atualizar uma entidade existente
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ledgerAccountId", ignore = true)
    @Mapping(target = "accountCode", source = "accountNumber")
    @Mapping(target = "merchantId", source = "merchantId")
    @Mapping(target = "balance", source = "balance")
    @Mapping(target = "version", ignore = true) // <-- MUDE PARA IGNORE AQUI
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "active", constant = "true")
    void updateEntityFromDomain(LedgerAccount domain, @MappingTarget LedgerAccountEntity entity);

    // Entity -> Domain
    @Mapping(target = "accountId", source = "id")
    @Mapping(target = "accountNumber", source = "accountCode")
    @Mapping(target = "version", source = "version") // Recupera a versão do banco
    LedgerAccount toDomain(LedgerAccountEntity entity);


    // Transaction -> LedgerEntry
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ledgerAccount", ignore = true) // Corrigido de ledgerAccountId para ledgerAccount
    @Mapping(target = "journal", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "type", source = "type")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "correlationId", source = "transactionId")
    LedgerEntryEntity toEntity(UUID transactionId, BigDecimal amount, EntryType type, String description);
}