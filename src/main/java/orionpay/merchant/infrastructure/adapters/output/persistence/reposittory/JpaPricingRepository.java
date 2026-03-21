package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orionpay.merchant.domain.model.enums.ProductType;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.PricingEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaPricingRepository extends JpaRepository<PricingEntity, UUID> {

    // Procura a taxa mais recente ou efetiva para o lojista
    // Opcional: filtrar por tipo de produto (DEBIT, CREDIT_A_VISTA) se quiser granularidade
    Optional<PricingEntity> findFirstByMerchantIdOrderByEffectiveDateDesc(UUID merchantId);

    Optional<PricingEntity> findFirstByMerchantIdAndProductTypeOrderByEffectiveDateDesc(UUID merchantId, ProductType productType);
}