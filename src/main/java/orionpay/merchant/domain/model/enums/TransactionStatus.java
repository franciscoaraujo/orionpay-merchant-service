package orionpay.merchant.domain.model.enums;

public enum TransactionStatus {
    /**
     * Transação recebida, mas aguardando processamento ou
     * análise de antifraude (Score).
     */
    PENDING("Pendente"),

    /**
     * Transação autorizada pelo emissor e capturada com sucesso.
     * Pronta para entrar na agenda de liquidação.
     */
    APPROVED("Aprovada"),

    /**
     * Negada pelo emissor (falta de saldo, cartão bloqueado, etc)
     * ou pelo nosso antifraude.
     */
    DECLINED("Recusada"),

    /**
     * Venda cancelada pelo lojista ou estornada antes da liquidação.
     */
    CANCELLED("Cancelada"),

    /**
     * Venda que já havia sido aprovada, mas foi devolvida ao portador do cartão.
     */
    REVERSED("Estornada"),

    /**
     * O portador do cartão contestou a venda junto ao banco (Chargeback).
     * Status crítico que gera débito imediato na agenda do lojista.
     */
    CHARGED_BACK("Contestada (Chargeback)");

    private final String description;

    TransactionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * REGRA DE NEGÓCIO: Define se a transação deve gerar crédito
     * no saldo contábil do lojista.
     */
    public boolean isEligibleForSettlement() {
        return this == APPROVED;
    }

    /**
     * REGRA DE NEGÓCIO: Define se a transação pode ser cancelada/estornada.
     */
    public boolean canBeReversed() {
        return this == APPROVED;
    }
}