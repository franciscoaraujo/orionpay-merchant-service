package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.LedgerAccountEntity;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaLedgerAccountRepository extends JpaRepository<LedgerAccountEntity, UUID> {
    Optional<LedgerAccountEntity> findByMerchantId(UUID merchantId);

    // Query para buscar saldo de forma atômica se necessário
    @Query("SELECT l.balance FROM LedgerAccountEntity l WHERE l.merchantId = :merchantId")
    BigDecimal getBalance(@Param("merchantId") UUID merchantId);
}