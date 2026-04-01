package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.DailyMerchantSummaryEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.TransactionSummaryProjection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaDailySummaryRepository extends JpaRepository<DailyMerchantSummaryEntity, UUID> {

    Optional<DailyMerchantSummaryEntity> findByMerchantIdAndDate(UUID merchantId, LocalDate date);

    @Modifying
    @Query(value = """
        INSERT INTO ops.daily_merchant_summary 
            (id, merchant_id, summary_date, total_tpv, total_net_revenue, approved_count, total_count)
        VALUES 
            (gen_random_uuid(), :merchantId, :date, :amount, :netAmount, :approvedInc, :totalInc)
        ON CONFLICT (merchant_id, summary_date) DO UPDATE SET
            total_tpv = EXCLUDED.total_tpv,
            total_net_revenue = EXCLUDED.total_net_revenue,
            approved_count = EXCLUDED.approved_count,
            total_count = EXCLUDED.total_count
        """, nativeQuery = true)
    void upsertDailyMetrics(
            @Param("merchantId") UUID merchantId,
            @Param("date") LocalDate date,
            @Param("amount") BigDecimal amount,
            @Param("netAmount") BigDecimal netAmount,
            @Param("approvedInc") long approvedInc,
            @Param("totalInc") long totalInc
    );

    @Query("""
        SELECT 
            COALESCE(SUM(s.totalTpv), 0) AS totalVolume,
            COALESCE(SUM(s.totalNetRevenue), 0) AS netVolume,
            COALESCE(SUM(s.totalCount), 0) AS totalCount,
            COALESCE(SUM(s.approvedCount), 0) AS approvedCount
        FROM DailyMerchantSummaryEntity s
        WHERE s.merchantId = :merchantId
        AND s.date BETWEEN :startDate AND :endDate
    """)
    TransactionSummaryProjection findConsolidatedSummaryByPeriod(
            @Param("merchantId") UUID merchantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Modifying
    @Query(value = """
        INSERT INTO ops.daily_merchant_summary (id, merchant_id, summary_date, total_tpv, total_net_revenue, approved_count, total_count)
        SELECT 
            gen_random_uuid(),
            t.merchant_id,
            CAST(t.created_at AS DATE),
            SUM(t.amount),
            SUM(COALESCE(t.net_amount, t.amount * 0.97)),
            COUNT(*) FILTER (WHERE t.status = 'APPROVED'),
            COUNT(*)
        FROM core.transaction t
        WHERE t.merchant_id = :merchantId
        GROUP BY t.merchant_id, CAST(t.created_at AS DATE)
        ON CONFLICT (merchant_id, summary_date) DO UPDATE SET
            total_tpv = EXCLUDED.total_tpv,
            total_net_revenue = EXCLUDED.total_net_revenue,
            approved_count = EXCLUDED.approved_count,
            total_count = EXCLUDED.total_count
        """, nativeQuery = true)
    void rebuildSummary(@Param("merchantId") UUID merchantId);
}
