package orionpay.merchant.domain.model.enums;

public enum ProductType {
    /**
     * Venda no Débito.
     * Liquidação geralmente em D+1. Menor taxa (MDR).
     */
    DEBIT("Débito"),

    /**
     * Venda no Crédito à Vista (1 parcela).
     * Liquidação geralmente em D+30.
     */
    CREDIT_A_VISTA("Crédito à Vista"),

    /**
     * Venda no Crédito Parcelado pelo Lojista.
     * Liquidação ocorre conforme as parcelas (D+30, D+60...) ou via Antecipação.
     */
    CREDIT_PARCELADO("Crédito Parcelado");

    private final String description;

    ProductType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * REGRA DE NEGÓCIO: Define se o produto é passível de
     * antecipação de recebíveis (apenas crédito).
     */
    public boolean isEligibleForAnticipation() {
        return this == CREDIT_A_VISTA || this == CREDIT_PARCELADO;
    }

    /**
     * REGRA DE NEGÓCIO: Define o prazo padrão de liquidação em dias.
     * DEBIT = D+1
     * CREDIT = D+30
     */
    public int getSettlementDays() {
        return switch (this) {
            case DEBIT -> 1;
            case CREDIT_A_VISTA, CREDIT_PARCELADO -> 30; // Simplificação para D+30 na primeira parcela/vista
        };
    }
}