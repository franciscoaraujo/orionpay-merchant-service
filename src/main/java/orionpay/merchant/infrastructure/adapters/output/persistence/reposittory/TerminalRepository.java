package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import orionpay.merchant.domain.model.Terminal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TerminalRepository {

    Optional<Terminal> findBySerialNumber(String serialNumber);

    List<Terminal> findAllByMerchantId(UUID merchantId);

    Terminal save(Terminal terminal);

    Long countByMerchantIdAndStatus(UUID merchantId, String active);
}