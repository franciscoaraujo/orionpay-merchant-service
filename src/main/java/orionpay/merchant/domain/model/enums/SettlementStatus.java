package orionpay.merchant.domain.model.enums;

public enum SettlementStatus {
    PENDING,     // Criado, aguardando Ledger
    SCHEDULED,   // Contabilizado, aguardando vencimento
    ANTICIPATED, // Antecipado pelo lojista
    BLOCKED,     // Travado (Garantia/Fraude)
    SETTLED,     // Pronto para payout
    PAID,        // Pago efetivamente ao lojista
    DISPUTE,     // Em disputa (Chargeback)
    FAILED,      // Erro de processamento
    PREPAID      // Liquidado antecipadamente
}
