package orionpay.merchant.infrastructure.adapters.output.persistence.projection;


import java.math.BigDecimal;

public interface HourlySalesProjection {
    int getHour();

    BigDecimal getToday();

    BigDecimal getYesterday();
}