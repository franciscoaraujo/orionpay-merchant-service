package orionpay.merchant.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import orionpay.merchant.domain.model.enums.EntryType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_entry", schema = "accounting")
@Getter // Retornando Lombok para facilitar, já que o problema de compilação foi resolvido no pom.xml
@Setter
public class LedgerEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private LedgerAccountEntity ledgerAccount;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private EntryType type; // DEBIT or CREDIT

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id", nullable = false)
    private JournalEntity journal;

    @Column(name = "correlation_id")
    private UUID correlationId; // Link com a venda que gerou este lançamento

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt; // Data de liquidação (D+1, D+30)
}