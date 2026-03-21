package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import orionpay.merchant.domain.model.LedgerAccount;
import orionpay.merchant.domain.model.enums.EntryType;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.LedgerBalanceProjection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface LedgerRepository {
    Optional<LedgerAccount> findByMerchantId(UUID merchantId);

    void saveAccount(LedgerAccount account);

    void saveEntry(LedgerAccount account, BigDecimal amount, EntryType type, String description, UUID correlationId, LocalDateTime availableAt);

    BigDecimal findAvailableBalance(UUID merchantId);
    BigDecimal findFutureReceivables(UUID merchantId);
    BigDecimal findRealAvailableBalance(UUID merchantId);

    // Novo método otimizado
    LedgerBalanceProjection getLedgerBalances(UUID merchantId);
}