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
       SELECT COALESCE(SUM(
           CASE 
               WHEN e.type IN ('CREDIT', 'PREPAYMENT_CREDIT', 'WITHDRAWAL_REVERSAL', 'REFUND_REVERSAL') AND e.availableAt <= CURRENT_TIMESTAMP THEN e.amount 
               WHEN e.type IN ('DEBIT', 'WITHDRAWAL_HOLD', 'WITHDRAWAL_COMPLETED', 'PREPAYMENT_FEE', 'REFUND_HOLD', 'REFUND_DEBIT') THEN -e.amount 
               ELSE 0 
           END
       ), 0)
       FROM LedgerEntryEntity e
       WHERE e.ledgerAccount.merchantId = :merchantId
       """)
    BigDecimal calculateRealAvailableBalance(@Param("merchantId") UUID merchantId);

    /**
     * QUERY HÍBRIDA DASHBOARD (Fonte de Verdade):
     * 1. availableBalance: Vem do LEDGER (Créditos - Débitos passados)
     * 2. futureReceivables: Vem da AGENDA (Apenas SCHEDULED não bloqueados)
     * 3. blockedAmount: Vem da AGENDA (Registros Bloqueados)
     */
    @Query(value = """
        SELECT 
            -- SALDO DISPONÍVEL (LEDGER)
            (SELECT COALESCE(SUM(
                CASE 
                    WHEN le.type IN ('CREDIT', 'PREPAYMENT_CREDIT', 'WITHDRAWAL_REVERSAL', 'REFUND_REVERSAL') AND le.available_at <= NOW() THEN le.amount
                    WHEN le.type IN ('DEBIT', 'WITHDRAWAL_HOLD', 'WITHDRAWAL_COMPLETED', 'PREPAYMENT_FEE', 'REFUND_HOLD', 'REFUND_DEBIT') THEN -le.amount
                    ELSE 0 
                END
            ), 0) FROM accounting.ledger_entry le JOIN accounting.ledger_account la ON le.account_id = la.id WHERE la.merchant_id = :merchantId) as availableBalance,
            
            -- A RECEBER (AGENDA - Somente Agendados e Não Antecipados)
            (SELECT COALESCE(SUM(se.net_amount), 0) FROM ops.settlement_entry se 
             WHERE se.merchant_id = :merchantId 
             AND se.status = 'SCHEDULED' 
             AND (se.is_blocked = false OR se.is_blocked IS NULL)
             AND (se.is_anticipated = false OR se.is_anticipated IS NULL)) as futureReceivables,
             
            -- COMPROMETIDO / BLOQUEADO (AGENDA)
            (SELECT COALESCE(SUM(se.net_amount), 0) FROM ops.settlement_entry se 
             WHERE se.merchant_id = :merchantId 
             AND se.is_blocked = true) as blockedAmount
    """, nativeQuery = true)
    LedgerBalanceProjection getBalances(@Param("merchantId") UUID merchantId);

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
}
