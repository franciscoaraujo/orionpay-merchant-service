package orionpay.merchant.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import orionpay.merchant.domain.model.enums.ProductType;

import java.util.UUID;

import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "merchant_pricing", schema = "ops")
@Getter
@Setter
public class PricingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "merchant_id")
    private MerchantEntity merchant;

    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type")
    private ProductType productType;

    @Column(name = "mdr_percentage")
    private BigDecimal mdrPercentage;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;
}