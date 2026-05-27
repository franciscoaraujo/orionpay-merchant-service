package orionpay.merchant.infrastructure.adapters.input.rest.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PayoutHistoryResponse {
    private UUID id;
    private BigDecimal amount;
    private String pixKey;
    private String status; // SUCCESS, FAILED, PROCESSING
    private LocalDateTime createdAt;
    private String errorMessage;
}
