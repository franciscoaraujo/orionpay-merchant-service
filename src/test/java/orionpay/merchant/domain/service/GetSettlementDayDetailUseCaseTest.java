package orionpay.merchant.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.SettlementDayDetailResponse;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.AgendaItemProjection;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaSettlementEntryRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para GetSettlementDayDetailUseCase.
 * Verifica cálculos e agregações de detalhe do dia.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetSettlementDayDetailUseCase Tests")
class GetSettlementDayDetailUseCaseTest {

    @Mock
    private JpaSettlementEntryRepository repository;

    @InjectMocks
    private GetSettlementDayDetailUseCase useCase;

    private UUID merchantId;
    private LocalDate settlementDate;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        settlementDate = LocalDate.of(2024, 1, 15);
        pageable = PageRequest.of(0, 50);
    }

    @Test
    @DisplayName("Should return day detail with aggregated data")
    void testExecuteWithValidDate() {
        // Arrange
        AgendaItemProjection item1 = createMockAgendaItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "123456",
                new BigDecimal("250.00"),
                new BigDecimal("250.00"),
                new BigDecimal("2.99"),
                new BigDecimal("7.50"),
                new BigDecimal("242.50"),
                "SCHEDULED",
                false,
                false,
                1
        );

        AgendaItemProjection item2 = createMockAgendaItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "123457",
                new BigDecimal("500.00"),
                new BigDecimal("1500.00"),
                new BigDecimal("2.99"),
                new BigDecimal("14.95"),
                new BigDecimal("485.05"),
                "ANTICIPATED",
                false,
                true,
                1
        );

        Page<AgendaItemProjection> page = new PageImpl<>(List.of(item1, item2), pageable, 2);

        when(repository.findAgendaByPeriod(
                eq(merchantId),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                isNull(),
                eq(pageable)
        )).thenReturn(page);

        // Act
        SettlementDayDetailResponse response = useCase.execute(merchantId, settlementDate, null, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(settlementDate, response.getSettlementDate());

        // Verificar totalizadores
        assertEquals(new BigDecimal("750.00"), response.getTotalGross());
        assertEquals(new BigDecimal("22.45"), response.getTotalMdr());
        assertEquals(new BigDecimal("727.55"), response.getTotalNet());

        // Verificar contadores
        assertEquals(2, response.getTotalCount());
        assertEquals(0, response.getBlockedCount());
        assertEquals(1, response.getAnticipatedCount());

        // Verificar status breakdown
        assertEquals(2, response.getStatusBreakdown().size());
        assertEquals(1L, response.getStatusBreakdown().get("SCHEDULED"));
        assertEquals(1L, response.getStatusBreakdown().get("ANTICIPATED"));

        // Verificar transações
        assertEquals(2, response.getTransactions().size());
    }

    @Test
    @DisplayName("Should calculate average transaction correctly")
    void testAverageTransactionCalculation() {
        // Arrange
        AgendaItemProjection item1 = createMockAgendaItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "111111",
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                new BigDecimal("3.00"),
                new BigDecimal("3.00"),
                new BigDecimal("97.00"),
                "SCHEDULED",
                false,
                false,
                1
        );

        AgendaItemProjection item2 = createMockAgendaItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "222222",
                new BigDecimal("200.00"),
                new BigDecimal("200.00"),
                new BigDecimal("3.00"),
                new BigDecimal("6.00"),
                new BigDecimal("194.00"),
                "SCHEDULED",
                false,
                false,
                1
        );

        Page<AgendaItemProjection> page = new PageImpl<>(List.of(item1, item2), pageable, 2);

        when(repository.findAgendaByPeriod(
                any(), any(), any(), isNull(), any()
        )).thenReturn(page);

        // Act
        SettlementDayDetailResponse response = useCase.execute(merchantId, settlementDate, null, pageable);

        // Assert
        // (100 + 200) / 2 = 150
        assertEquals(new BigDecimal("150.00"), response.getAverageTransaction());
    }

    @Test
    @DisplayName("Should count blocked and anticipated items correctly")
    void testCountBlockedAndAnticipatedItems() {
        // Arrange
        AgendaItemProjection blocked = createMockAgendaItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "333333",
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                new BigDecimal("3.00"),
                new BigDecimal("3.00"),
                new BigDecimal("97.00"),
                "BLOCKED",
                true,
                false,
                1
        );

        AgendaItemProjection anticipated = createMockAgendaItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "444444",
                new BigDecimal("200.00"),
                new BigDecimal("200.00"),
                new BigDecimal("3.00"),
                new BigDecimal("6.00"),
                new BigDecimal("194.00"),
                "ANTICIPATED",
                false,
                true,
                1
        );

        AgendaItemProjection normal = createMockAgendaItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "555555",
                new BigDecimal("300.00"),
                new BigDecimal("300.00"),
                new BigDecimal("3.00"),
                new BigDecimal("9.00"),
                new BigDecimal("291.00"),
                "SCHEDULED",
                false,
                false,
                1
        );

        Page<AgendaItemProjection> page = new PageImpl<>(List.of(blocked, anticipated, normal), pageable, 3);

        when(repository.findAgendaByPeriod(
                any(), any(), any(), isNull(), any()
        )).thenReturn(page);

        // Act
        SettlementDayDetailResponse response = useCase.execute(merchantId, settlementDate, null, pageable);

        // Assert
        assertEquals(1, response.getBlockedCount());
        assertEquals(1, response.getAnticipatedCount());
        assertEquals(3, response.getTotalCount());
    }

    @Test
    @DisplayName("Should filter by status correctly")
    void testFilterByStatus() {
        // Arrange
        AgendaItemProjection item = createMockAgendaItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "666666",
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("2.99"),
                new BigDecimal("14.95"),
                new BigDecimal("485.05"),
                "SCHEDULED",
                false,
                false,
                1
        );

        Page<AgendaItemProjection> page = new PageImpl<>(List.of(item), pageable, 1);

        when(repository.findAgendaByPeriod(
                eq(merchantId),
                any(),
                any(),
                eq("SCHEDULED"),
                eq(pageable)
        )).thenReturn(page);

        // Act
        SettlementDayDetailResponse response = useCase.execute(merchantId, settlementDate, "SCHEDULED", pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalCount());
        assertEquals(1, response.getStatusBreakdown().get("SCHEDULED"));
    }

    @Test
    @DisplayName("Should throw exception when merchantId is null")
    void testThrowExceptionWhenMerchantIdNull() {
        // Act & Assert
        assertThrows(DomainException.class, () -> {
            useCase.execute(null, settlementDate, null, pageable);
        });
    }

    @Test
    @DisplayName("Should throw exception when settlementDate is null")
    void testThrowExceptionWhenSettlementDateNull() {
        // Act & Assert
        assertThrows(DomainException.class, () -> {
            useCase.execute(merchantId, null, null, pageable);
        });
    }

    @Test
    @DisplayName("Should handle empty result")
    void testHandleEmptyResult() {
        // Arrange
        Page<AgendaItemProjection> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(repository.findAgendaByPeriod(
                any(), any(), any(), isNull(), any()
        )).thenReturn(emptyPage);

        // Act
        SettlementDayDetailResponse response = useCase.execute(merchantId, settlementDate, null, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getTotalCount());
        assertEquals(BigDecimal.ZERO, response.getTotalGross());
        assertEquals(BigDecimal.ZERO, response.getTotalMdr());
        assertEquals(BigDecimal.ZERO, response.getTotalNet());
        assertTrue(response.getTransactions().isEmpty());
    }

    @Test
    @DisplayName("Should handle pagination correctly")
    void testPaginationHandling() {
        // Arrange
        AgendaItemProjection item = createMockAgendaItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "777777",
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                new BigDecimal("3.00"),
                new BigDecimal("3.00"),
                new BigDecimal("97.00"),
                "SCHEDULED",
                false,
                false,
                1
        );

        Pageable page1 = PageRequest.of(0, 10);
        Page<AgendaItemProjection> responsePage = new PageImpl<>(List.of(item), page1, 25);

        when(repository.findAgendaByPeriod(
                any(), any(), any(), isNull(), any()
        )).thenReturn(responsePage);

        // Act
        SettlementDayDetailResponse response = useCase.execute(merchantId, settlementDate, null, page1);

        // Assert
        assertEquals(0, response.getPageNumber());
        assertEquals(10, response.getPageSize());
        assertEquals(3, response.getTotalPages());  // 25 items / 10 per page = 3 pages
        assertEquals(25L, response.getTotalElements());
    }

    // Helper method to create mock agenda items
    private AgendaItemProjection createMockAgendaItem(
            UUID idExt, UUID transactionId, String nsu,
            BigDecimal grossAmount, BigDecimal originalAmount,
            BigDecimal mdrPercentage, BigDecimal mdrAmount, BigDecimal netAmount,
            String status, Boolean blocked, Boolean anticipated, Integer installmentNumber
    ) {
        return new AgendaItemProjection() {
            @Override
            public UUID getIdExt() {
                return idExt;
            }

            @Override
            public UUID getTransactionId() {
                return transactionId;
            }

            @Override
            public LocalDateTime getSettlementDate() {
                return settlementDate.atStartOfDay();
            }

            @Override
            public LocalDateTime getTransactionDate() {
                return settlementDate.minusDays(5).atTime(14, 30);
            }

            @Override
            public BigDecimal getGrossAmount() {
                return grossAmount;
            }

            @Override
            public BigDecimal getMdrAmount() {
                return mdrAmount;
            }

            @Override
            public BigDecimal getNetAmount() {
                return netAmount;
            }

            @Override
            public String getStatus() {
                return status;
            }

            @Override
            public LocalDateTime getPaidAt() {
                return null;
            }

            @Override
            public String getNsu() {
                return nsu;
            }

            @Override
            public String getCardBrand() {
                return "VISA";
            }

            @Override
            public String getCardLastFour() {
                return "1234";
            }

            @Override
            public String getProductType() {
                return "CREDIT_CARD";
            }

            @Override
            public Boolean getBlocked() {
                return blocked;
            }

            @Override
            public Boolean getAnticipated() {
                return anticipated;
            }

            @Override
            public Integer getInstallmentNumber() {
                return installmentNumber;
            }

            @Override
            public BigDecimal getMdrPercentage() {
                return mdrPercentage;
            }

            @Override
            public BigDecimal getOriginalAmount() {
                return originalAmount;
            }
        };
    }
}

