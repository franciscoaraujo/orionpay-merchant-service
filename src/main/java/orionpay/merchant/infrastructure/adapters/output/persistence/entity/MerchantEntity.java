package orionpay.merchant.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import orionpay.merchant.domain.model.enums.MerchantStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "merchant", schema = "core")
@Getter
@Setter
public class MerchantEntity {

    @Id
    private UUID id;

    @Column(unique = true, nullable = false)
    private String document;

    @Column(name = "legal_name")
    private String legalName;

    private String email; // Adicionado para suportar o domínio, embora ausente no DDL original

    @Enumerated(EnumType.STRING)
    private MerchantStatus status;

    // Correção: mappedBy indica que a FK está na outra ponta (BankAccountEntity)
    @OneToOne(mappedBy = "merchant", cascade = CascadeType.ALL)
    private BankAccountEntity bankAccount;

    // Adicionado: Relacionamento com AddressEntity
    @OneToOne(mappedBy = "merchant", cascade = CascadeType.ALL)
    private AddressEntity address;

    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL)
    private List<PricingEntity> pricings;

    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL)
    private List<TransactionEntity> transactions;

    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL)
    private List<TerminalEntity> terminals;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}