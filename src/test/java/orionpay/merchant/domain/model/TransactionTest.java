package orionpay.merchant.domain.model;

import org.junit.jupiter.api.Test;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.enums.ProductType;
import orionpay.merchant.domain.model.enums.TransactionStatus;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    private Merchant buildMerchant() {
        return Merchant.create(UUID.randomUUID(), "Loja Teste", "12345678901", "test@test.com");
    }

    private Transaction buildPendingTransaction() {
        return new Transaction(
                UUID.randomUUID(),
                buildMerchant(),
                new BigDecimal("100.00"),
                ProductType.CREDIT_A_VISTA,
                new TransactionSource("TERM-001", "v1.0", "CHIP")
        );
    }

    @Test
    void constructor_shouldInitializeAsPending() {
        Transaction transaction = buildPendingTransaction();

        assertEquals(TransactionStatus.PENDING, transaction.getStatus());
        assertEquals("BRL", transaction.getCurrency());
        assertNotNull(transaction.getCreatedAt());
        assertNotNull(transaction.getId());
    }

    @Test
    void setCardInfo_shouldStoreCardDetails() {
        Transaction transaction = buildPendingTransaction();
        transaction.setCardInfo("VISA", "411111", "1234", "John Doe");

        assertEquals("VISA", transaction.getCardBrand());
        assertEquals("411111", transaction.getCardBin());
        assertEquals("1234", transaction.getCardLastFour());
        assertEquals("John Doe", transaction.getCardHolderName());
    }

    @Test
    void processApproval_shouldTransitionToApproved() {
        Transaction transaction = buildPendingTransaction();
        transaction.processApproval("NSU123456", "AUTH789");

        assertEquals(TransactionStatus.APPROVED, transaction.getStatus());
        assertEquals("NSU123456", transaction.getNsu());
        assertEquals("AUTH789", transaction.getAuthorizationCode());
    }

    @Test
    void processApproval_shouldThrow_whenNotPending() {
        Transaction transaction = buildPendingTransaction();
        transaction.processApproval("NSU1", "AUTH1");
        // Already APPROVED, try again
        assertThrows(DomainException.class, () -> transaction.processApproval("NSU2", "AUTH2"));
    }

    @Test
    void processApproval_shouldThrow_whenNsuIsBlank() {
        Transaction transaction = buildPendingTransaction();
        assertThrows(DomainException.class, () -> transaction.processApproval("", "AUTH789"));
    }

    @Test
    void processApproval_shouldThrow_whenNsuIsNull() {
        Transaction transaction = buildPendingTransaction();
        assertThrows(DomainException.class, () -> transaction.processApproval(null, "AUTH789"));
    }

    @Test
    void processApproval_shouldThrow_whenAuthCodeIsBlank() {
        Transaction transaction = buildPendingTransaction();
        assertThrows(DomainException.class, () -> transaction.processApproval("NSU123", ""));
    }

    @Test
    void decline_shouldTransitionToDeclined() {
        Transaction transaction = buildPendingTransaction();
        transaction.decline("51 - Saldo insuficiente");

        assertEquals(TransactionStatus.DECLINED, transaction.getStatus());
        assertEquals("51 - Saldo insuficiente", transaction.getRefusalReason());
    }

    @Test
    void decline_shouldThrow_whenNotPending() {
        Transaction transaction = buildPendingTransaction();
        transaction.decline("reason");
        // Already DECLINED, try again
        assertThrows(DomainException.class, () -> transaction.decline("another reason"));
    }

    @Test
    void reverse_shouldTransitionToReversed() {
        Transaction transaction = buildPendingTransaction();
        transaction.processApproval("NSU1", "AUTH1");
        transaction.reverse();

        assertEquals(TransactionStatus.REVERSED, transaction.getStatus());
    }

    @Test
    void reverse_shouldThrow_whenNotApproved() {
        Transaction transaction = buildPendingTransaction();
        assertThrows(DomainException.class, transaction::reverse);
    }

    @Test
    void calculateNetValue_shouldDeductMdrFee() {
        Transaction transaction = buildPendingTransaction();
        // 100.00 - (100 * 3.50 / 100) = 100 - 3.50 = 96.50
        transaction.calculateNetValue(new BigDecimal("3.50"));

        assertEquals(new BigDecimal("96.50"), transaction.getNetAmount());
    }

    @Test
    void calculateNetValue_shouldHandleZeroMdr() {
        Transaction transaction = buildPendingTransaction();
        transaction.calculateNetValue(BigDecimal.ZERO);

        assertEquals(new BigDecimal("100.00"), transaction.getNetAmount());
    }

    @Test
    void transactionStatus_isEligibleForSettlement_shouldReturnTrueOnlyForApproved() {
        assertTrue(TransactionStatus.APPROVED.isEligibleForSettlement());
        assertFalse(TransactionStatus.PENDING.isEligibleForSettlement());
        assertFalse(TransactionStatus.DECLINED.isEligibleForSettlement());
        assertFalse(TransactionStatus.REVERSED.isEligibleForSettlement());
    }

    @Test
    void transactionStatus_canBeReversed_shouldReturnTrueOnlyForApproved() {
        assertTrue(TransactionStatus.APPROVED.canBeReversed());
        assertFalse(TransactionStatus.PENDING.canBeReversed());
        assertFalse(TransactionStatus.DECLINED.canBeReversed());
    }

    @Test
    void productType_getSettlementDays_shouldReturnCorrectDays() {
        assertEquals(1, ProductType.DEBIT.getSettlementDays());
        assertEquals(30, ProductType.CREDIT_A_VISTA.getSettlementDays());
        assertEquals(30, ProductType.CREDIT_PARCELADO.getSettlementDays());
    }

    @Test
    void productType_isEligibleForAnticipation_onlyCredit() {
        assertFalse(ProductType.DEBIT.isEligibleForAnticipation());
        assertTrue(ProductType.CREDIT_A_VISTA.isEligibleForAnticipation());
        assertTrue(ProductType.CREDIT_PARCELADO.isEligibleForAnticipation());
    }
}
