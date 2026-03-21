package orionpay.merchant.domain.excepion;

public class DomainException extends RuntimeException {

    private final String code;

    public DomainException(String message) {
        super(message);
        this.code = "BUSINESS_RULE_VIOLATION";
    }

    public DomainException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}