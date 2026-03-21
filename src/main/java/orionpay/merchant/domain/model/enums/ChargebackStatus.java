package orionpay.merchant.domain.model.enums;

public enum ChargebackStatus {
    /**
     * Disputa recebida pela bandeira.
     * O valor é preventivamente debitado ou bloqueado na agenda do lojista.
     */
    OPEN("Aberto / Notificado"),

    /**
     * O lojista enviou documentos (comprovantes, logs) para contestar o chargeback.
     * Aguardando análise da adquirente ou do emissor.
     */
    UNDER_REVIEW("Em Análise de Defesa"),

    /**
     * O lojista venceu a disputa.
     * O valor deve ser devolvido (recreditado) ao lojista.
     */
    REVERSED("Disputa Vencida (Estornado)"),

    /**
     * O lojista perdeu a disputa ou não apresentou defesa no prazo.
     * O débito torna-se definitivo.
     */
    LOST("Disputa Perdida"),

    /**
     * Quando há uma reapresentação do chargeback pela bandeira após a defesa (Pre-arbitration).
     */
    REPRESENTMENT("Reapresentado");

    private final String description;

    ChargebackStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * REGRA DE NEGÓCIO: Define se o lojista ainda pode enviar documentos.
     */
    public boolean canPresentDefense() {
        return this == OPEN;
    }

    /**
     * REGRA DE NEGÓCIO: Define se este status representa um prejuízo financeiro final.
     */
    public boolean isFinalLoss() {
        return this == LOST;
    }
}