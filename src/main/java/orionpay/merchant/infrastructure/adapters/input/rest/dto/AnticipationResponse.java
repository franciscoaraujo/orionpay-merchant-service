package orionpay.merchant.infrastructure.adapters.input.rest.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AnticipationResponse {
    private BigDecimal totalGrossToAnticipate;
    private BigDecimal totalCost;
    private BigDecimal totalNetToReceive;
    private List<AvailableSettlement> items;

    @Data
    @Builder
    public static class AvailableSettlement {
        private UUID settlementId;
        private LocalDateTime originalSettlementDate;
        private BigDecimal grossAmount;
        private BigDecimal netAmount;
        private BigDecimal anticipationCost; // Calculado individualmente
        private int daysToAnticipate; // Dias entre hoje e a data original
    }
}
