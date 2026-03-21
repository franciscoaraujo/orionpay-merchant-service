package orionpay.merchant.domain.service;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import orionpay.merchant.application.ports.input.rest.dto.DashboardSummaryDto;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.infrastructure.adapters.output.persistence.mapper.DashboardMapper;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.LedgerRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.PricingRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.TerminalRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetDashboardSummaryUseCase {


    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final TerminalRepository terminalRepository; // core.terminal
    private final PricingRepository pricingRepository;   // ops.merchant_pricing
    private final DashboardMapper dashboardMapper;


    @Transactional
    public DashboardSummaryDto execute(UUID merchantId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();

        // 1. Métricas de Vendas e Gráficos (Schema: core)
        var salesSummary = transactionRepository.getDailySummary(merchantId, startOfDay);
        var trend = transactionRepository.getHourlyTrend(merchantId, now);
        var brands = transactionRepository.getBrandDistribution(merchantId, startOfDay);

        // 2. Saldo e Recebíveis (Schema: accounting) - OTIMIZADO
        var balances = ledgerRepository.getLedgerBalances(merchantId);
        
        BigDecimal availableBalance = balances != null ? balances.getAvailableBalance() : BigDecimal.ZERO;
        BigDecimal futureReceivables = balances != null ? balances.getFutureReceivables() : BigDecimal.ZERO;

        // 3. Terminais (Schema: core)
        Long activeTerminals = terminalRepository.countByMerchantIdAndStatus(merchantId, "ACTIVE");

        // 5. Mapeamento Final
        return dashboardMapper.toDTO(
                salesSummary,
                availableBalance,
                futureReceivables,
                activeTerminals,
                trend,
                brands
        );
    }
}
