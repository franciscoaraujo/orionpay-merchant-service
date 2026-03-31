package orionpay.merchant.domain.model.enums;

import org.springframework.stereotype.Component;
import orionpay.merchant.domain.model.DateRange;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Estratégia para cálculo de períodos dinâmicos do dashboard.
 */
@Component
public enum DashboardPeriod {

    HOJE {
        @Override
        public DateRange getRange() {
            LocalDateTime now = LocalDateTime.now();
            return new DateRange(now.toLocalDate().atStartOfDay(), now);
        }
    },

    ONTEM {
        @Override
        public DateRange getRange() {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            return new DateRange(yesterday.atStartOfDay(), yesterday.atTime(LocalTime.MAX));
        }
    },

    MES_ATUAL {
        @Override
        public DateRange getRange() {
            LocalDateTime now = LocalDateTime.now();
            return new DateRange(now.toLocalDate().withDayOfMonth(1).atStartOfDay(), now);
        }
    },

    ULTIMOS_30_DIAS {
        @Override
        public DateRange getRange() {
            LocalDateTime now = LocalDateTime.now();
            return new DateRange(now.minusDays(30).toLocalDate().atStartOfDay(), now);
        }
    };

    public abstract DateRange getRange();
}
