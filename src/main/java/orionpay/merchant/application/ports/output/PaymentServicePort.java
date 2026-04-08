package orionpay.merchant.application.ports.output;

import java.math.BigDecimal;

public interface PaymentServicePort {
    boolean processPixPayout(String pixKey, BigDecimal amount);
}
