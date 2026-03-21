package orionpay.merchant.infrastructure.adapters.output.persistence.mapper;

import org.mapstruct.*;
import orionpay.merchant.domain.model.BankAccount;
import orionpay.merchant.domain.model.Merchant;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.BankAccountEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.MerchantEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {AddressMapper.class, MerchantPricingMapper.class, TerminalMapper.class})
public interface MerchantMapper {

    @Mapping(target = "businessAddress", source = "address")
    Merchant toDomain(MerchantEntity entity);

    @Named("toDomainSummary")
    @Mapping(target = "businessAddress", source = "address")
    @Mapping(target = "pricings", ignore = true) // Evita LazyInitializationException
    @Mapping(target = "terminals", ignore = true) // Evita LazyInitializationException
    Merchant toDomainSummary(MerchantEntity entity);

    @Mapping(target = "address", source = "businessAddress")
    MerchantEntity toEntity(Merchant domain);

    // Mapeamento da Conta Bancária (usado internamente pelo MerchantMapper)
    @Mapping(target = "type", source = "accountType")
    BankAccount toDomain(BankAccountEntity entity);

    @Mapping(target = "accountType", source = "type")
    @Mapping(target = "id", ignore = true) // Gerado pelo banco
    @Mapping(target = "merchant", ignore = true) // Definido pelo relacionamento no AfterMapping
    BankAccountEntity toEntity(BankAccount domain);

    @AfterMapping
    default void linkRelationships(@MappingTarget MerchantEntity merchantEntity) {
        // Link BankAccount -> Merchant
        if (merchantEntity.getBankAccount() != null) {
            merchantEntity.getBankAccount().setMerchant(merchantEntity);
        }
        // Link Address -> Merchant
        if (merchantEntity.getAddress() != null) {
            merchantEntity.getAddress().setMerchant(merchantEntity);
        }
        // Link Pricings -> Merchant
        if (merchantEntity.getPricings() != null) {
            merchantEntity.getPricings().forEach(p -> p.setMerchant(merchantEntity));
        }
        // Link Terminals -> Merchant
        if (merchantEntity.getTerminals() != null) {
            merchantEntity.getTerminals().forEach(t -> t.setMerchant(merchantEntity));
        }
    }
}