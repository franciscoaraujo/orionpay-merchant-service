package orionpay.merchant.domain.model.enums;

public enum TerminalStatus {
    /**
     * O terminal foi fabricado/comprado, mas ainda não foi vinculado a ninguém
     * ou está em estoque.
     */
    AVAILABLE("Disponível"),

    /**
     * O terminal está em mãos do lojista e pronto para transacionar.
     */
    ACTIVE("Ativo"),

    /**
     * O terminal foi desativado temporariamente (ex: suspeita de fraude
     * ou manutenção). Impede transações.
     */
    INACTIVE("Inativo"),

    /**
     * O terminal foi perdido, roubado ou danificado permanentemente.
     * Nunca mais pode voltar a ser ACTIVE.
     */
    TERMINATED("Encerrado/Descartado"),

    /**
     * O terminal foi enviado para o lojista, mas ainda não foi ligado
     * pela primeira vez para ativação.
     */
    IN_TRANSIT("Em Trânsito");

    private final String description;

    TerminalStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    // REGRA DE NEGÓCIO: Valida se o terminal pode realizar uma venda
    public boolean canTransact() {
        return this == ACTIVE;
    }

    // REGRA DE NEGÓCIO: Valida se o terminal pode ser reativado
    public boolean canBeReactivated() {
        return this == INACTIVE || this == IN_TRANSIT;
    }
}