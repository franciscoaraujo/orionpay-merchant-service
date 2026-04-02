package orionpay.merchant.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import orionpay.merchant.domain.model.enums.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlement_entry", schema = "ops", 
       uniqueConstraints = {@UniqueConstraint(name = "uk_settlement_transaction_installment", 
                                            columnNames = {"transaction_id", "installment_number"})})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "installment_number")
    private Integer installmentNumber;

    @Column(name = "total_installments")
    private Integer totalInstallments;

    @Column(name = "terminal_id")
    private String terminalId;

    @Column(name = "mdr_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal mdrAmount;

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal netAmount;

    @Column(name = "expected_settlement_date", nullable = false)
    private LocalDateTime expectedSettlementDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SettlementStatus status;

    @Column(name = "is_blocked")
    private Boolean blocked;

    @Column(name = "is_anticipated")
    private Boolean anticipated;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "mdr_rate", precision = 5, scale = 4)
    private BigDecimal mdrRate;

    @Column(name = "mdr_percentage", precision = 5, scale = 4)
    private BigDecimal mdrPercentage;

    @UpdateTimestamp
    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "original_amount", precision = 19, scale = 4)
    private BigDecimal originalAmount;
}
