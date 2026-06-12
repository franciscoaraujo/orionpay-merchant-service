package orionpay.merchant.infrastructure.adapters.input.rest.dto;

import lombok.Builder;
import lombok.Data;
import orionpay.merchant.domain.model.enums.ProductType;
import orionpay.merchant.domain.model.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TransactionExtratoResponse {
    private UUID id;
    private BigDecimal amount;
    private BigDecimal netAmount;
    private ProductType productType;
    private TransactionStatus status;
    private String nsu;
    private String authCode;
    private String cardBrand;
    private String cardLastFour;
    private LocalDateTime createdAt;
    private String errorMessage;
}
