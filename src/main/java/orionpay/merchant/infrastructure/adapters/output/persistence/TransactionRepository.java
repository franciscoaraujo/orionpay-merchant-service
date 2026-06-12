package orionpay.merchant.infrastructure.adapters.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.TransactionEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.TransactionStatsProjection;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    @Query(value = """
            SELECT
                COALESCE(SUM(CASE WHEN status = 'APPROVED' THEN amount ELSE 0 END), 0) AS totalVolume,
                COALESCE(SUM(CASE WHEN status = 'APPROVED' THEN net_amount ELSE 0 END), 0) AS netVolume,
                COUNT(*) AS totalCount,
                COUNT(CASE WHEN status = 'APPROVED' THEN 1 END) AS approvedCount
            FROM core.transaction
            WHERE merchant_id = CAST(:merchantId AS UUID)
            AND created_at >= :startDate
            AND created_at < :endDate
            """, nativeQuery = true)
    TransactionStatsProjection findTransactionStats(
            @Param("merchantId") String merchantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}