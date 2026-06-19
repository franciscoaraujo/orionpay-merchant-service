package orionpay.merchant.domain.excepion;

public class InsufficientFundsException extends DomainException {
    public InsufficientFundsException(String message) {
        super(message, "INSUFFICIENT_FUNDS");
    }
}