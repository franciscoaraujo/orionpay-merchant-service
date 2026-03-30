package orionpay.merchant.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_merchant_summary", schema = "ops", 
       uniqueConstraints = {@UniqueConstraint(columnNames = {"merchant_id", "summary_date"})})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyMerchantSummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "summary_date", nullable = false)
    private LocalDate date;

    @Column(name = "total_tpv", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalTpv;

    @Column(name = "total_net_revenue", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalNetRevenue;

    @Column(name = "approved_count", nullable = false)
    private Long approvedCount;

    @Column(name = "total_count", nullable = false)
    private Long totalCount;
}
