package orionpay.merchant.domain.excepion;

public class PayoutPendingException extends RuntimeException {
    public PayoutPendingException(String message) {
        super(message);
    }
}
