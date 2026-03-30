package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.DailyMerchantSummaryEntity;

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
            total_tpv = ops.daily_merchant_summary.total_tpv + EXCLUDED.total_tpv,
            total_net_revenue = ops.daily_merchant_summary.total_net_revenue + EXCLUDED.total_net_revenue,
            approved_count = ops.daily_merchant_summary.approved_count + EXCLUDED.approved_count,
            total_count = ops.daily_merchant_summary.total_count + EXCLUDED.total_count
        """, nativeQuery = true)
    void upsertDailyMetrics(
            @Param("merchantId") UUID merchantId,
            @Param("date") LocalDate date,
            @Param("amount") BigDecimal amount,
            @Param("netAmount") BigDecimal netAmount,
            @Param("approvedInc") int approvedInc,
            @Param("totalInc") int totalInc
    );
}
