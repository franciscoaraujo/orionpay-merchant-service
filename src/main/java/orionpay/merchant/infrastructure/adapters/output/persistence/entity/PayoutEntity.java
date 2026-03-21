package orionpay.merchant.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payout", schema = "accounting")
@Data
@NoArgsConstructor
public class PayoutEntity {

    @Id
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "pix_key", nullable = false)
    private String pixKey;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PayoutStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum PayoutStatus {
        PENDING,
        COMPLETED,
        FAILED
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = PayoutStatus.PENDING;
    }
}