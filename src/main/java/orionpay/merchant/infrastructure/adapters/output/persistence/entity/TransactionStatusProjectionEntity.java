package orionpay.merchant.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import orionpay.merchant.domain.model.enums.TransactionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction_status_projection", schema = "core")
@Getter
@Setter
public class TransactionStatusProjectionEntity {

    @Id
    @Column(name = "transaction_id")
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    private TransactionStatus currentStatus;

    private LocalDateTime lastUpdate;

    @Column(name = "is_fully_settled")
    private boolean isFullySettled;
}