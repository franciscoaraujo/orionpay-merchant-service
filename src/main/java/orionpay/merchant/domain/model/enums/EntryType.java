package orionpay.merchant.domain.model.enums;

public enum EntryType {
    /**
     * Entrada de recurso na conta do lojista.
     * Ex: Liquidação de uma venda (Settlement) ou ajuste a favor.
     */
    CREDIT("Crédito"),

    /**
     * Saída de recurso da conta do lojista.
     * Ex: Cobrança de taxas, estorno de venda (Chargeback) ou transferência bancária.
     */
    DEBIT("Débito");

    private final String description;

    EntryType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * REGRA DE NEGÓCIO: Define o multiplicador para cálculo de saldo.
     * Créditos somam (1), Débitos subtraem (-1).
     */
    public int getMultiplier() {
        return this == CREDIT ? 1 : -1;
    }
}