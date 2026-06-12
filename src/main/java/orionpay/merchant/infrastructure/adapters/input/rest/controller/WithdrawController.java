package orionpay.merchant.infrastructure.adapters.input.rest.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import orionpay.merchant.domain.excepion.PayoutPendingException;
import orionpay.merchant.domain.service.GetPayoutHistoryUseCase;
import orionpay.merchant.domain.service.WithdrawMoneyUseCase;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.PayoutHistoryResponse;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.WithdrawRequest;
import orionpay.merchant.infrastructure.adapters.input.rest.security.SecurityContextService;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/merchants/withdrawals") // Restaurado o padrão original
@RequiredArgsConstructor
public class WithdrawController {

    private final WithdrawMoneyUseCase withdrawUseCase;
    private final GetPayoutHistoryUseCase getPayoutHistoryUseCase;
    private final SecurityContextService securityContextService;

    @PostMapping
    public ResponseEntity<Map<String, String>> requestWithdraw(
            @RequestBody WithdrawRequest request,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey
    ) {
        // O merchantId é extraído do Token JWT por segurança
        UUID merchantId = securityContextService.getCurrentMerchantId();
        
        try {
            withdrawUseCase.execute(request, idempotencyKey);
            return ResponseEntity.ok(Map.of("message", "Saque realizado com sucesso."));
            
        } catch (PayoutPendingException e) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", e.getMessage(), "status", "PROCESSING"));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<Page<PayoutHistoryResponse>> getPayoutHistory(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        UUID merchantId = securityContextService.getCurrentMerchantId();
        
        log.info("Auditoria: Merchant {} solicitando histórico de saques.", merchantId);
        
        Page<PayoutHistoryResponse> response = getPayoutHistoryUseCase.execute(merchantId, pageable);
        
        return ResponseEntity.ok(response);
    }
}
