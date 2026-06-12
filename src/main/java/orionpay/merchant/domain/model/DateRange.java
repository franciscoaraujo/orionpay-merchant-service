package orionpay.merchant.domain.model;

import java.time.LocalDateTime;

/**
 * Record que representa um intervalo de tempo com início e fim.
 */
public record DateRange(LocalDateTime start, LocalDateTime end) {
}
