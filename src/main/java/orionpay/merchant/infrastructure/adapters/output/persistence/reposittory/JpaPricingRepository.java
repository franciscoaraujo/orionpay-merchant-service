package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orionpay.merchant.domain.model.enums.ProductType;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.PricingEntity;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaPricingRepository extends JpaRepository<PricingEntity, UUID> {

    List<PricingEntity> findByMerchantId(UUID merchantId);

    Optional<PricingEntity> findFirstByMerchantIdOrderByEffectiveDateDesc(UUID merchantId);

    Optional<PricingEntity> findFirstByMerchantIdAndProductTypeOrderByEffectiveDateDesc(UUID merchantId, ProductType productType);
}
