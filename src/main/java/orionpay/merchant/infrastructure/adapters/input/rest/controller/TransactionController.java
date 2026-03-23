package orionpay.merchant.infrastructure.adapters.input.rest.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import orionpay.merchant.domain.model.ExtratoTransaction;
import orionpay.merchant.domain.service.AuthorizeTransactionUseCase;
import orionpay.merchant.domain.service.GetTransactionDetailUseCase;
import orionpay.merchant.domain.service.GetTransactionExtratoUseCase;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionRequest;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionResponse;
import orionpay.merchant.domain.model.ExtratoTransactionDetail;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class TransactionController {

    private final AuthorizeTransactionUseCase authorizeUseCase;
    private final GetTransactionExtratoUseCase getTransactionExtratoUseCase;
    private final GetTransactionDetailUseCase getTransactionDetailUseCase;


    @PostMapping("/authorize")
    public ResponseEntity<TransactionResponse> authorize(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransactionRequest request
    ) {
        // Passa a chave de idempotência para o Use Case
        TransactionResponse response = authorizeUseCase.execute(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{merchantId}/extrato")
    public ResponseEntity<Page<ExtratoTransaction>> getExtrato(
            @PathVariable UUID merchantId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(getTransactionExtratoUseCase.execute(merchantId, search, pageable));
    }

    @GetMapping("/{transactionId}/detail")
    public ResponseEntity<ExtratoTransactionDetail> getDetail(
            @PathVariable UUID transactionId,
            @RequestHeader("X-Merchant-Id") UUID merchantId) {
        return ResponseEntity.ok(getTransactionDetailUseCase.execute(transactionId, merchantId));
    }
}
