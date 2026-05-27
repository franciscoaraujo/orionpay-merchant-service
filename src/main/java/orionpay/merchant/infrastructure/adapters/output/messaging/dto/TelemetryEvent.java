package orionpay.merchant.infrastructure.adapters.output.messaging.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO para eventos de telemetria enviados via RabbitMQ.
 *
 * @param serviceName O nome do microserviço que origina o evento.
 * @param action      A ação específica que ocorreu (ex: "TRANSACTION_AUTHORIZED").
 * @param payload     Um mapa flexível com dados relevantes para a telemetria.
 * @param timestamp   O momento em que o evento foi gerado.
 */
public record TelemetryEvent(
    String serviceName,
    String action,
    Map<String, Object> payload,
    LocalDateTime timestamp
) {
}
