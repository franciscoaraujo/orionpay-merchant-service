package orionpay.merchant.domain.model;

import org.junit.jupiter.api.Test;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.enums.EntryType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LedgerAccountTest {

    private LedgerAccount buildAccount(BigDecimal balance) {
        return new LedgerAccount(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "CC-ABCDEF01",
                balance,
                0L
        );
    }

    @Test
    void constructor_shouldInitializeFields() {
        UUID accountId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        LedgerAccount account = new LedgerAccount(accountId, merchantId, "CC-001", new BigDecimal("100.00"), 1L);

        assertEquals(accountId, account.getAccountId());
        assertEquals(merchantId, account.getMerchantId());
        assertEquals("CC-001", account.getAccountNumber());
        assertEquals(new BigDecimal("100.00"), account.getBalance());
        assertEquals(1L, account.getVersion());
    }

    @Test
    void constructor_shouldDefaultBalanceToZero_whenNull() {
        LedgerAccount account = new LedgerAccount(UUID.randomUUID(), UUID.randomUUID(), "CC-001", null, 0L);
        assertEquals(BigDecimal.ZERO, account.getBalance());
    }

    @Test
    void credit_shouldIncreaseBalance() {
        LedgerAccount account = buildAccount(new BigDecimal("100.00"));
        account.credit(new BigDecimal("50.00"));
        assertEquals(new BigDecimal("150.00"), account.getBalance());
    }

    @Test
    void credit_shouldThrow_whenAmountIsZero() {
        LedgerAccount account = buildAccount(new BigDecimal("100.00"));
        assertThrows(DomainException.class, () -> account.credit(BigDecimal.ZERO));
    }

    @Test
    void credit_shouldThrow_whenAmountIsNegative() {
        LedgerAccount account = buildAccount(new BigDecimal("100.00"));
        assertThrows(DomainException.class, () -> account.credit(new BigDecimal("-10.00")));
    }

    @Test
    void debit_shouldDecreaseBalance() {
        LedgerAccount account = buildAccount(new BigDecimal("100.00"));
        account.debit(new BigDecimal("30.00"));
        assertEquals(new BigDecimal("70.00"), account.getBalance());
    }

    @Test
    void debit_shouldThrow_whenAmountIsZero() {
        LedgerAccount account = buildAccount(new BigDecimal("100.00"));
        assertThrows(DomainException.class, () -> account.debit(BigDecimal.ZERO));
    }

    @Test
    void debit_shouldThrow_whenAmountIsNegative() {
        LedgerAccount account = buildAccount(new BigDecimal("100.00"));
        assertThrows(DomainException.class, () -> account.debit(new BigDecimal("-5.00")));
    }

    @Test
    void debit_shouldThrow_whenInsufficientBalance() {
        LedgerAccount account = buildAccount(new BigDecimal("50.00"));
        assertThrows(DomainException.class, () -> account.debit(new BigDecimal("100.00")));
    }

    @Test
    void applyEntry_credit_shouldIncreaseBalance() {
        LedgerAccount account = buildAccount(new BigDecimal("100.00"));
        account.applyEntry(new BigDecimal("50.00"), EntryType.CREDIT);
        assertEquals(new BigDecimal("150.00"), account.getBalance());
    }

    @Test
    void applyEntry_debit_shouldDecreaseBalance() {
        LedgerAccount account = buildAccount(new BigDecimal("100.00"));
        account.applyEntry(new BigDecimal("40.00"), EntryType.DEBIT);
        assertEquals(new BigDecimal("60.00"), account.getBalance());
    }

    @Test
    void applyEntry_shouldThrow_whenAmountIsZero() {
        LedgerAccount account = buildAccount(new BigDecimal("100.00"));
        assertThrows(DomainException.class, () -> account.applyEntry(BigDecimal.ZERO, EntryType.CREDIT));
    }

    @Test
    void applyEntry_debit_shouldThrow_whenWouldResultInNegativeBalance() {
        LedgerAccount account = buildAccount(new BigDecimal("50.00"));
        assertThrows(DomainException.class, () -> account.applyEntry(new BigDecimal("100.00"), EntryType.DEBIT));
    }

    @Test
    void entryType_getMultiplier_shouldReturnCorrectValues() {
        assertEquals(1, EntryType.CREDIT.getMultiplier());
        assertEquals(-1, EntryType.DEBIT.getMultiplier());
    }

    @Test
    void create_factoryMethod_shouldReturnAccountWithNullVersion() {
        UUID accountId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        LedgerAccount account = LedgerAccount.create(accountId, merchantId, "CC-NEW", BigDecimal.ZERO);

        assertNull(account.getVersion());
        assertEquals(BigDecimal.ZERO, account.getBalance());
    }
}
