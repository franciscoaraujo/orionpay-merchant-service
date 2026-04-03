package orionpay.merchant.domain.model.enums;

import lombok.Getter;

@Getter
public enum EntryType {
    CREDIT("Crédito", 1),
    DEBIT("Débito", -1),
    WITHDRAWAL_HOLD("Reserva de Saque", -1),      // Primeiro passo do Saque
    WITHDRAWAL_COMPLETED("Saque Concluído", -1), // Sucesso do Saque (baixa real)
    WITHDRAWAL_REVERSAL("Estorno de Saque", 1);  // Falha do Saque (devolução)

    private final String description;
    private final int multiplier;

    EntryType(String description, int multiplier) {
        this.description = description;
        this.multiplier = multiplier;
    }
}
