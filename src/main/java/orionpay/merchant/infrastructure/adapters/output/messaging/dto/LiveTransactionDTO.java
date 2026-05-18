package orionpay.merchant.infrastructure.adapters.output.messaging.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO (Data Transfer Object) para representar um evento de transação em tempo real
 * enviado via WebSocket para dashboards de observabilidade.
 *
 * @param transactionId O ID único da transação.
 * @param merchantId    O ID do lojista associado.
 * @param amount        O valor da transação.
 * @param status        O status final da autorização (ex: "APPROVED").
 * @param step          A etapa do fluxo em que o evento foi gerado (ex: "OUTBOX_SAVED").
 * @param timestamp     O momento exato em que o evento foi criado.
 */
public record LiveTransactionDTO(
    UUID transactionId,
    UUID merchantId,
    BigDecimal amount,
    String status,
    String step,
    LocalDateTime timestamp
) {
}
