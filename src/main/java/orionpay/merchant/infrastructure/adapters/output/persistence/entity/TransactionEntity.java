package orionpay.merchant.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import orionpay.merchant.domain.model.enums.ProductType;
import orionpay.merchant.domain.model.enums.TransactionStatus;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.vo.TransactionSourceVo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private MerchantEntity merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id") // Vínculo opcional se vier de e-commerce
    private TerminalEntity terminal;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "net_amount", precision = 19, scale = 2)
    private BigDecimal netAmount;

    @Column(length = 20)
    private String nsu;

    @Column(name = "auth_code", length = 20)
    private String authCode;

    @Column(name = "authorization_code", length = 50) // Campo extra solicitado
    private String authorizationCode;

    @Column(length = 3)
    private String currency = "BRL";

    @Column(name = "product_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductType productType;

    @Column(name = "card_bin", length = 6)
    private String cardBin;

    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Column(name = "card_brand", length = 20)
    private String cardBrand;

    @Column(name = "card_holder_name")
    private String cardHolderName;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Embedded
    private TransactionSourceVo source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
