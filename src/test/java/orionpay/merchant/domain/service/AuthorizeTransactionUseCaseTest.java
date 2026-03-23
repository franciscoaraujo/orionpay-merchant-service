package orionpay.merchant.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import orionpay.merchant.application.ports.output.GatewayAuthorizationResult;
import orionpay.merchant.application.ports.output.PaymentGatewayPort;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.LedgerAccount;
import orionpay.merchant.domain.model.Merchant;
import orionpay.merchant.domain.model.MerchantPricing;
import orionpay.merchant.domain.model.Transaction;
import orionpay.merchant.domain.model.enums.AccountType;
import orionpay.merchant.domain.model.enums.ProductType;
import orionpay.merchant.domain.model.enums.TransactionStatus;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionRequest;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionResponse;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.LedgerRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.MerchantRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.PricingRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizeTransactionUseCaseTest {

    @Mock
    private MerchantRepository merchantRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private LedgerRepository ledgerRepository;
    @Mock
    private PaymentGatewayPort paymentGateway;
    @Mock
    private PricingRepository pricingRepository;

    @InjectMocks
    private AuthorizeTransactionUseCase authorizeTransactionUseCase;

    private final UUID merchantId = UUID.randomUUID();

    private Merchant buildMerchant() {
        return Merchant.create(merchantId, "Loja Teste", "12345678901", "test@test.com");
    }

    private TransactionRequest buildRequest() {
        return new TransactionRequest(
                merchantId,
                new BigDecimal("100.00"),
                ProductType.CREDIT_A_VISTA,
                "TERM-00001",
                null,
                "CHIP",
                "VISA",
                "John Doe",
                "4111111111111111",
                "12/28",
                "123"
        );
    }

    private MerchantPricing buildPricing() {
        return new MerchantPricing(merchantId, "VISA", ProductType.CREDIT_A_VISTA,
                new BigDecimal("3.50"), LocalDate.now());
    }

    private LedgerAccount buildLedgerAccount() {
        return new LedgerAccount(UUID.randomUUID(), merchantId, "CC-ABCDEF01",
                new BigDecimal("0.00"), 0L);
    }

    @Test
    void execute_shouldReturnApprovedResponse_whenAllConditionsAreMet() {
        TransactionRequest request = buildRequest();
        Merchant merchant = buildMerchant();
        MerchantPricing pricing = buildPricing();
        LedgerAccount account = buildLedgerAccount();

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(pricingRepository.findCurrentPricing(merchantId, ProductType.CREDIT_A_VISTA))
                .thenReturn(Optional.of(pricing));
        when(paymentGateway.authorize(any(Transaction.class), any(TransactionRequest.class)))
                .thenReturn(GatewayAuthorizationResult.success("NSU12345", "AUTH789"));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerRepository.findByMerchantId(merchantId)).thenReturn(Optional.of(account));

        TransactionResponse response = authorizeTransactionUseCase.execute(request);

        assertNotNull(response);
        assertEquals(TransactionStatus.APPROVED, response.status());
        assertEquals("NSU12345", response.nsu());
        assertEquals("AUTH789", response.authorizationCode());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(ledgerRepository).saveAccount(any(LedgerAccount.class));
        verify(ledgerRepository).saveEntry(any(), any(), any(), any(), any(), any());
    }

    @Test
    void execute_shouldThrow_whenMerchantNotFound() {
        TransactionRequest request = buildRequest();
        when(merchantRepository.findById(merchantId)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> authorizeTransactionUseCase.execute(request));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrow_whenPricingNotFound() {
        TransactionRequest request = buildRequest();
        Merchant merchant = buildMerchant();

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(pricingRepository.findCurrentPricing(merchantId, ProductType.CREDIT_A_VISTA))
                .thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> authorizeTransactionUseCase.execute(request));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void execute_shouldSaveDeclinedTransaction_whenGatewayDenies() {
        TransactionRequest request = buildRequest();
        Merchant merchant = buildMerchant();
        MerchantPricing pricing = buildPricing();

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(pricingRepository.findCurrentPricing(merchantId, ProductType.CREDIT_A_VISTA))
                .thenReturn(Optional.of(pricing));
        when(paymentGateway.authorize(any(Transaction.class), any(TransactionRequest.class)))
                .thenReturn(GatewayAuthorizationResult.declined("51 - Saldo insuficiente"));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(DomainException.class, () -> authorizeTransactionUseCase.execute(request));

        // The declined transaction should still be saved
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(ledgerRepository, never()).saveEntry(any(), any(), any(), any(), any(), any());
    }

    @Test
    void execute_shouldThrow_whenLedgerAccountNotFound() {
        TransactionRequest request = buildRequest();
        Merchant merchant = buildMerchant();
        MerchantPricing pricing = buildPricing();

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(pricingRepository.findCurrentPricing(merchantId, ProductType.CREDIT_A_VISTA))
                .thenReturn(Optional.of(pricing));
        when(paymentGateway.authorize(any(Transaction.class), any(TransactionRequest.class)))
                .thenReturn(GatewayAuthorizationResult.success("NSU1", "AUTH1"));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerRepository.findByMerchantId(merchantId)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> authorizeTransactionUseCase.execute(request));
    }
}
