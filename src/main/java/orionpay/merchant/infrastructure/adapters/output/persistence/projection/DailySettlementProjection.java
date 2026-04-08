package orionpay.merchant.infrastructure.adapters.output.persistence.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailySettlementProjection {
    LocalDate getExpectedSettlementDate();
    BigDecimal getTotalGrossAmount();
    BigDecimal getTotalNetAmount();
    String getStatusSummary(); // Retorna uma string concatenada de status
    Long getTransactionCount();
}
