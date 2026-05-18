package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.OutboxEventEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaOutboxRepository extends JpaRepository<OutboxEventEntity, UUID> {
    
    List<OutboxEventEntity> findByStatus(OutboxEventEntity.OutboxStatus status);

    /**
     * Conta o número de eventos no outbox com um status específico.
     * @param status O status a ser contado (ex: PENDING).
     * @return O número de eventos.
     */
    Integer countByStatus(OutboxEventEntity.OutboxStatus status);
}
