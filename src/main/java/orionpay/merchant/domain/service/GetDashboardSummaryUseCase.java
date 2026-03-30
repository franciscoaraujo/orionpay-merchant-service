package orionpay.merchant.domain.service;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import orionpay.merchant.application.ports.input.rest.dto.DashboardSummaryDto;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.DailyMerchantSummaryEntity;
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
    private final JpaDailySummaryRepository dailySummaryRepository; // Novo Repositório
    private final LedgerRepository ledgerRepository;
    private final TerminalRepository terminalRepository;
    private final DashboardMapper dashboardMapper;


    @Transactional
    @Cacheable(value = "dashboard_summary", key = "#merchantId")
    public DashboardSummaryDto execute(UUID merchantId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDate yesterday = today.minusDays(1);

        // 1. Período Atual (Lê o Read Model - Complexidade O(1))
        DailyMerchantSummaryEntity currentSummary = dailySummaryRepository.findByMerchantIdAndDate(merchantId, today)
                .orElse(createEmptySummary(merchantId, today));

        // 2. Período Anterior (Lê o Read Model - Complexidade O(1))
        DailyMerchantSummaryEntity previousSummary = dailySummaryRepository.findByMerchantIdAndDate(merchantId, yesterday)
                .orElse(createEmptySummary(merchantId, yesterday));

        // 3. Métricas de Tendência e Distribuição (Mantemos pois são dinâmicas por hora)
        var trend = transactionRepository.getHourlyTrend(merchantId, now);
        var brands = transactionRepository.getBrandDistribution(merchantId, now.toLocalDate().atStartOfDay());

        // 4. Saldo e Recebíveis (Schema: accounting)
        var balances = ledgerRepository.getLedgerBalances(merchantId);
        BigDecimal availableBalance = balances != null ? balances.getAvailableBalance() : BigDecimal.ZERO;
        BigDecimal futureReceivables = balances != null ? balances.getFutureReceivables() : BigDecimal.ZERO;

        // 5. Terminais
        Long activeTerminals = terminalRepository.countByMerchantIdAndStatus(merchantId, "ACTIVE");

        // 6. Mapeamento Final (Convertemos Entity para o objeto de projeção que o Mapper espera)
        return dashboardMapper.toDTO(
                mapToProjection(currentSummary),
                mapToProjection(previousSummary),
                availableBalance,
                futureReceivables,
                activeTerminals,
                trend,
                brands
        );
    }

    private DailyMerchantSummaryEntity createEmptySummary(UUID merchantId, LocalDate date) {
        return DailyMerchantSummaryEntity.builder()
                .merchantId(merchantId)
                .date(date)
                .totalTpv(BigDecimal.ZERO)
                .totalNetRevenue(BigDecimal.ZERO)
                .approvedCount(0L)
                .totalCount(0L)
                .build();
    }

    private TransactionSummaryProjection mapToProjection(DailyMerchantSummaryEntity s) {
        return new TransactionSummaryProjection() {
            @Override public BigDecimal getTotalVolume() { return s.getTotalTpv(); }
            @Override public BigDecimal getNetVolume() { return s.getTotalNetRevenue(); }
            @Override public Long getTotalCount() { return s.getTotalCount(); }
            @Override public Long getApprovedCount() { return s.getApprovedCount(); }
        };
    }
}
