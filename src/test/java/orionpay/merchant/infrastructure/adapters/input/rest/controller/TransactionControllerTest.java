package orionpay.merchant.infrastructure.adapters.input.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import orionpay.merchant.domain.excepion.GlobalExceptionHandler;
import orionpay.merchant.domain.excepion.TransactionNotFoundException;
import orionpay.merchant.domain.model.ExtratoTransaction;
import orionpay.merchant.domain.model.ExtratoTransactionDetail;
import orionpay.merchant.domain.model.enums.ProductType;
import orionpay.merchant.domain.model.enums.TransactionStatus;
import orionpay.merchant.domain.service.AuthorizeTransactionUseCase;
import orionpay.merchant.domain.service.GetTransactionDetailUseCase;
import orionpay.merchant.domain.service.GetTransactionExtratoUseCase;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionRequest;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private AuthorizeTransactionUseCase authorizeUseCase;
    @Mock
    private GetTransactionExtratoUseCase getTransactionExtratoUseCase;
    @Mock
    private GetTransactionDetailUseCase getTransactionDetailUseCase;

    @InjectMocks
    private TransactionController transactionController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private final UUID merchantId = UUID.randomUUID();
    private final UUID transactionId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private TransactionRequest buildValidTransactionRequest() {
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

    @Test
    void authorize_shouldReturn201_whenRequestIsValid() throws Exception {
        TransactionRequest request = buildValidTransactionRequest();
        TransactionResponse response = new TransactionResponse(
                transactionId, "NSU12345", "AUTH789",
                new BigDecimal("100.00"), "BRL",
                TransactionStatus.APPROVED, ProductType.CREDIT_A_VISTA,
                "TERM-00001", LocalDateTime.now(), "Transação aprovada."
        );

        when(authorizeUseCase.execute(any(TransactionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/transactions/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nsu").value("NSU12345"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.message").value("Transação aprovada."));
    }

    @Test
    void authorize_shouldReturn400_whenUseCaseFails() throws Exception {
        TransactionRequest request = buildValidTransactionRequest();
        when(authorizeUseCase.execute(any()))
                .thenThrow(new orionpay.merchant.domain.excepion.DomainException("Lojista não encontrado."));

        mockMvc.perform(post("/api/v1/transactions/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lojista não encontrado."));
    }

    @Test
    void getExtrato_shouldReturn200_withPagedResults() throws Exception {
        ExtratoTransaction item = ExtratoTransaction.builder()
                .id(transactionId)
                .nsu("NSU001")
                .amount(new BigDecimal("100.00"))
                .createdAt(LocalDateTime.now())
                .brand("VISA")
                .lastFour("**** 1234")
                .status("Capturado")
                .externalId("optr_NSU001")
                .build();
        Pageable pageable = PageRequest.of(0, 15);
        Page<ExtratoTransaction> page = new PageImpl<>(List.of(item), pageable, 1);

        when(getTransactionExtratoUseCase.execute(any(UUID.class), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/transactions/{merchantId}/extrato", merchantId))
                .andExpect(status().isOk());
    }

    @Test
    void getDetail_shouldReturn200_whenTransactionExists() throws Exception {
        ExtratoTransactionDetail detail = ExtratoTransactionDetail.builder()
                .id(transactionId).externalId("optr_NSU001").nsu("NSU001")
                .amount(new BigDecimal("100.00")).createdAt(LocalDateTime.now())
                .status("Capturado").brand("VISA").cardBin("411111").lastFour("1234")
                .holderName("John Doe").terminalSerialNumber("TERM-00001")
                .entryMode("CHIP").authCode("AUTH789")
                .build();

        when(getTransactionDetailUseCase.execute(transactionId, merchantId)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/transactions/{transactionId}/detail", transactionId)
                        .header("X-Merchant-Id", merchantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nsu").value("NSU001"));
    }

    @Test
    void getDetail_shouldReturn404_whenTransactionNotFound() throws Exception {
        when(getTransactionDetailUseCase.execute(transactionId, merchantId))
                .thenThrow(new TransactionNotFoundException(transactionId));

        mockMvc.perform(get("/api/v1/transactions/{transactionId}/detail", transactionId)
                        .header("X-Merchant-Id", merchantId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
