package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orionpay.merchant.domain.model.enums.TerminalStatus;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.TerminalEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaTerminalRepository extends JpaRepository<TerminalEntity, UUID> {

    Optional<TerminalEntity> findBySerialNumber(String serialNumber);

    List<TerminalEntity> findByMerchantId(UUID merchantId);

    Long countByMerchantIdAndStatus(UUID merchantId, TerminalStatus terminalStatus);
}