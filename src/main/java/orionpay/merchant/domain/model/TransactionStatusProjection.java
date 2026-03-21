package orionpay.merchant.domain.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import orionpay.merchant.domain.model.enums.TransactionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
public class TransactionStatusProjection {
    private final UUID transactionId;
    private TransactionStatus currentStatus;
    private LocalDateTime lastUpdate;
    private boolean isFullySettled;

    public void updateStatus(TransactionStatus newStatus) {
        // A projeção apenas reflete a verdade; a lógica de transição está no Model Transaction
        this.currentStatus = newStatus;
        this.lastUpdate = LocalDateTime.now();
    }
}