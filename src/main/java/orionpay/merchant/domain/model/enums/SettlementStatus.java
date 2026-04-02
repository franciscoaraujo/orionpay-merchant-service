package orionpay.merchant.domain.model.enums;

public enum SettlementStatus {
    PENDING,     // Registro criado, aguardando Ledger
    SCHEDULED,   // Contabilizado, aguardando data de vencimento (Aparece na Agenda)
    ANTICIPATED, // Valor já pago via antecipação
    BLOCKED,     // Travado (Garantia/Fraude)
    SETTLED,     // Pronto para envio ao banco (Payout iniciado)
    PAID,        // Confirmado pelo banco (Fim do ciclo)
    DISPUTE,     // Contestação em curso
    FAILED       // Erro de processamento
}