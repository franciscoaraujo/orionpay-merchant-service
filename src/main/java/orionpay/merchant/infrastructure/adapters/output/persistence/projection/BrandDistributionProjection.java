package orionpay.merchant.infrastructure.adapters.output.persistence.projection;


import java.math.BigDecimal;

public interface BrandDistributionProjection {
    String getBrand();

    BigDecimal getValue();

    Double getPercentage();
}