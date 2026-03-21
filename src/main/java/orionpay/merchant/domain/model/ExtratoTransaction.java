package orionpay.merchant.domain.model;

import lombok.Builder;
import lombok.Value;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class ExtratoTransaction {
    UUID id;
    String nsu;
    BigDecimal amount;
    LocalDateTime createdAt;
    String brand;        // Visa, Mastercard, etc.
    String lastFour;     // **** 1234
    String status;       // APPROVED, DECLINED, etc.
    String externalId;   // tx_123456
}