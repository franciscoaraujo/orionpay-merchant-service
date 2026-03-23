package orionpay.merchant.infrastructure.adapters.input.rest.controller;

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
import orionpay.merchant.application.ports.input.rest.dto.MerchantResponse;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.excepion.GlobalExceptionHandler;
import orionpay.merchant.domain.model.enums.AccountType;
import orionpay.merchant.domain.model.enums.MerchantStatus;
import orionpay.merchant.domain.service.RegisterMerchantUseCase;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.MerchantRegistrationRequest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MerchantControllerTest {

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
                "Loja Teste LTDA",
                "12345678000195",
                "loja@test.com",
                "01310100",
                "Rua das Flores",
                "100",
                null,
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
    void onboarding_shouldReturn201_whenRequestIsValid() throws Exception {
        MerchantRegistrationRequest request = buildValidRequest();
        MerchantResponse response = new MerchantResponse(
                UUID.randomUUID(), "Loja Teste LTDA", "12345678000195",
                "loja@test.com", MerchantStatus.PROVISIONAL,
                new MerchantResponse.AddressResponse(
                        "Rua das Flores", "100", "Centro", "São Paulo", "SP", "01310100"),
                LocalDateTime.now()
        );

        when(registerMerchantUseCase.execute(any(MerchantRegistrationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/merchants/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Loja Teste LTDA"))
                .andExpect(jsonPath("$.document").value("12345678000195"))
                .andExpect(jsonPath("$.status").value("PROVISIONAL"));
    }

    @Test
    void onboarding_shouldReturn400_whenUseCaseThrowsDomainException() throws Exception {
        MerchantRegistrationRequest request = buildValidRequest();

        when(registerMerchantUseCase.execute(any()))
                .thenThrow(new DomainException("Lojista já cadastrado.", "MERCHANT_ALREADY_EXISTS"));

        mockMvc.perform(post("/api/v1/merchants/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lojista já cadastrado."));
    }
}
