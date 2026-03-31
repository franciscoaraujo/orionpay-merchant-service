package orionpay.merchant.infrastructure.adapters.output.persistence.projection;

import java.math.BigDecimal;

public interface TransactionStatsProjection {
    BigDecimal getTotalVolume();
    BigDecimal getNetVolume();
    Long getTotalCount();
    Long getApprovedCount();
}