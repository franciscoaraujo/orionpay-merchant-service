package orionpay.merchant.application.ports.input.rest.dto;


import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardSummaryDto {

    // --- CARDS PRINCIPAIS (TOP) ---
    private BigDecimal tpv;               // Total Processed Volume (Bruto)
    private BigDecimal netRevenue;        // Receita Líquida (TPV - MDR)
    private Double approvalRate;          // % de Aprovação
    private Long activeTerminals;         // Total de Terminais Ativos
    private BigDecimal availableBalance;  // Saldo na accounting.ledger_account
    private BigDecimal futureReceivables; // Novo campo para Saldo Futuro (Recebíveis)
    private long approvedTransactions;

    // --- GRÁFICO DE TENDÊNCIA (CENTRO) ---
    private List<HourlySalesDTO> salesTrend; // Dados para o gráfico Today vs Yesterday

    // --- DISTRIBUIÇÃO (LATERAL) ---
    private List<BrandDistributionDTO> brandDistribution; // Visa, Master, Elo...

    /**
     * DTO interno para os pontos do gráfico de linha
     */
    @Data
    @Builder
    public static class HourlySalesDTO {
        private int hour;           // 0 a 23
        private BigDecimal today;    // Valor hoje nessa hora
        private BigDecimal yesterday;// Valor ontem nessa hora
    }

    /**
     * DTO interno para o gráfico de barras/progresso lateral
     */
    @Data
    @Builder
    public static class BrandDistributionDTO {
        private String brand;        // Nome da bandeira
        private BigDecimal value;    // Volume total
        private Double percentage;   // % representativa no TPV
    }
}