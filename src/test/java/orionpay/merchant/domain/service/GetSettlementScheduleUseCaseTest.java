package orionpay.merchant.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.SettlementScheduleResponse;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.DailyScheduleProjection;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaSettlementEntryRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para GetSettlementScheduleUseCase.
 * Verifica cálculos de agregação e formatação de resposta.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetSettlementScheduleUseCase Tests")
class GetSettlementScheduleUseCaseTest {

    @Mock
    private JpaSettlementEntryRepository repository;

    @InjectMocks
    private GetSettlementScheduleUseCase useCase;

    private UUID merchantId;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        startDate = LocalDate.of(2024, 1, 1);
        endDate = LocalDate.of(2024, 1, 31);
    }

    @Test
    @DisplayName("Should return schedule with aggregated data for the period")
    void testExecuteWithValidDateRange() {
        // Arrange
        DailyScheduleProjection day1 = createMockProjection(
                LocalDate.of(2024, 1, 5),
                new BigDecimal("2500.00"),
                new BigDecimal("2425.00"),
                "SCHEDULED,PENDING",
                12L
        );

        DailyScheduleProjection day2 = createMockProjection(
                LocalDate.of(2024, 1, 10),
                new BigDecimal("5000.00"),
                new BigDecimal("4850.00"),
                "SCHEDULED,SETTLED",
                23L
        );

        when(repository.findDailySchedule(merchantId, startDate, endDate, null))
                .thenReturn(List.of(day1, day2));

        // Act
        SettlementScheduleResponse response = useCase.execute(merchantId, startDate, endDate, null);

        // Assert
        assertNotNull(response);
        assertEquals(startDate, response.getPeriodStart());
        assertEquals(endDate, response.getPeriodEnd());

        // Verificar totalizadores
        assertEquals(new BigDecimal("7500.00"), response.getTotalPeriodGross());
        assertEquals(new BigDecimal("7275.00"), response.getTotalPeriodNet());
        assertEquals(35, response.getTotalTransactionsInPeriod());

        // Verificar agenda detalhada
        assertEquals(2, response.getSchedule().size());

        // Dia 1
        SettlementScheduleResponse.DailySchedule daySchedule1 = response.getSchedule().get(0);
        assertEquals(LocalDate.of(2024, 1, 5), daySchedule1.getDate());
        assertEquals(new BigDecimal("2500.00"), daySchedule1.getTotalGross());
        assertEquals(new BigDecimal("2425.00"), daySchedule1.getTotalNet());
        assertEquals(new BigDecimal("75.00"), daySchedule1.getMdrAmount());
        assertEquals(12, daySchedule1.getTransactionCount());
        assertEquals(2, daySchedule1.getStatusSummary().size());
        assertTrue(daySchedule1.getStatusSummary().contains("SCHEDULED"));
        assertTrue(daySchedule1.getStatusSummary().contains("PENDING"));

        // Dia 2
        SettlementScheduleResponse.DailySchedule daySchedule2 = response.getSchedule().get(1);
        assertEquals(LocalDate.of(2024, 1, 10), daySchedule2.getDate());
        assertEquals(new BigDecimal("5000.00"), daySchedule2.getTotalGross());
        assertEquals(new BigDecimal("4850.00"), daySchedule2.getTotalNet());
        assertEquals(new BigDecimal("150.00"), daySchedule2.getMdrAmount());
        assertEquals(23, daySchedule2.getTransactionCount());
    }

    @Test
    @DisplayName("Should filter by status correctly")
    void testExecuteWithStatusFilter() {
        // Arrange
        DailyScheduleProjection schedProjection = createMockProjection(
                LocalDate.of(2024, 1, 15),
                new BigDecimal("3500.00"),
                new BigDecimal("3395.00"),
                "SCHEDULED",
                18L
        );

        when(repository.findDailySchedule(merchantId, startDate, endDate, "SCHEDULED"))
                .thenReturn(List.of(schedProjection));

        // Act
        SettlementScheduleResponse response = useCase.execute(merchantId, startDate, endDate, "SCHEDULED");

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getSchedule().size());
        assertEquals(new BigDecimal("3500.00"), response.getTotalPeriodGross());
        assertEquals(1, response.getSchedule().get(0).getStatusSummary().size());
        assertTrue(response.getSchedule().get(0).getStatusSummary().contains("SCHEDULED"));
    }

    @Test
    @DisplayName("Should handle empty schedule")
    void testExecuteWithEmptySchedule() {
        // Arrange
        when(repository.findDailySchedule(merchantId, startDate, endDate, null))
                .thenReturn(List.of());

        // Act
        SettlementScheduleResponse response = useCase.execute(merchantId, startDate, endDate, null);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getSchedule().size());
        assertEquals(BigDecimal.ZERO, response.getTotalPeriodGross());
        assertEquals(BigDecimal.ZERO, response.getTotalPeriodNet());
        assertEquals(0, response.getTotalTransactionsInPeriod());
    }

    @Test
    @DisplayName("Should calculate daily average transaction correctly")
    void testDailyAverageTransactionCalculation() {
        // Arrange
        DailyScheduleProjection projection = createMockProjection(
                LocalDate.of(2024, 1, 5),
                new BigDecimal("1000.00"),
                new BigDecimal("970.00"),
                "SCHEDULED",
                4L
        );

        when(repository.findDailySchedule(merchantId, startDate, endDate, null))
                .thenReturn(List.of(projection));

        // Act
        SettlementScheduleResponse response = useCase.execute(merchantId, startDate, endDate, null);

        // Assert
        SettlementScheduleResponse.DailySchedule day = response.getSchedule().get(0);
        assertEquals(new BigDecimal("250.00"), day.getDailyAverageTransaction());
    }

    @Test
    @DisplayName("Should handle null values in projection")
    void testHandleNullValuesInProjection() {
        // Arrange
        DailyScheduleProjection projection = createMockProjection(
                LocalDate.of(2024, 1, 5),
                null,  // null totalGross
                null,  // null totalNet
                null,  // null statuses
                null   // null count
        );

        when(repository.findDailySchedule(merchantId, startDate, endDate, null))
                .thenReturn(List.of(projection));

        // Act
        SettlementScheduleResponse response = useCase.execute(merchantId, startDate, endDate, null);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getSchedule().size());
        assertEquals(BigDecimal.ZERO, response.getTotalPeriodGross());
        assertEquals(BigDecimal.ZERO, response.getTotalPeriodNet());
        assertEquals(0, response.getTotalTransactionsInPeriod());
        assertEquals(0, response.getSchedule().get(0).getTransactionCount());
        assertTrue(response.getSchedule().get(0).getStatusSummary().isEmpty());
    }

    @Test
    @DisplayName("Should parse multiple statuses correctly")
    void testParseMultipleStatuses() {
        // Arrange
        DailyScheduleProjection projection = createMockProjection(
                LocalDate.of(2024, 1, 5),
                new BigDecimal("1000.00"),
                new BigDecimal("970.00"),
                "SCHEDULED, PENDING, SETTLED",  // Com espaços
                3L
        );

        when(repository.findDailySchedule(merchantId, startDate, endDate, null))
                .thenReturn(List.of(projection));

        // Act
        SettlementScheduleResponse response = useCase.execute(merchantId, startDate, endDate, null);

        // Assert
        SettlementScheduleResponse.DailySchedule day = response.getSchedule().get(0);
        assertEquals(3, day.getStatusSummary().size());
        assertTrue(day.getStatusSummary().contains("SCHEDULED"));
        assertTrue(day.getStatusSummary().contains("PENDING"));
        assertTrue(day.getStatusSummary().contains("SETTLED"));
    }

    @Test
    @DisplayName("Should handle zero transaction count")
    void testHandleZeroTransactionCount() {
        // Arrange
        DailyScheduleProjection projection = createMockProjection(
                LocalDate.of(2024, 1, 5),
                new BigDecimal("1000.00"),
                new BigDecimal("970.00"),
                "SCHEDULED",
                0L
        );

        when(repository.findDailySchedule(merchantId, startDate, endDate, null))
                .thenReturn(List.of(projection));

        // Act
        SettlementScheduleResponse response = useCase.execute(merchantId, startDate, endDate, null);

        // Assert
        SettlementScheduleResponse.DailySchedule day = response.getSchedule().get(0);
        assertEquals(0, day.getTransactionCount());
        assertEquals(BigDecimal.ZERO, day.getDailyAverageTransaction());
    }

    // Helper method to create mock projections
    private DailyScheduleProjection createMockProjection(LocalDate date, BigDecimal gross,
                                                          BigDecimal net, String statuses, Long count) {
        return new DailyScheduleProjection() {
            @Override
            public LocalDate getSettlementDate() {
                return date;
            }

            @Override
            public BigDecimal getTotalGross() {
                return gross;
            }

            @Override
            public BigDecimal getTotalNet() {
                return net;
            }

            @Override
            public String getStatuses() {
                return statuses;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }
}

