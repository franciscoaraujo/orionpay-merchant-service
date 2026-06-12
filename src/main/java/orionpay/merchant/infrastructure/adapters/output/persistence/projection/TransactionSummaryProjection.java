package orionpay.merchant.infrastructure.adapters.output.persistence.projection;

import java.math.BigDecimal;

public interface TransactionSummaryProjection {

    BigDecimal getTotalVolume();    // TPV Bruto

    BigDecimal getNetVolume();      // Net Revenue (TPV - MDR)

    Long getTotalCount();

    Long getApprovedCount();
}
