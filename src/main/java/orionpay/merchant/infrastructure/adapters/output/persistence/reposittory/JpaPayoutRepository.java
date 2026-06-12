package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.PayoutEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaPayoutRepository extends JpaRepository<PayoutEntity, UUID> {
    
    Page<PayoutEntity> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId, Pageable pageable);

    boolean existsByMerchantIdAndStatusIn(UUID merchantId, List<PayoutEntity.PayoutStatus> statuses);
}
