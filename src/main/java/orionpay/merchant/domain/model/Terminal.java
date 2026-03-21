package orionpay.merchant.domain.model;

import lombok.Getter;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.enums.TerminalStatus;

import java.util.UUID;

@Getter
public class Terminal {

    private final UUID id;
    private final String serialNumber;
    private String model;
    private TerminalStatus status;
    private final UUID merchantId;

    public Terminal(UUID id, String serialNumber, String model, UUID merchantId) {
        if (serialNumber == null || serialNumber.isBlank()) {
            throw new DomainException("Serial Number é obrigatório para o terminal.");
        }
        this.id = id;
        this.serialNumber = serialNumber;
        this.model = model;
        this.merchantId = merchantId;
        this.status = TerminalStatus.INACTIVE; // Todo terminal nasce inativo
    }

    public void activate() {
        this.status = TerminalStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = TerminalStatus.INACTIVE;
    }
}