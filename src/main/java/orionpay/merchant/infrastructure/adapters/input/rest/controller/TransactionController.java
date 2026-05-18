package orionpay.merchant.infrastructure.adapters.input.rest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import orionpay.merchant.config.AuthenticatedUser;
import orionpay.merchant.domain.service.AuthorizeTransactionUseCase;
import orionpay.merchant.domain.service.GetTransactionExtratoUseCase;
import orionpay.merchant.domain.service.RefundTransactionUseCase;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.RefundRequest;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionExtratoResponse;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionRequest;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionResponse;
import orionpay.merchant.infrastructure.adapters.input.rest.security.SecurityContextService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class TransactionController {

    private final AuthorizeTransactionUseCase authorizeUseCase;
    private final GetTransactionExtratoUseCase getExtratoUseCase;
    private final RefundTransactionUseCase refundUseCase;
    private final SecurityContextService securityContextService;

    @PostMapping("/authorize")
    public ResponseEntity<TransactionResponse> authorize(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransactionRequest request
    ) {
        // Passa a chave de idempotência para o Use Case
        TransactionResponse response = authorizeUseCase.execute(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

        TransactionResponse response = authorizeUseCase.execute(request, idempotencyKey);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/extrato")
    public ResponseEntity<Page<TransactionExtratoResponse>> getExtrato(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        UUID merchantId = securityContextService.getCurrentMerchantId();
        Page<TransactionExtratoResponse> extrato = getExtratoUseCase.execute(merchantId, pageable);
        return ResponseEntity.ok(extrato);
    }

    @PostMapping("/refund")
    public ResponseEntity<Void> requestRefund(
            @Valid @RequestBody RefundRequest request
    ) {
        UUID merchantId = securityContextService.getCurrentMerchantId();
        refundUseCase.execute(merchantId, request);
        return ResponseEntity.ok().build();
    }
}
