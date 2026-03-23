package orionpay.merchant.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import orionpay.merchant.application.ports.output.PaymentServicePort;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.LedgerAccount;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.WithdrawRequest;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.PayoutEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaPayoutRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.LedgerRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WithdrawMoneyUseCaseTest {

    @Mock
    private LedgerRepository ledgerRepository;
    @Mock
    private JpaPayoutRepository payoutRepository;
    @Mock
    private PaymentServicePort paymentService;

    @InjectMocks
    private WithdrawMoneyUseCase withdrawMoneyUseCase;

    private final UUID merchantId = UUID.randomUUID();

    private WithdrawRequest buildRequest(BigDecimal amount) {
        return new WithdrawRequest(merchantId, amount, "pix@email.com");
    }

    private LedgerAccount buildAccount(BigDecimal balance) {
        return new LedgerAccount(UUID.randomUUID(), merchantId, "CC-001", balance, 0L);
    }

    @Test
    void execute_shouldCompleteWithdraw_whenSufficientBalance() {
        WithdrawRequest request = buildRequest(new BigDecimal("50.00"));
        LedgerAccount account = buildAccount(new BigDecimal("200.00"));

        when(ledgerRepository.findRealAvailableBalance(merchantId)).thenReturn(new BigDecimal("200.00"));
        when(ledgerRepository.findByMerchantId(merchantId)).thenReturn(Optional.of(account));
        when(payoutRepository.save(any(PayoutEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentService.processPixPayout("pix@email.com", new BigDecimal("50.00"))).thenReturn(true);

        assertDoesNotThrow(() -> withdrawMoneyUseCase.execute(request));

        verify(ledgerRepository).saveAccount(any(LedgerAccount.class));
        verify(ledgerRepository).saveEntry(any(), any(), any(), any(), any(), any());
        verify(payoutRepository, times(2)).save(any(PayoutEntity.class));
    }

    @Test
    void execute_shouldThrow_whenAmountIsZero() {
        WithdrawRequest request = buildRequest(BigDecimal.ZERO);

        assertThrows(DomainException.class, () -> withdrawMoneyUseCase.execute(request));

        verifyNoInteractions(ledgerRepository);
        verifyNoInteractions(paymentService);
    }

    @Test
    void execute_shouldThrow_whenAmountIsNegative() {
        WithdrawRequest request = buildRequest(new BigDecimal("-10.00"));

        assertThrows(DomainException.class, () -> withdrawMoneyUseCase.execute(request));

        verifyNoInteractions(ledgerRepository);
    }

    @Test
    void execute_shouldThrow_whenInsufficientRealAvailableBalance() {
        WithdrawRequest request = buildRequest(new BigDecimal("500.00"));

        when(ledgerRepository.findRealAvailableBalance(merchantId)).thenReturn(new BigDecimal("100.00"));

        assertThrows(DomainException.class, () -> withdrawMoneyUseCase.execute(request));

        verify(ledgerRepository, never()).findByMerchantId(any());
        verifyNoInteractions(paymentService);
    }

    @Test
    void execute_shouldThrow_whenLedgerAccountNotFound() {
        WithdrawRequest request = buildRequest(new BigDecimal("50.00"));

        when(ledgerRepository.findRealAvailableBalance(merchantId)).thenReturn(new BigDecimal("200.00"));
        when(ledgerRepository.findByMerchantId(merchantId)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> withdrawMoneyUseCase.execute(request));
    }

    @Test
    void execute_shouldThrow_whenPaymentServiceFails() {
        WithdrawRequest request = buildRequest(new BigDecimal("50.00"));
        LedgerAccount account = buildAccount(new BigDecimal("200.00"));

        when(ledgerRepository.findRealAvailableBalance(merchantId)).thenReturn(new BigDecimal("200.00"));
        when(ledgerRepository.findByMerchantId(merchantId)).thenReturn(Optional.of(account));
        when(payoutRepository.save(any(PayoutEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentService.processPixPayout("pix@email.com", new BigDecimal("50.00"))).thenReturn(false);

        assertThrows(DomainException.class, () -> withdrawMoneyUseCase.execute(request));
    }
}
