package orionpay.merchant.infrastructure.adapters.input.rest.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class FinancialAgendaResponse {
    private AgendaSummary summary;
    private List<AgendaItem> items;
    private int totalPages;
    private long totalElements;

    @Data
    @Builder
    public static class AgendaSummary {
        private BigDecimal totalGrossAmount;
        private BigDecimal totalCommittedAmount;
        private BigDecimal totalAvailableAmount;
    }

    @Data
    @Builder
    public static class AgendaItem {
        private UUID idExt;
        private LocalDateTime settlementDate;
        private LocalDateTime transactionDate;
        private BigDecimal grossAmount;
        private BigDecimal mdrAmount;
        private BigDecimal netAmount;
        private String status; // PAGO, AGENDADO, PENDENTE
        private String titularidade; // VINCULADO A GARANTIA, ANTECIPADO, DISPONÍVEL
        
        // Detalhes
        private String nsu;
        private String cardBrand;
        private String cardLastFour;
        private String productType;
    }
}
