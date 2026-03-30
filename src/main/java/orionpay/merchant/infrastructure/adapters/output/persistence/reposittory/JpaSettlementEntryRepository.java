package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.SettlementEntryEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaSettlementEntryRepository extends JpaRepository<SettlementEntryEntity, UUID> {
    Optional<SettlementEntryEntity> findByTransactionId(UUID transactionId);
}
