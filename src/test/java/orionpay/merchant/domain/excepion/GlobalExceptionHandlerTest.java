package orionpay.merchant.domain.excepion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import orionpay.merchant.domain.service.RegisterMerchantUseCase;
import orionpay.merchant.infrastructure.adapters.input.rest.controller.MerchantController;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.MerchantRegistrationRequest;
import orionpay.merchant.domain.model.enums.AccountType;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private RegisterMerchantUseCase registerMerchantUseCase;

    @InjectMocks
    private MerchantController merchantController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(merchantController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private MerchantRegistrationRequest buildValidRequest() {
        return new MerchantRegistrationRequest(
                "Loja Teste LTDA", "12345678000195", "loja@test.com", "01310100",
                "Rua das Flores", "100", null, "Centro", "São Paulo", "SP",
                "001", "0001", "123456", "7", AccountType.CHECKING
        );
    }

    @Test
    void handleDomainException_shouldReturn400_withErrorBody() throws Exception {
        when(registerMerchantUseCase.execute(any()))
                .thenThrow(new DomainException("Lojista já cadastrado.", "MERCHANT_ALREADY_EXISTS"));

        mockMvc.perform(post("/api/v1/merchants/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Lojista já cadastrado."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void handleTransactionNotFoundException_shouldReturn404_withErrorBody() throws Exception {
        UUID id = UUID.randomUUID();
        when(registerMerchantUseCase.execute(any()))
                .thenThrow(new TransactionNotFoundException(id));

        mockMvc.perform(post("/api/v1/merchants/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Transação não encontrada com o ID: " + id));
    }

    @Test
    void domainException_shouldCarryCorrectCode() {
        DomainException ex = new DomainException("Mensagem de erro", "CUSTOM_CODE");
        assertEquals("CUSTOM_CODE", ex.getCode());
        assertEquals("Mensagem de erro", ex.getMessage());
    }

    @Test
    void domainException_shouldUseDefaultCode_whenNoCodeProvided() {
        DomainException ex = new DomainException("Violação de regra");
        assertEquals("BUSINESS_RULE_VIOLATION", ex.getCode());
    }

    @Test
    void transactionNotFoundException_shouldFormatMessageWithId() {
        UUID id = UUID.randomUUID();
        TransactionNotFoundException ex = new TransactionNotFoundException(id);
        assertTrue(ex.getMessage().contains(id.toString()));
    }

    @Test
    void transactionNotFoundException_shouldAcceptCustomMessage() {
        TransactionNotFoundException ex = new TransactionNotFoundException("Custom error");
        assertEquals("Custom error", ex.getMessage());
    }
}
