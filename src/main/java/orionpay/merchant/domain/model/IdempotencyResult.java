package orionpay.merchant.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdempotencyResult implements Serializable {
    private String status; // "PROCESSING", "SUCCESS", "ERROR"
    private Object responseBody; // O DTO de resposta (ex: TransactionResponse)
    private String errorMessage;
}