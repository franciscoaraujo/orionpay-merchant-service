package orionpay.merchant.infrastructure.adapters.output.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import orionpay.merchant.application.ports.output.TelemetryPublisherPort;
import orionpay.merchant.infrastructure.adapters.output.messaging.dto.TelemetryEvent;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Implementação da porta de telemetria utilizando RabbitMQ.
 * Publica eventos de forma "fire-and-forget", logando falhas sem lançar exceções.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQTelemetryPublisher implements TelemetryPublisherPort {

    private static final String TELEMETRY_EXCHANGE = "orionpay.telemetry.exchange";
    private static final String ROUTING_KEY = "telemetry.merchant";

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishTelemetry(String serviceName, String action, Map<String, Object> payload) {
        try {
            log.debug("Publishing telemetry event. Action: {}", action);

            var telemetryEvent = new TelemetryEvent(
                serviceName,
                action,
                payload,
                LocalDateTime.now()
            );

            rabbitTemplate.convertAndSend(TELEMETRY_EXCHANGE, ROUTING_KEY, telemetryEvent);

        } catch (Exception e) {
            // A falha na publicação de telemetria é registrada, mas nunca deve interromper
            // o fluxo de negócio principal.
            log.warn(
                "Failed to publish telemetry event for action: {}. This will not affect the business transaction. Error: {}",
                action,
                e.getMessage()
            );
        }
    }
}
