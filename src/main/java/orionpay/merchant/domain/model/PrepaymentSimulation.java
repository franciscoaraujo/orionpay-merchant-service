package orionpay.merchant.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class PrepaymentSimulation {
    private final UUID settlementId;
    private final BigDecimal originalNetValue;
    private final BigDecimal monthlyFee;
    private final LocalDate originalDate;

    public BigDecimal calculateCost(LocalDate referenceDate) {
        long days = ChronoUnit.DAYS.between(referenceDate, originalDate);
        if (days <= 0) return BigDecimal.ZERO;

        BigDecimal dailyFee = monthlyFee.divide(BigDecimal.valueOf(30), 8, RoundingMode.HALF_UP)
                                       .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);

        return originalNetValue.multiply(dailyFee).multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateNetFinal(LocalDate referenceDate) {
        return originalNetValue.subtract(calculateCost(referenceDate));
    }
}
