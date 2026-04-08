package orionpay.merchant.infrastructure.adapters.input.rest.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class SettlementScheduleResponse {
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalPeriodGross;
    private BigDecimal totalPeriodNet;
    private Integer totalTransactionsInPeriod;
    private List<DailySchedule> schedule;

    @Data
    @Builder
    public static class DailySchedule {
        private LocalDate date;
        private BigDecimal totalGross;
        private BigDecimal totalNet;
        private Set<String> statusSummary;
        private Integer transactionCount;

        // Campos calculados úteis
        private BigDecimal mdrAmount;
        private BigDecimal dailyAverageTransaction;
    }
}
