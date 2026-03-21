package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.MerchantEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaMerchantRepository extends JpaRepository<MerchantEntity, UUID> {
    Optional<MerchantEntity> findByDocument(String document);
}