package orionpay.merchant.infrastructure.adapters.output.persistence.projection;

import java.math.BigDecimal;

public interface LedgerBalanceProjection {
    BigDecimal getAvailableBalance();
    BigDecimal getFutureReceivables();
    BigDecimal getBlockedAmount();
}
