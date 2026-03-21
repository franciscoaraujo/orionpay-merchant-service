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

    @ManyToOne
    @JoinColumn(name = "merchant_id")
    private MerchantEntity merchant;


    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "net_amount")
    private BigDecimal netAmount;

    @Column(length = 20)
    private String nsu;

    // Ajustado para bater com o nome exato 'auth_code' do seu DDL
    @Column(name = "auth_code", length = 20)
    private String authCode;

    @Column(length = 3)
    private String currency = "BRL";

    @Column(name = "product_type") // Mapeia para a coluna com Check Constraint
    @Enumerated(EnumType.STRING)
    private ProductType productType;

    @Column(name = "card_bin", length = 6)
    private String cardBin; // Primeiros 6 dígitos

    @Column(name = "card_last_four", length = 4)
    private String cardLastFour; // Últimos 4 dígitos para o portal

    @Column(name = "card_brand", length = 20)
    private String cardBrand; // VISA, MASTERCARD, ELO

    @Column(name = "card_holder_name")
    private String cardHolderName;

    @Embedded
    private TransactionSourceVo source;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(name = "created_at") // Conforme definido no DDL
    private LocalDateTime createdAt;
}