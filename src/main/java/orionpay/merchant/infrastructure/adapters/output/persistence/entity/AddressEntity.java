package orionpay.merchant.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "merchant_address", schema = "core")
@Getter
@Setter
@NoArgsConstructor
public class AddressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false, unique = true)
    private MerchantEntity merchant;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false, length = 10)
    private String number;

    private String complement;

    @Column(nullable = false)
    private String neighborhood;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false, length = 2)
    private String state; // UF (Ex: SP, RJ)

    @Column(name = "zip_code", nullable = false, length = 8)
    private String zipCode;

    @Column(name = "is_main_address")
    private boolean mainAddress = true;
}