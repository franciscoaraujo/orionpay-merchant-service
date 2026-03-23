package orionpay.merchant.domain.model;

import org.junit.jupiter.api.Test;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.enums.AccountType;
import orionpay.merchant.domain.model.enums.MerchantStatus;
import orionpay.merchant.domain.model.enums.TerminalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MerchantTest {

    private Merchant buildActiveMerchant() {
        Merchant merchant = Merchant.create(UUID.randomUUID(), "Loja Teste", "12345678901", "test@test.com");
        merchant.changeAddress(new Address("Rua A", "1", null, "Centro", "SP", "SP", "01310100"));
        merchant.updateBankAccount(new BankAccount("001", "0001", "12345", "6", AccountType.CHECKING));
        merchant.getPricings().add(new MerchantPricing(
                merchant.getId(), "VISA", orionpay.merchant.domain.model.enums.ProductType.CREDIT_A_VISTA,
                new BigDecimal("3.50"), LocalDate.now()));
        return merchant;
    }

    @Test
    void create_shouldSetProvisionalStatusAndCaptureFields() {
        UUID id = UUID.randomUUID();
        Merchant merchant = Merchant.create(id, "Minha Loja", "12345678901", "loja@email.com");

        assertEquals(id, merchant.getId());
        assertEquals("Minha Loja", merchant.getLegalName());
        assertEquals("12345678901", merchant.getDocument());
        assertEquals("loja@email.com", merchant.getEmail());
        assertEquals(MerchantStatus.PROVISIONAL, merchant.getStatus());
        assertNotNull(merchant.getCreatedAt());
    }

    @Test
    void create_shouldThrow_whenDocumentIsInvalid() {
        assertThrows(DomainException.class,
                () -> Merchant.create(UUID.randomUUID(), "Loja", "123", "e@e.com"));
    }

    @Test
    void create_shouldThrow_whenDocumentIsNull() {
        assertThrows(DomainException.class,
                () -> Merchant.create(UUID.randomUUID(), "Loja", null, "e@e.com"));
    }

    @Test
    void create_shouldThrow_whenEmailIsBlank() {
        assertThrows(DomainException.class,
                () -> Merchant.create(UUID.randomUUID(), "Loja", "12345678901", ""));
    }

    @Test
    void create_shouldThrow_whenEmailIsNull() {
        assertThrows(DomainException.class,
                () -> Merchant.create(UUID.randomUUID(), "Loja", "12345678901", null));
    }

    @Test
    void create_shouldAcceptCpf_elevenDigits() {
        assertDoesNotThrow(() -> Merchant.create(UUID.randomUUID(), "Loja", "12345678901", "e@e.com"));
    }

    @Test
    void create_shouldAcceptCnpj_fourteenDigits() {
        assertDoesNotThrow(() -> Merchant.create(UUID.randomUUID(), "Empresa", "12345678000195", "e@e.com"));
    }

    @Test
    void activate_shouldSetActiveStatus() {
        Merchant merchant = buildActiveMerchant();
        merchant.activate();
        assertEquals(MerchantStatus.ACTIVE, merchant.getStatus());
    }

    @Test
    void activate_shouldThrow_whenNoBankAccount() {
        Merchant merchant = Merchant.create(UUID.randomUUID(), "Loja", "12345678901", "e@e.com");
        merchant.getPricings().add(new MerchantPricing(
                merchant.getId(), "VISA", orionpay.merchant.domain.model.enums.ProductType.CREDIT_A_VISTA,
                new BigDecimal("3.50"), LocalDate.now()));

        assertThrows(DomainException.class, merchant::activate);
    }

    @Test
    void activate_shouldThrow_whenNoPricings() {
        Merchant merchant = Merchant.create(UUID.randomUUID(), "Loja", "12345678901", "e@e.com");
        merchant.updateBankAccount(new BankAccount("001", "0001", "12345", "6", AccountType.CHECKING));

        assertThrows(DomainException.class, merchant::activate);
    }

    @Test
    void suspend_shouldSetSuspendedStatus() {
        Merchant merchant = buildActiveMerchant();
        merchant.activate();
        merchant.suspend("Fraude detectada");
        assertEquals(MerchantStatus.SUSPENDED, merchant.getStatus());
    }

    @Test
    void suspend_shouldThrow_whenAlreadyTerminated() {
        Merchant merchant = new Merchant(
                UUID.randomUUID(), "12345678901", "Loja", "e@e.com",
                MerchantStatus.TERMINATED,
                new BankAccount("001", "0001", "123", "1", AccountType.CHECKING),
                new ArrayList<>(), new ArrayList<>(),
                new Address("Rua", "1", null, "Bairro", "Cidade", "SP", "01310100"),
                LocalDateTime.now());
        assertThrows(DomainException.class, () -> merchant.suspend("reason"));
    }

    @Test
    void updateBankAccount_shouldThrow_whenNull() {
        Merchant merchant = Merchant.create(UUID.randomUUID(), "Loja", "12345678901", "e@e.com");
        assertThrows(DomainException.class, () -> merchant.updateBankAccount(null));
    }

    @Test
    void addTerminal_shouldThrow_whenLimitReached() {
        Merchant merchant = buildActiveMerchant();
        merchant.activate();

        for (int i = 0; i < 10; i++) {
            Terminal t = new Terminal(UUID.randomUUID(), "SN-" + i, "Model", merchant.getId());
            t.activate();
            merchant.addTerminal(t);
        }
        Terminal extra = new Terminal(UUID.randomUUID(), "SN-EXTRA", "Model", merchant.getId());
        extra.activate();

        assertThrows(DomainException.class, () -> merchant.addTerminal(extra));
    }

    @Test
    void addTerminal_shouldSucceed_whenUnderLimit() {
        Merchant merchant = buildActiveMerchant();
        Terminal terminal = new Terminal(UUID.randomUUID(), "SN-001", "Model", merchant.getId());
        assertDoesNotThrow(() -> merchant.addTerminal(terminal));
        assertEquals(1, merchant.getTerminals().size());
    }

    @Test
    void changeAddress_shouldUpdateAddress_andResetToProvisional_whenActive() {
        Merchant merchant = buildActiveMerchant();
        merchant.activate();
        assertEquals(MerchantStatus.ACTIVE, merchant.getStatus());

        Address newAddress = new Address("Rua Nova", "2", null, "Vila", "Rio", "RJ", "20040020");
        merchant.changeAddress(newAddress);

        assertEquals(MerchantStatus.PROVISIONAL, merchant.getStatus());
        assertEquals("Rua Nova", merchant.getBusinessAddress().street());
    }

    @Test
    void merchantStatus_canReceivePayments_shouldReturnTrueForActiveAndAwaitingActivation() {
        assertTrue(MerchantStatus.ACTIVE.canReceivePayments());
        assertTrue(MerchantStatus.AWAITING_ACTIVATION.canReceivePayments());
        assertFalse(MerchantStatus.PROVISIONAL.canReceivePayments());
        assertFalse(MerchantStatus.SUSPENDED.canReceivePayments());
        assertFalse(MerchantStatus.TERMINATED.canReceivePayments());
    }

    @Test
    void merchantStatus_canTransact_shouldReturnTrueOnlyForActive() {
        assertTrue(MerchantStatus.ACTIVE.canTransact());
        assertFalse(MerchantStatus.PROVISIONAL.canTransact());
        assertFalse(MerchantStatus.SUSPENDED.canTransact());
    }
}
