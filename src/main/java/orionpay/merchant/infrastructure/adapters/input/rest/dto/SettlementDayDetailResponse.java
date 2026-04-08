package orionpay.merchant.infrastructure.adapters.input.rest.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO para detalhe de um dia específico na agenda de liquidação.
 * Retorna agregações e lista detalhada de transações/parcelas daquele dia.
 */
@Data
@Builder
public class SettlementDayDetailResponse {

    // Informações do dia
    private LocalDate settlementDate;

    // Totalizadores
    private BigDecimal totalGross;
    private BigDecimal totalMdr;
    private BigDecimal totalNet;
    private BigDecimal averageTransaction;

    // Contadores
    private Integer totalCount;
    private Integer blockedCount;
    private Integer anticipatedCount;

    // Breakdown de status
    private Map<String, Long> statusBreakdown;

    // Detalhes das transações
    private List<TransactionDetail> transactions;

    // Informações de paginação
    private Integer pageNumber;
    private Integer pageSize;
    private Integer totalPages;
    private Long totalElements;

    @Data
    @Builder
    public static class TransactionDetail {
        // IDs e Rastreabilidade
        private UUID idExt;
        private UUID transactionId;
        private String nsu;

        // Datas
        private LocalDateTime transactionDate;
        private LocalDateTime settlementDate;
        private LocalDateTime paidAt;

        // Valores
        private BigDecimal grossAmount;
        private BigDecimal originalAmount;
        private BigDecimal mdrPercentage;
        private BigDecimal mdrAmount;
        private BigDecimal netAmount;

        // Informações de Cartão
        private String cardBrand;
        private String cardLastFour;

        // Tipo de Produto
        private String productType;

        // Flags e Parcelas
        private Boolean blocked;
        private Boolean anticipated;
        private Integer installmentNumber;
        private String installmentLabel;  // Ex: "1/12", "À vista"

        // Status
        private String status;
    }
}

