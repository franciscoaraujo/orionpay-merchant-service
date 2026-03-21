package orionpay.merchant.domain.model.enums;

public enum MerchantStatus {
    /**
     * Lojista acabou de se cadastrar.
     * Pode acessar o portal, mas não pode transacionar nem receber pagamentos.
     */
    PROVISIONAL("Provisório / Em Análise"),

    /**
     * Cadastro aprovado e conta contábil criada.
     * Totalmente operacional.
     */
    ACTIVE("Ativo"),

    /**
     * Bloqueio temporário (ex: suspeita de fraude, falta de documentos ou disputa judicial).
     * Transações podem ser negadas e pagamentos retidos.
     */
    SUSPENDED("Suspenso"),

    /**
     * O contrato foi encerrado.
     * Acesso ao portal pode ser restrito apenas a consulta de extratos antigos.
     */
    TERMINATED("Encerrado"),

    /**
     * Lojista aprovado, mas aguardando a ativação do primeiro terminal
     * ou a primeira transação para validar o fluxo.
     */
    AWAITING_ACTIVATION("Aguardando Ativação");

    private final String description;

    MerchantStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    // REGRA DE NEGÓCIO: Define se o lojista pode receber depósitos (Settlement)
    public boolean canReceivePayments() {
        return this == ACTIVE || this == AWAITING_ACTIVATION;
    }

    // REGRA DE NEGÓCIO: Define se o lojista pode realizar novas vendas
    public boolean canTransact() {
        return this == ACTIVE;
    }
}