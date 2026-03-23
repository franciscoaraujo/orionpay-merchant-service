package orionpay.merchant.domain.service.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import orionpay.merchant.domain.event.MerchantCreatedEvent;
import orionpay.merchant.domain.model.LedgerAccount;
import orionpay.merchant.domain.model.MerchantPricing;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.LedgerRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.PricingRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantOnboardingListenerTest {

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private PricingRepository pricingRepository;

    @InjectMocks
    private MerchantOnboardingListener merchantOnboardingListener;

    private MerchantCreatedEvent buildEvent() {
        return new MerchantCreatedEvent(UUID.randomUUID(), "12345678000195", "Loja Teste LTDA");
    }

    @Test
    void provisionLedgerAccount_shouldCreateZeroBalanceAccount_forNewMerchant() {
        MerchantCreatedEvent event = buildEvent();

        merchantOnboardingListener.provisionLedgerAccount(event);

        ArgumentCaptor<LedgerAccount> captor = ArgumentCaptor.forClass(LedgerAccount.class);
        verify(ledgerRepository).saveAccount(captor.capture());

        LedgerAccount savedAccount = captor.getValue();
        assertNotNull(savedAccount);
        assertEquals(event.merchantId(), savedAccount.getMerchantId());
        assertEquals(java.math.BigDecimal.ZERO, savedAccount.getBalance());
        assertTrue(savedAccount.getAccountNumber().startsWith("CC-"));
    }

    @Test
    void provisionDefaultFees_shouldSaveThreeDefaultPricings() {
        MerchantCreatedEvent event = buildEvent();

        merchantOnboardingListener.provisionDefaultFees(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MerchantPricing>> captor = ArgumentCaptor.forClass(List.class);
        verify(pricingRepository).saveAll(captor.capture());

        List<MerchantPricing> savedPricings = captor.getValue();
        assertEquals(3, savedPricings.size());
        assertTrue(savedPricings.stream().allMatch(p -> event.merchantId().equals(p.getMerchantId())));
    }

    @Test
    void provisionDefaultFees_shouldIncludeCreditDebitAndParcelado() {
        MerchantCreatedEvent event = buildEvent();

        merchantOnboardingListener.provisionDefaultFees(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MerchantPricing>> captor = ArgumentCaptor.forClass(List.class);
        verify(pricingRepository).saveAll(captor.capture());

        List<MerchantPricing> pricings = captor.getValue();
        boolean hasCredit = pricings.stream().anyMatch(
                p -> p.getProductType() == orionpay.merchant.domain.model.enums.ProductType.CREDIT_A_VISTA);
        boolean hasDebit = pricings.stream().anyMatch(
                p -> p.getProductType() == orionpay.merchant.domain.model.enums.ProductType.DEBIT);
        boolean hasParcelado = pricings.stream().anyMatch(
                p -> p.getProductType() == orionpay.merchant.domain.model.enums.ProductType.CREDIT_PARCELADO);

        assertTrue(hasCredit);
        assertTrue(hasDebit);
        assertTrue(hasParcelado);
    }
}
