package orionpay.merchant.infrastructure.adapters.output.persistence.projection;

import java.math.BigDecimal;

/**
 * Projeção para retornar saldos Agrupados em uma única query.
 */
public interface LedgerBalanceProjection {
    BigDecimal getAvailableBalance();
    BigDecimal getFutureReceivables();
}