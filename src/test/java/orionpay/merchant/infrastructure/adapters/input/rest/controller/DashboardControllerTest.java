package orionpay.merchant.infrastructure.adapters.input.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import orionpay.merchant.application.ports.input.rest.dto.DashboardSummaryDto;
import orionpay.merchant.domain.excepion.GlobalExceptionHandler;
import orionpay.merchant.domain.service.GetDashboardSummaryUseCase;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private GetDashboardSummaryUseCase dashboardUseCase;

    @InjectMocks
    private DashboardController dashboardController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getSummary_shouldReturn200_withSummaryData() throws Exception {
        UUID merchantId = UUID.randomUUID();
        DashboardSummaryDto dto = DashboardSummaryDto.builder()
                .tpv(new BigDecimal("1000.00"))
                .netRevenue(new BigDecimal("965.00"))
                .approvalRate(95.0)
                .activeTerminals(2L)
                .availableBalance(new BigDecimal("500.00"))
                .futureReceivables(new BigDecimal("465.00"))
                .approvedTransactions(10L)
                .salesTrend(Collections.emptyList())
                .brandDistribution(Collections.emptyList())
                .build();

        when(dashboardUseCase.execute(merchantId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/dashboard/{merchantId}/summary", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tpv").value(1000.00))
                .andExpect(jsonPath("$.netRevenue").value(965.00))
                .andExpect(jsonPath("$.activeTerminals").value(2))
                .andExpect(jsonPath("$.availableBalance").value(500.00));
    }

    @Test
    void getSummary_shouldReturn200_withEmptyData() throws Exception {
        UUID merchantId = UUID.randomUUID();
        DashboardSummaryDto emptyDto = DashboardSummaryDto.builder()
                .tpv(BigDecimal.ZERO)
                .netRevenue(BigDecimal.ZERO)
                .approvalRate(0.0)
                .activeTerminals(0L)
                .availableBalance(BigDecimal.ZERO)
                .futureReceivables(BigDecimal.ZERO)
                .approvedTransactions(0L)
                .salesTrend(Collections.emptyList())
                .brandDistribution(Collections.emptyList())
                .build();

        when(dashboardUseCase.execute(merchantId)).thenReturn(emptyDto);

        mockMvc.perform(get("/api/v1/dashboard/{merchantId}/summary", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tpv").value(0));
    }
}
