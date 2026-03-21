package orionpay.merchant.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import orionpay.merchant.domain.model.enums.AccountType;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "merchant_bank_account", schema = "ops")
@Getter
@Setter
public class BankAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false, unique = true)
    private MerchantEntity merchant;

    @Column(name = "bank_code", length = 3, nullable = false)
    private String bankCode;

    @Column(nullable = false, length = 10)
    private String branch;

    @Column(nullable = false, length = 20)
    private String account;

    @Column(name = "account_digit", length = 2)
    private String accountDigit;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(nullable = false)
    private boolean verified = false;

}