package orionpay.merchant.application.ports.output;

import java.util.Map;

/**
 * Porta de saída para a publicação de eventos de telemetria.
 * As implementações devem garantir que falhas na publicação não afetem o fluxo de negócio.
 */
public interface TelemetryPublisherPort {

    /**
     * Publica um evento de telemetria de forma assíncrona ou não-bloqueante.
     *
     * @param serviceName O nome do serviço que está publicando.
     * @param action      Uma string que descreve a ação (ex: "TRANSACTION_AUTHORIZED").
     * @param payload     Um mapa contendo dados relevantes para o evento.
     */
    void publishTelemetry(String serviceName, String action, Map<String, Object> payload);
}
