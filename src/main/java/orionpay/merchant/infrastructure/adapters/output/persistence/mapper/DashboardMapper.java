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

    @Mapping(target = "tpv", source = "summary.approvedVolume")
    @Mapping(target = "availableBalance", source = "availableBalance")
    @Mapping(target = "futureReceivables", source = "futureReceivables")
    @Mapping(target = "activeTerminals", source = "activeTerminals")
    @Mapping(target = "salesTrend", source = "trend")
    @Mapping(target = "brandDistribution", source = "brands")
    @Mapping(target = "netRevenue", source = "summary.approvedVolume") // Simplificado pois MDR varia por transação
    @Mapping(target = "approvalRate", expression = "java(calculateApprovalRate(summary))")
    @Mapping(target = "approvedTransactions", source = "summary.approvedCount")
    DashboardSummaryDto toDTO(
            TransactionSummaryProjection summary,
            BigDecimal availableBalance,
            BigDecimal futureReceivables,
            Long activeTerminals,
            List<HourlySalesProjection> trend,
            List<BrandDistributionProjection> brands
    );

    // Métodos de conversão para as listas (O MapStruct implementa-os automaticamente)
    List<DashboardSummaryDto.HourlySalesDTO> toHourlySalesDTOList(List<HourlySalesProjection> projections);

    List<DashboardSummaryDto.BrandDistributionDTO> toBrandDistributionDTOList(List<BrandDistributionProjection> projections);

    default Double calculateApprovalRate(TransactionSummaryProjection s) {
        if (s == null || s.getTotalCount() == 0) return 0.0;
        return (s.getApprovedCount().doubleValue() / s.getTotalCount().doubleValue()) * 100;
    }
}