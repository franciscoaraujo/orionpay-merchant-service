package orionpay.merchant.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import orionpay.merchant.domain.model.TransactionEvent;
import orionpay.merchant.domain.model.enums.ProductType;
import orionpay.merchant.domain.model.enums.TransactionStatus;
import orionpay.merchant.domain.service.SettlementCalculator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettlementCalculatorTest {

    private final SettlementCalculator calculator = new SettlementCalculator();

    @Test
    @DisplayName("Deve balancear centavos na última parcela (10.00 em 3x)")
    void shouldBalancePenniesInLastInstallment() {
        TransactionEvent event = createEvent(new BigDecimal("10.00"), 3);
        BigDecimal mdrPercentage = new BigDecimal("0"); // Simplificando para focar no bruto

        List<SettlementCalculator.CalculatedInstallment> result = calculator.calculate(event, mdrPercentage);

        assertEquals(3, result.size());
        assertEquals(new BigDecimal("3.33"), result.get(0).getGrossAmount());
        assertEquals(new BigDecimal("3.33"), result.get(1).getGrossAmount());
        assertEquals(new BigDecimal("3.34"), result.get(2).getGrossAmount()); // Última parcela recebe o centavo de resíduo

        BigDecimal totalSum = result.stream()
                .map(SettlementCalculator.CalculatedInstallment::getGrossAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("10.00"), totalSum);
    }

    @Test
    @DisplayName("Deve validar comportamento com valor mínimo (0.01 em 2x)")
    void shouldHandleMinimumValuesInMultipleInstallments() {
        TransactionEvent event = createEvent(new BigDecimal("0.01"), 2);
        List<SettlementCalculator.CalculatedInstallment> result = calculator.calculate(event, BigDecimal.ZERO);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("0.00"), result.get(0).getGrossAmount());
        assertEquals(new BigDecimal("0.01"), result.get(1).getGrossAmount());

        BigDecimal totalSum = result.stream()
                .map(SettlementCalculator.CalculatedInstallment::getGrossAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("0.01"), totalSum);
    }

    private TransactionEvent createEvent(BigDecimal amount, int installments) {
        return TransactionEvent.builder()
                .id(UUID.randomUUID())
                .transactionId(UUID.randomUUID())
                .merchantId(UUID.randomUUID())
                .amount(amount)
                .installments(installments)
                .productType(ProductType.CREDIT_PARCELADO)
                .status(TransactionStatus.APPROVED)
                .occurredAt(LocalDateTime.now())
                .build();
    }
}
