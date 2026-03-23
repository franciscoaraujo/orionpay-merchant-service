package orionpay.merchant.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import orionpay.merchant.application.ports.input.rest.dto.MerchantResponse;
import orionpay.merchant.domain.event.MerchantCreatedEvent;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.Merchant;
import orionpay.merchant.domain.model.enums.AccountType;
import orionpay.merchant.domain.model.enums.MerchantStatus;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.MerchantRegistrationRequest;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.MerchantRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterMerchantUseCaseTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RegisterMerchantUseCase registerMerchantUseCase;

    private MerchantRegistrationRequest buildValidRequest() {
        return new MerchantRegistrationRequest(
                "Loja Teste LTDA",
                "12345678000195",
                "loja@test.com",
                "01310100",
                "Rua das Flores",
                "100",
                "Sala 1",
                "Centro",
                "São Paulo",
                "SP",
                "001",
                "0001",
                "123456",
                "7",
                AccountType.CHECKING
        );
    }

    @Test
    void execute_shouldRegisterMerchantAndPublishEvent_whenDocumentIsNew() {
        MerchantRegistrationRequest request = buildValidRequest();
        when(merchantRepository.findByDocument(request.document())).thenReturn(Optional.empty());
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(inv -> inv.getArgument(0));

        MerchantResponse response = registerMerchantUseCase.execute(request);

        assertNotNull(response);
        assertEquals("Loja Teste LTDA", response.name());
        assertEquals(MerchantStatus.PROVISIONAL, response.status());

        verify(merchantRepository).save(any(Merchant.class));

        ArgumentCaptor<MerchantCreatedEvent> eventCaptor = ArgumentCaptor.forClass(MerchantCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals("12345678000195", eventCaptor.getValue().document());
    }

    @Test
    void execute_shouldThrow_whenDocumentAlreadyExists() {
        MerchantRegistrationRequest request = buildValidRequest();
        Merchant existingMerchant = Merchant.create(
                java.util.UUID.randomUUID(), "Old Merchant", request.document(), "old@email.com");
        when(merchantRepository.findByDocument(request.document())).thenReturn(Optional.of(existingMerchant));

        assertThrows(DomainException.class, () -> registerMerchantUseCase.execute(request));

        verify(merchantRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void execute_shouldSetAddressOnMerchant() {
        MerchantRegistrationRequest request = buildValidRequest();
        when(merchantRepository.findByDocument(request.document())).thenReturn(Optional.empty());

        ArgumentCaptor<Merchant> merchantCaptor = ArgumentCaptor.forClass(Merchant.class);
        when(merchantRepository.save(merchantCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        registerMerchantUseCase.execute(request);

        Merchant savedMerchant = merchantCaptor.getValue();
        assertNotNull(savedMerchant.getBusinessAddress());
        assertEquals("Rua das Flores", savedMerchant.getBusinessAddress().street());
        assertEquals("01310100", savedMerchant.getBusinessAddress().zipCode());
    }

    @Test
    void execute_shouldSetBankAccountOnMerchant() {
        MerchantRegistrationRequest request = buildValidRequest();
        when(merchantRepository.findByDocument(request.document())).thenReturn(Optional.empty());

        ArgumentCaptor<Merchant> merchantCaptor = ArgumentCaptor.forClass(Merchant.class);
        when(merchantRepository.save(merchantCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        registerMerchantUseCase.execute(request);

        Merchant savedMerchant = merchantCaptor.getValue();
        assertNotNull(savedMerchant.getBankAccount());
        assertEquals("001", savedMerchant.getBankAccount().getBankCode());
    }
}
