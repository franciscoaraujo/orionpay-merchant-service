package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import orionpay.merchant.domain.model.enums.SettlementStatus;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.SettlementEntryEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.AgendaItemProjection;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.DailyScheduleProjection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

@Repository
public interface JpaSettlementEntryRepository extends JpaRepository<SettlementEntryEntity, UUID> {

    Optional<SettlementEntryEntity> findByTransactionIdAndInstallmentNumber(UUID transactionId, Integer installmentNumber);

    boolean existsByTransactionIdAndInstallmentNumber(UUID transactionId, Integer installmentNumber);

    @Query("""
            SELECT se FROM SettlementEntryEntity se 
            WHERE se.merchantId = :merchantId 
            AND se.status IN (
                orionpay.merchant.domain.model.enums.SettlementStatus.SCHEDULED, 
                orionpay.merchant.domain.model.enums.SettlementStatus.SETTLED
            )
            AND se.expectedSettlementDate > CURRENT_TIMESTAMP
            AND (se.blocked IS NULL OR se.blocked = false)
            AND (se.anticipated IS NULL OR se.anticipated = false)
            AND se.status != orionpay.merchant.domain.model.enums.SettlementStatus.DISPUTE
            ORDER BY se.expectedSettlementDate ASC
            """)
    List<SettlementEntryEntity> findAvailableForAnticipation(@Param("merchantId") UUID merchantId);

    @Query(value = """
            SELECT 
                se.id as idExt, 
                se.transaction_id as transactionId,
                se.expected_settlement_date as settlementDate, 
                t.created_at as transactionDate,
                se.amount as grossAmount, 
                se.mdr_amount as mdrAmount, 
                se.net_amount as netAmount, 
                se.status as status,
                se.paid_at as paidAt,
                t.nsu as nsu,
                t.card_brand as cardBrand,
                t.card_last_four as cardLastFour,
                t.product_type as productType,
                se.is_blocked as blocked,
                se.is_anticipated as anticipated,
                se.installment_number as installmentNumber,
                se.mdr_percentage as mdrPercentage,
                se.original_amount as originalAmount
            FROM ops.settlement_entry se
            LEFT JOIN core.transaction t ON se.transaction_id = t.id
            WHERE se.merchant_id = :merchantId 
            AND se.expected_settlement_date >= :start 
            AND se.expected_settlement_date <= :end
            AND (:status IS NULL OR UPPER(se.status) = UPPER(:status))
            ORDER BY se.expected_settlement_date DESC
            """, nativeQuery = true)
    Page<AgendaItemProjection> findAgendaByPeriod(
            @Param("merchantId") UUID merchantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") String status,
            Pageable pageable
    );

    @Query(value = """
            SELECT 
                se.id as idExt, 
                se.transaction_id as transactionId,
                se.expected_settlement_date as settlementDate, 
                t.created_at as transactionDate,
                se.amount as grossAmount, 
                se.mdr_amount as mdrAmount, 
                se.net_amount as netAmount, 
                se.status as status,
                se.paid_at as paidAt,
                t.nsu as nsu,
                t.card_brand as cardBrand,
                t.card_last_four as cardLastFour,
                t.product_type as productType,
                se.is_blocked as blocked,
                se.is_anticipated as anticipated,
                se.installment_number as installmentNumber,
                se.mdr_percentage as mdrPercentage,
                se.original_amount as originalAmount
            FROM ops.settlement_entry se
            LEFT JOIN core.transaction t ON se.transaction_id = t.id
            WHERE se.id = :id
            """, nativeQuery = true)
    Optional<AgendaItemProjection> findDetailById(@Param("id") UUID id);

    @Query(value = """
            SELECT
                -- Forçamos a truncagem para o dia para garantir o agrupamento correto
                DATE_TRUNC('day', se.expected_settlement_date) as settlementDate,
                COALESCE(SUM(se.amount), 0) as totalGross,
                COALESCE(SUM(se.net_amount), 0) as totalNet,
                STRING_AGG(DISTINCT se.status, ',') as statuses,
                COUNT(se.id) as count
            FROM ops.settlement_entry se
            WHERE se.merchant_id = :merchantId
            -- Uso de BETWEEN com cast explícito para evitar problemas de tipo
            AND se.expected_settlement_date BETWEEN CAST(:startDate AS TIMESTAMP) AND (CAST(:endDate AS TIMESTAMP) + INTERVAL '1 day' - INTERVAL '1 second')
            AND (:status IS NULL OR se.status::text = :status)
            GROUP BY 1
            ORDER BY 1 ASC
            """, nativeQuery = true)
    List<DailyScheduleProjection> findDailySchedule(
            @Param("merchantId") UUID merchantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") String status
    );

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM ops.settlement_entry WHERE merchant_id = :merchantId AND expected_settlement_date >= :start AND expected_settlement_date <= :end", nativeQuery = true)
    BigDecimal sumGrossAmountByPeriod(@Param("merchantId") UUID merchantId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT COALESCE(SUM(net_amount), 0) FROM ops.settlement_entry WHERE merchant_id = :merchantId AND expected_settlement_date >= :start AND expected_settlement_date <= :end AND status = 'FAILED'", nativeQuery = true)
    BigDecimal sumCommittedAmountByPeriod(@Param("merchantId") UUID merchantId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT COALESCE(SUM(net_amount), 0) FROM ops.settlement_entry WHERE merchant_id = :merchantId AND expected_settlement_date >= :start AND expected_settlement_date <= :end AND status = 'SETTLED'", nativeQuery = true)
    BigDecimal sumAvailableAmountByPeriod(@Param("merchantId") UUID merchantId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
