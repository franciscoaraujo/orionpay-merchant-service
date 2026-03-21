package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.JournalEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaJournalRepository extends JpaRepository<JournalEntity, UUID> {
    Optional<JournalEntity> findByReferenceId(UUID referenceId);
}