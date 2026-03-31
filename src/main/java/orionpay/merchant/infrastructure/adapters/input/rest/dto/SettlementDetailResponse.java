package orionpay.merchant.infrastructure.adapters.input.rest.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SettlementDetailResponse {
    private UUID idExt;
    private UUID transactionId;
    private String nsu;
    private LocalDateTime settlementDate;
    private BigDecimal grossAmount;      // Valor da parcela
    private BigDecimal originalAmount;   // Valor total da venda
    private BigDecimal mdrPercentage;
    private BigDecimal mdrAmount;
    private BigDecimal netAmount;
    private String installmentLabel;     // Ex: "1/12"
    private BigDecimal anticipationFee;  // Custo da antecipação (se houver)
    private String status;
    private String titularidade;
}
