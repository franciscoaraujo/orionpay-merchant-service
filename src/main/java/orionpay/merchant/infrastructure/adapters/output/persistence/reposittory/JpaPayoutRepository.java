package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.PayoutEntity;

import java.util.UUID;

@Repository
public interface JpaPayoutRepository extends JpaRepository<PayoutEntity, UUID> {
}