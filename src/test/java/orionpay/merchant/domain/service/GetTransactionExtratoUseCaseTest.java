package orionpay.merchant.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import orionpay.merchant.domain.model.ExtratoTransaction;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetTransactionExtratoUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private GetTransactionExtratoUseCase getTransactionExtratoUseCase;

    @Test
    void execute_shouldReturnPage_whenCalled() {
        UUID merchantId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 15);
        ExtratoTransaction item = ExtratoTransaction.builder()
                .id(UUID.randomUUID())
                .nsu("NSU001")
                .amount(new BigDecimal("100.00"))
                .createdAt(LocalDateTime.now())
                .brand("VISA")
                .lastFour("**** 1234")
                .status("Capturado")
                .externalId("optr_NSU001")
                .build();
        Page<ExtratoTransaction> expectedPage = new PageImpl<>(List.of(item));

        when(transactionRepository.findCustomExtrato(merchantId, null, pageable))
                .thenReturn(expectedPage);

        Page<ExtratoTransaction> result = getTransactionExtratoUseCase.execute(merchantId, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("NSU001", result.getContent().get(0).getNsu());
        verify(transactionRepository).findCustomExtrato(merchantId, null, pageable);
    }

    @Test
    void execute_shouldPassSearchTermToRepository() {
        UUID merchantId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 15);
        Page<ExtratoTransaction> emptyPage = Page.empty();

        when(transactionRepository.findCustomExtrato(merchantId, "visa", pageable))
                .thenReturn(emptyPage);

        Page<ExtratoTransaction> result = getTransactionExtratoUseCase.execute(merchantId, "visa", pageable);

        assertTrue(result.isEmpty());
        verify(transactionRepository).findCustomExtrato(merchantId, "visa", pageable);
    }
}
