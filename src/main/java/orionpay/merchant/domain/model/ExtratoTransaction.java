package orionpay.merchant.domain.model;

import lombok.Builder;
import lombok.Data;
import orionpay.merchant.domain.model.enums.ProductType;
import orionpay.merchant.domain.model.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ExtratoTransaction {
    private UUID id;
    private String nsu;
    private BigDecimal amount;
    private BigDecimal netAmount;
    private ProductType productType;
    private TransactionStatus status;
    private String authCode;
    private String cardBrand;
    private String cardLastFour;
    private LocalDateTime createdAt;
    private String errorMessage;
}
