package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.AuditLogEntity;

import java.util.UUID;

@Repository
public interface JpaAuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
}

