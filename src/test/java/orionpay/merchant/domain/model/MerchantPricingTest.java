package orionpay.merchant.domain.model;

import org.junit.jupiter.api.Test;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.enums.ProductType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MerchantPricingTest {

    private MerchantPricing buildPricing(ProductType type, BigDecimal mdr) {
        return new MerchantPricing(UUID.randomUUID(), "VISA", type, mdr, LocalDate.now());
    }

    @Test
    void constructor_shouldCaptureAllFields() {
        UUID merchantId = UUID.randomUUID();
        LocalDate effective = LocalDate.of(2024, 1, 1);
        MerchantPricing pricing = new MerchantPricing(merchantId, "VISA", ProductType.CREDIT_A_VISTA,
                new BigDecimal("3.50"), effective);

        assertEquals(merchantId, pricing.getMerchantId());
        assertEquals("VISA", pricing.getBrand());
        assertEquals(ProductType.CREDIT_A_VISTA, pricing.getProductType());
        assertEquals(new BigDecimal("3.50"), pricing.getMdrPercentage());
        assertEquals(effective, pricing.getEffectiveDate());
    }

    @Test
    void updateMdr_shouldUpdatePercentage() {
        MerchantPricing pricing = buildPricing(ProductType.CREDIT_A_VISTA, new BigDecimal("3.50"));
        pricing.updateMdr(new BigDecimal("5.00"));
        assertEquals(new BigDecimal("5.00"), pricing.getMdrPercentage());
    }

    @Test
    void updateMdr_shouldThrow_whenNegative() {
        MerchantPricing pricing = buildPricing(ProductType.CREDIT_A_VISTA, new BigDecimal("3.50"));
        assertThrows(DomainException.class, () -> pricing.updateMdr(new BigDecimal("-1.00")));
    }

    @Test
    void updateMdr_shouldThrow_whenAbove30Percent() {
        MerchantPricing pricing = buildPricing(ProductType.CREDIT_A_VISTA, new BigDecimal("3.50"));
        assertThrows(DomainException.class, () -> pricing.updateMdr(new BigDecimal("30.01")));
    }

    @Test
    void updateMdr_shouldAccept_boundaryValues() {
        MerchantPricing pricing = buildPricing(ProductType.CREDIT_A_VISTA, new BigDecimal("3.50"));
        assertDoesNotThrow(() -> pricing.updateMdr(BigDecimal.ZERO));
        assertDoesNotThrow(() -> pricing.updateMdr(new BigDecimal("30.00")));
    }

    @Test
    void validateMdrLimit_shouldPass_whenDebitRateIsWithinLimit() {
        MerchantPricing pricing = buildPricing(ProductType.DEBIT, new BigDecimal("1.50"));
        assertDoesNotThrow(pricing::validateMdrLimit);
    }

    @Test
    void validateMdrLimit_shouldThrow_whenDebitRateExceedsLimit() {
        MerchantPricing pricing = buildPricing(ProductType.DEBIT, new BigDecimal("3.01"));
        assertThrows(DomainException.class, pricing::validateMdrLimit);
    }

    @Test
    void validateMdrLimit_shouldNotThrowForCreditProduct_evenHighRate() {
        MerchantPricing pricing = buildPricing(ProductType.CREDIT_A_VISTA, new BigDecimal("10.00"));
        assertDoesNotThrow(pricing::validateMdrLimit);
    }

    @Test
    void isApplicable_shouldReturnTrue_whenDateIsOnOrAfterEffectiveDate() {
        MerchantPricing pricing = new MerchantPricing(
                UUID.randomUUID(), "VISA", ProductType.CREDIT_A_VISTA,
                new BigDecimal("3.50"), LocalDate.of(2024, 1, 1));

        assertTrue(pricing.isApplicable(LocalDate.of(2024, 1, 1)));
        assertTrue(pricing.isApplicable(LocalDate.of(2025, 6, 15)));
    }

    @Test
    void isApplicable_shouldReturnFalse_whenDateIsBeforeEffectiveDate() {
        MerchantPricing pricing = new MerchantPricing(
                UUID.randomUUID(), "VISA", ProductType.CREDIT_A_VISTA,
                new BigDecimal("3.50"), LocalDate.of(2025, 1, 1));

        assertFalse(pricing.isApplicable(LocalDate.of(2024, 12, 31)));
    }

    @Test
    void create_factoryMethod_shouldReturnPricingWithNullFields() {
        MerchantPricing pricing = MerchantPricing.create("MASTER", ProductType.DEBIT);
        assertEquals("MASTER", pricing.getBrand());
        assertEquals(ProductType.DEBIT, pricing.getProductType());
        assertNull(pricing.getMerchantId());
        assertNull(pricing.getMdrPercentage());
        assertNull(pricing.getEffectiveDate());
    }
}
