package orionpay.merchant.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UpdateTimestamp;
import java.util.UUID;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ledger_account", schema = "accounting")
@NoArgsConstructor
public class LedgerAccountEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false, unique = true)
    private UUID ledgerAccountId;

    @Column(name = "merchant_id", nullable = false, unique = true)
    private UUID merchantId;

    @Column(name = "account_code", nullable = false, unique = true)
    private String accountCode;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean active = true;

    @Column(name = "last_update")
    @UpdateTimestamp
    private LocalDateTime lastUpdate;

    @Version
    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long version = 0L;
}