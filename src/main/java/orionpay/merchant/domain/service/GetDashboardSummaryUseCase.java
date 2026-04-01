package orionpay.merchant.domain.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import orionpay.merchant.application.ports.input.rest.dto.DashboardSummaryDto;
import orionpay.merchant.domain.model.enums.DashboardPeriod;
import orionpay.merchant.infrastructure.adapters.output.persistence.mapper.DashboardMapper;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.TransactionSummaryProjection;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaDailySummaryRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.LedgerRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.TerminalRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetDashboardSummaryUseCase {

    private final TransactionRepository transactionRepository;
    private final JpaDailySummaryRepository dailySummaryRepository;
    private final LedgerRepository ledgerRepository;
    private final TerminalRepository terminalRepository;
    private final DashboardMapper dashboardMapper;

    @Transactional
    // REMOVIDO @Cacheable p/ garantir visualização real dos dados de teste
    public DashboardSummaryDto execute(UUID merchantId, String periodStr) {
        LocalDate today = LocalDate.now();
        
        DashboardPeriod period = DashboardPeriod.fromString(periodStr);
        DashboardPeriod.PeriodRange ranges = period.getRanges(today);

        TransactionSummaryProjection currentSummary = dailySummaryRepository.findConsolidatedSummaryByPeriod(
                merchantId, ranges.startCurrent(), ranges.endCurrent());
        
        TransactionSummaryProjection previousSummary = dailySummaryRepository.findConsolidatedSummaryByPeriod(
                merchantId, ranges.startPrev(), ranges.endPrev());

        var trend = transactionRepository.getHourlyTrend(merchantId, LocalDateTime.now());
        var brands = transactionRepository.getBrandDistribution(merchantId, ranges.startCurrent().atStartOfDay());

        var balances = ledgerRepository.getLedgerBalances(merchantId);
        BigDecimal availableBalance = balances != null ? balances.getAvailableBalance() : BigDecimal.ZERO;
        BigDecimal futureReceivables = balances != null ? balances.getFutureReceivables() : BigDecimal.ZERO;

        Long activeTerminals = terminalRepository.countByMerchantIdAndStatus(merchantId, "ACTIVE");

        return dashboardMapper.toDTO(
                currentSummary,
                previousSummary,
                availableBalance,
                futureReceivables,
                activeTerminals,
                trend,
                brands
        );
    }
}
