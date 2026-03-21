package orionpay.merchant.infrastructure.adapters.output.persistence.projection;

import java.math.BigDecimal;

public interface TransactionSummaryProjection {

    BigDecimal getTotalVolume();    // Para o R$ 100,00

    BigDecimal getApprovedVolume(); // Para o R$ 96,50

    Long getTotalCount();

    Long getApprovedCount();
}
