package orionpay.merchant.domain.model.enums;

public enum AccountType {
    /**
     * Conta corrente tradicional em bancos comerciais.
     */
    CHECKING("Conta Corrente"),

    /**
     * Conta poupança. Geralmente evitada para recebimento de vendas
     * comerciais, mas permitida para MEI/Pessoa Física.
     */
    SAVINGS("Conta Poupança"),

    /**
     * Contas digitais em Instituições de Pagamento (IP).
     * Muito comum no ecossistema de subadquirença.
     */
    PAYMENT("Conta de Pagamento");

    private final String description;

    AccountType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * REGRA DE NEGÓCIO: Valida se o tipo de conta é elegível para
     * receber antecipação de recebíveis.
     */
    public boolean allowsAnticipation() {
        // Exemplo: Permitir apenas para Corrente e Pagamento
        return this == CHECKING || this == PAYMENT;
    }
}