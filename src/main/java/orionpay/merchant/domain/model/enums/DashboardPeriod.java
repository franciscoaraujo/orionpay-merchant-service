package orionpay.merchant.domain.model.enums;

import lombok.Getter;
import java.time.LocalDate;

@Getter
public enum DashboardPeriod {
    HOJE("Hoje") {
        @Override
        public PeriodRange getRanges(LocalDate today) {
            return new PeriodRange(today, today, today.minusDays(1), today.minusDays(1));
        }
    },
    ONTEM("Ontem") {
        @Override
        public PeriodRange getRanges(LocalDate today) {
            LocalDate yesterday = today.minusDays(1);
            return new PeriodRange(yesterday, yesterday, today.minusDays(2), today.minusDays(2));
        }
    },
    MES_ATUAL("Mês Atual") {
        @Override
        public PeriodRange getRanges(LocalDate today) {
            LocalDate startCurrent = today.withDayOfMonth(1);
            LocalDate startPrev = startCurrent.minusMonths(1);
            LocalDate endPrev = startPrev.withDayOfMonth(startPrev.lengthOfMonth());
            return new PeriodRange(startCurrent, today, startPrev, endPrev);
        }
    },
    ULTIMOS_30_DIAS("Últimos 30 Dias") {
        @Override
        public PeriodRange getRanges(LocalDate today) {
            LocalDate startCurrent = today.minusDays(30);
            LocalDate startPrev = startCurrent.minusDays(30);
            LocalDate endPrev = startCurrent.minusDays(1);
            return new PeriodRange(startCurrent, today, startPrev, endPrev);
        }
    };

    private final String description;

    DashboardPeriod(String description) {
        this.description = description;
    }

    public abstract PeriodRange getRanges(LocalDate today);

    public record PeriodRange(LocalDate startCurrent, LocalDate endCurrent, LocalDate startPrev, LocalDate endPrev) {}

    public static DashboardPeriod fromString(String value) {
        for (DashboardPeriod period : values()) {
            if (period.name().equalsIgnoreCase(value)) {
                return period;
            }
        }
        return HOJE;
    }
}
