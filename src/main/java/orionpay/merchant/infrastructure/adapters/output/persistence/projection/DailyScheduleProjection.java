package orionpay.merchant.infrastructure.adapters.output.persistence.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyScheduleProjection {
    LocalDate getSettlementDate();
    BigDecimal getTotalGross();
    BigDecimal getTotalNet();
    String getStatuses();
    Long getCount();
}
