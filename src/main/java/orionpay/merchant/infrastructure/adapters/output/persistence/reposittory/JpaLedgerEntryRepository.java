package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import orionpay.merchant.domain.model.enums.EntryType;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.LedgerEntryEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.LedgerBalanceProjection;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaLedgerEntryRepository extends JpaRepository<LedgerEntryEntity, UUID> {

    @Query("SELECT e FROM LedgerEntryEntity e WHERE e.ledgerAccount.id = :accountId ORDER BY e.createdAt DESC")
    List<LedgerEntryEntity> findByAccountIdOrderByCreatedAtDesc(@Param("accountId") UUID accountId);

    boolean existsByCorrelationIdAndType(UUID correlationId, EntryType type);

    @Query("""
       SELECT COALESCE(SUM(e.amount), 0)
       FROM LedgerEntryEntity e
       WHERE e.ledgerAccount.merchantId = :merchantId
         AND e.type = 'CREDIT'
         AND e.availableAt <= CURRENT_TIMESTAMP
       """)
    BigDecimal sumAvailableCredits(@Param("merchantId") UUID merchantId);

    @Query("""
       SELECT COALESCE(SUM(e.amount), 0)
       FROM LedgerEntryEntity e
       WHERE e.ledgerAccount.merchantId = :merchantId
         AND e.type = 'DEBIT'
       """)
    BigDecimal sumDebits(@Param("merchantId") UUID merchantId);

    @Query("""
       SELECT COALESCE(SUM(e.amount), 0)
       FROM LedgerEntryEntity e
       WHERE e.ledgerAccount.merchantId = :merchantId
         AND e.type = 'CREDIT'
         AND e.availableAt > CURRENT_TIMESTAMP
       """)
    BigDecimal sumFutureReceivables(@Param("merchantId") UUID merchantId);

    // Soma Créditos Disponíveis e subtrai Todos os Débitos
    @Query("""
       SELECT COALESCE(SUM(
           CASE 
               WHEN e.type = 'CREDIT' AND e.availableAt <= CURRENT_TIMESTAMP THEN e.amount 
               WHEN e.type = 'DEBIT' THEN -e.amount 
               ELSE 0 
           END
       ), 0)
       FROM LedgerEntryEntity e
       WHERE e.ledgerAccount.merchantId = :merchantId
       """)
    BigDecimal calculateRealAvailableBalance(@Param("merchantId") UUID merchantId);

    /**
     * QUERY OTIMIZADA DASHBOARD:
     * Retorna Disponível e Futuro em uma única execução.
     * 1. Available: (Créditos Disponíveis - Todos os Débitos)
     * 2. Future: (Apenas Créditos Futuros)
     */
    @Query(value = """
        SELECT 
            COALESCE(SUM(
                CASE 
                    WHEN e.type = 'DEBIT' THEN -e.amount
                    WHEN e.type = 'CREDIT' AND e.available_at <= NOW() THEN e.amount
                    ELSE 0 
                END
            ), 0) as availableBalance,
            
            COALESCE(SUM(
                CASE 
                    WHEN e.type = 'CREDIT' AND e.available_at > NOW() THEN e.amount
                    ELSE 0 
                END
            ), 0) as futureReceivables
        FROM accounting.ledger_entry e
        JOIN accounting.ledger_account a ON e.account_id = a.id
        WHERE a.merchant_id = :merchantId
    """, nativeQuery = true)
    LedgerBalanceProjection getBalances(@Param("merchantId") UUID merchantId);
}