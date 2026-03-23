package orionpay.merchant.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import orionpay.merchant.domain.excepion.TransactionNotFoundException;
import orionpay.merchant.domain.model.ExtratoTransactionDetail;
import orionpay.merchant.domain.model.Merchant;
import orionpay.merchant.domain.model.Transaction;
import orionpay.merchant.domain.model.TransactionSource;
import orionpay.merchant.domain.model.enums.ProductType;
import orionpay.merchant.infrastructure.adapters.output.persistence.mapper.TransactionMapper;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.TransactionRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetTransactionDetailUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private GetTransactionDetailUseCase getTransactionDetailUseCase;

    private Transaction buildTransaction() {
        Merchant merchant = Merchant.create(UUID.randomUUID(), "Loja", "12345678901", "t@t.com");
        return new Transaction(
                UUID.randomUUID(), merchant, new BigDecimal("100.00"),
                ProductType.CREDIT_A_VISTA, new TransactionSource("TERM-001", "v1.0", "CHIP")
        );
    }

    @Test
    void execute_shouldReturnDetail_whenTransactionExists() {
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        Transaction transaction = buildTransaction();
        ExtratoTransactionDetail detail = ExtratoTransactionDetail.builder()
                .id(transactionId).nsu("NSU001").amount(new BigDecimal("100.00"))
                .createdAt(java.time.LocalDateTime.now()).status("Capturado")
                .brand("VISA").cardBin("411111").lastFour("1234")
                .holderName("John").terminalSerialNumber("TERM-001")
                .entryMode("CHIP").authCode("AUTH1").externalId("optr_NSU001")
                .build();

        when(transactionRepository.findByIdAndMerchantId(transactionId, merchantId))
                .thenReturn(Optional.of(transaction));
        when(transactionMapper.toDetailDomain(transaction)).thenReturn(detail);

        ExtratoTransactionDetail result = getTransactionDetailUseCase.execute(transactionId, merchantId);

        assertNotNull(result);
        assertEquals(detail, result);
        verify(transactionRepository).findByIdAndMerchantId(transactionId, merchantId);
        verify(transactionMapper).toDetailDomain(transaction);
    }

    @Test
    void execute_shouldThrowTransactionNotFoundException_whenNotFound() {
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();

        when(transactionRepository.findByIdAndMerchantId(transactionId, merchantId))
                .thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class,
                () -> getTransactionDetailUseCase.execute(transactionId, merchantId));
    }
}
