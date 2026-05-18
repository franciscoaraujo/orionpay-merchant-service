package orionpay.merchant.infrastructure.adapters.input.rest.dto;

import java.math.BigDecimal;

/**
 * DTO para o snapshot de métricas do dashboard.
 *
 * @param totalTransactions   Total de transações aprovadas no dia.
 * @param totalVolume         Volume financeiro total (TPV) do dia.
 * @param pendingOutboxEvents Número de eventos aguardando no outbox para serem processados.
 */
public record DashboardMetricsDTO(
    Long totalTransactions,
    BigDecimal totalVolume,
    Integer pendingOutboxEvents
) {}
