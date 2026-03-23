package orionpay.merchant.infrastructure.adapters.input.rest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import orionpay.merchant.domain.service.WithdrawMoneyUseCase;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.WithdrawRequest;

@RestController
@RequestMapping("/api/v1/withdrawals")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class WithdrawController {

    private final WithdrawMoneyUseCase withdrawMoneyUseCase;

    @PostMapping
    public ResponseEntity<Void> requestWithdraw(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody WithdrawRequest request
    ) {
        withdrawMoneyUseCase.execute(request, idempotencyKey);
        return ResponseEntity.accepted().build();
    }
}
