package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import orionpay.merchant.domain.model.MerchantPricing;
import orionpay.merchant.domain.model.enums.ProductType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PricingRepository {

    Optional<MerchantPricing> findCurrentPricing(UUID merchantId);

    Optional<MerchantPricing> findCurrentPricing(UUID merchantId, ProductType productType);

    void saveAll(List<MerchantPricing> domainList);

}
