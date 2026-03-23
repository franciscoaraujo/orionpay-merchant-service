package orionpay.merchant.infrastructure.adapters.input.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.excepion.GlobalExceptionHandler;
import orionpay.merchant.domain.service.WithdrawMoneyUseCase;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.WithdrawRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WithdrawControllerTest {

    @Mock
    private WithdrawMoneyUseCase withdrawMoneyUseCase;

    @InjectMocks
    private WithdrawController withdrawController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private final UUID merchantId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(withdrawController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    private WithdrawRequest buildValidRequest() {
        return new WithdrawRequest(merchantId, new BigDecimal("100.00"), "pix@email.com");
    }

    @Test
    void requestWithdraw_shouldReturn202_whenRequestIsValid() throws Exception {
        WithdrawRequest request = buildValidRequest();
        doNothing().when(withdrawMoneyUseCase).execute(any(WithdrawRequest.class));

        mockMvc.perform(post("/api/v1/merchants/withdrawals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

    @Test
    void requestWithdraw_shouldReturn400_whenUseCaseThrowsDomainException() throws Exception {
        WithdrawRequest request = buildValidRequest();
        doThrow(new DomainException("Saldo disponível insuficiente para saque imediato (Vendas futuras não incluídas)."))
                .when(withdrawMoneyUseCase).execute(any(WithdrawRequest.class));

        mockMvc.perform(post("/api/v1/merchants/withdrawals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Saldo disponível insuficiente para saque imediato (Vendas futuras não incluídas)."));
    }
}
