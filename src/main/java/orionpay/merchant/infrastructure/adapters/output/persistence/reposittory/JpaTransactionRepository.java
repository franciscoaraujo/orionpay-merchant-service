package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.TransactionEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.BrandDistributionProjection;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.HourlySalesProjection;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.TransactionSummaryProjection;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaTransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    // CORREÇÃO 1: Use o padrão de "Property Traversal" (Atributo_SubAtributo)
    List<TransactionEntity> findByMerchantId(UUID merchantId);

    Optional<TransactionEntity> findByNsu(String nsu);

    // No Native Query, você usa os nomes das COLUNAS do banco (merchant_id), então aqui estava correto.
    @Query(value = """
            SELECT 
                COALESCE(SUM(amount), 0) AS approvedVolume,
                COUNT(*) AS totalCount,
                COUNT(*) FILTER (WHERE status = 'APPROVED') AS approvedCount
            FROM core.transaction 
            WHERE merchant_id = :merchant 
            AND created_at >= :startOfDay
            """, nativeQuery = true)
    TransactionSummaryProjection getDailySummary(
            @Param("merchant") UUID merchantId,
            @Param("startOfDay") LocalDateTime startOfDay
    );

    @Query(value = """
            WITH hours AS (
                SELECT generate_series(0, 23) AS hour -- Gera os números de 0 a 23
            )
            SELECT 
                h.hour,
                COALESCE(SUM(CASE WHEN CAST(t.created_at AS DATE) = CAST(:targetDate AS DATE) THEN t.amount ELSE 0 END), 0) as today,
                COALESCE(SUM(CASE WHEN CAST(t.created_at AS DATE) = CAST(:targetDate AS DATE) - 1 THEN t.amount ELSE 0 END), 0) as yesterday
            FROM hours h
            LEFT JOIN core.transaction t ON EXTRACT(HOUR FROM t.created_at) = h.hour 
                AND t.status = 'APPROVED'
                AND t.merchant_id = :merchantId
            GROUP BY h.hour
            ORDER BY h.hour
            """, nativeQuery = true)
    List<HourlySalesProjection> getHourlyTrend(@Param("merchantId") UUID merchantId, @Param("targetDate") LocalDateTime targetDate);

    @Query(value = """
            SELECT 
                product_type as brand, 
                SUM(amount) as value,
                (SUM(amount) * 100.0 / SUM(SUM(amount)) OVER()) as percentage
            FROM core.transaction
            WHERE merchant_id = :merchantId 
            AND status = 'APPROVED'
            AND created_at >= :startOfDay
            GROUP BY product_type
            """, nativeQuery = true)
    List<BrandDistributionProjection> getBrandDistribution(@Param("merchantId") UUID merchantId, @Param("startOfDay") LocalDateTime startOfDay);

    // CORREÇÃO 2: Em JPQL (sem o nativeQuery=true), você deve usar o caminho do objeto: t.merchant.id
    @Query("SELECT t FROM TransactionEntity t WHERE t.merchant.id = :merchantId " +
            "AND (:search IS NULL OR t.nsu LIKE %:search% OR t.authCode LIKE %:search%)")
    Page<TransactionEntity> findAllByMerchantIdAndFilter(
            @Param("merchantId") UUID merchantId,
            @Param("search") String search,
            Pageable pageable
    );

    // CORREÇÃO 3: Seguir o mesmo padrão Property Traversal
    Optional<TransactionEntity> findByIdAndMerchantId(UUID id, UUID merchantId);

}