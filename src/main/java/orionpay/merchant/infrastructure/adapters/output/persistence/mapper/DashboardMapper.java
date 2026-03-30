package orionpay.merchant.infrastructure.adapters.output.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import orionpay.merchant.application.ports.input.rest.dto.DashboardSummaryDto;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.BrandDistributionProjection;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.HourlySalesProjection;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.TransactionSummaryProjection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Mapper(componentModel = "spring")
public interface DashboardMapper {

    @Mapping(target = "totalTpv", source = "current.totalVolume")
    @Mapping(target = "totalNetRevenue", source = "current.netVolume")
    @Mapping(target = "countTransactions", source = "current.approvedCount")
    @Mapping(target = "ticketMedia", expression = "java(calculateTicketMedia(current.getTotalVolume(), current.getApprovedCount()))")
    @Mapping(target = "percentualComparison", expression = "java(calculateComparison(current, previous))")
    @Mapping(target = "availableBalance", source = "availableBalance")
    @Mapping(target = "futureReceivables", source = "futureReceivables")
    @Mapping(target = "activeTerminals", source = "activeTerminals")
    @Mapping(target = "salesTrend", source = "trend")
    @Mapping(target = "brandDistribution", source = "brands")
    @Mapping(target = "approvalRate", expression = "java(calculateApprovalRate(current))")
    @Mapping(target = "approvedTransactions", source = "current.approvedCount")
    DashboardSummaryDto toDTO(
            TransactionSummaryProjection current,
            TransactionSummaryProjection previous,
            BigDecimal availableBalance,
            BigDecimal futureReceivables,
            Long activeTerminals,
            List<HourlySalesProjection> trend,
            List<BrandDistributionProjection> brands
    );

    List<DashboardSummaryDto.HourlySalesDTO> toHourlySalesDTOList(List<HourlySalesProjection> projections);

    List<DashboardSummaryDto.BrandDistributionDTO> toBrandDistributionDTOList(List<BrandDistributionProjection> projections);

    default Double calculateApprovalRate(TransactionSummaryProjection s) {
        if (s == null || s.getTotalCount() == 0) return 0.0;
        return (s.getApprovedCount().doubleValue() / s.getTotalCount().doubleValue()) * 100;
    }

    default BigDecimal calculateTicketMedia(BigDecimal totalVolume, Long approvedCount) {
        if (totalVolume == null || approvedCount == null || approvedCount == 0) return BigDecimal.ZERO;
        return totalVolume.divide(BigDecimal.valueOf(approvedCount), 2, RoundingMode.HALF_UP);
    }

    default DashboardSummaryDto.PercentualComparison calculateComparison(TransactionSummaryProjection current, TransactionSummaryProjection previous) {
        if (current == null || previous == null) return null;

        return DashboardSummaryDto.PercentualComparison.builder()
                .tpvComparison(calculatePercentageChange(current.getTotalVolume(), previous.getTotalVolume()))
                .netRevenueComparison(calculatePercentageChange(current.getNetVolume(), previous.getNetVolume()))
                .countTransactionsComparison(calculatePercentageChange(BigDecimal.valueOf(current.getApprovedCount()), BigDecimal.valueOf(previous.getApprovedCount())))
                .ticketMediaComparison(calculatePercentageChange(
                        calculateTicketMedia(current.getTotalVolume(), current.getApprovedCount()),
                        calculateTicketMedia(previous.getTotalVolume(), previous.getApprovedCount())
                ))
                .build();
    }

    default BigDecimal calculatePercentageChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
             return (current == null || current.compareTo(BigDecimal.ZERO) == 0) ? BigDecimal.ZERO : BigDecimal.valueOf(100);
        }
        if (current == null) return BigDecimal.valueOf(-100);
        
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
