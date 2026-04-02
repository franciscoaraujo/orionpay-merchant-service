package orionpay.merchant.domain.excepion;

import lombok.Getter;

@Getter
public class BusinessResilienceException extends RuntimeException {
    private final String code;

    public BusinessResilienceException(String message, String code) {
        super(message);
        this.code = code;
    }
}
