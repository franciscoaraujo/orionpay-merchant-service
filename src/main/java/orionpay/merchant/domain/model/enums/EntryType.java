package orionpay.merchant.domain.model.enums;

import lombok.Getter;

@Getter
public enum EntryType {
    CREDIT("Crédito", 1),
    DEBIT("Débito", -1),
    WITHDRAWAL_HOLD("Reserva de Saque", -1),
    WITHDRAWAL_COMPLETED("Saque Concluído", -1),
    WITHDRAWAL_REVERSAL("Estorno de Saque", 1),
    PREPAYMENT_CREDIT("Crédito de Antecipação", 1),
    PREPAYMENT_FEE("Taxa de Antecipação", -1),
    REFUND_HOLD("Reserva de Estorno", -1),      // Bloqueia o saldo para o estorno
    REFUND_DEBIT("Estorno Concluído", -1),      // Baixa definitiva do saldo
    REFUND_REVERSAL("Estorno Revertido", 1);    // Devolve o saldo se o estorno falhar

    private final String description;
    private final int multiplier;

    EntryType(String description, int multiplier) {
        this.description = description;
        this.multiplier = multiplier;
    }
}
