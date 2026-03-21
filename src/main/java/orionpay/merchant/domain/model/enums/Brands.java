package orionpay.merchant.domain.model.enums;

public enum Brands {
    VISA("visa"),
    MASTER_CARD("master-card"),
    ELO("elo"),
    AMEX("Amex"),
    HIPERCARD("Hipercard");

    private final String description;

    Brands(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
