package orionpay.merchant.infrastructure.adapters.input.rest.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import orionpay.merchant.infrastructure.adapters.output.email.SendEmailReceiptUseCase;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/transactions") // Ajustado para v1 para bater com o path do erro
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*") // Permite CORS para todos (dev) ou ajuste para localhost:3000
public class TransactionEmailController {

    private final SendEmailReceiptUseCase sendEmailReceiptUseCase;

    @PostMapping("/{transactionId}/send-email")
    public ResponseEntity<Void> sendEmail(
            @PathVariable UUID transactionId,
            @RequestHeader("X-Merchant-Id") UUID merchantId,
            @RequestBody EmailRequest request) {

        sendEmailReceiptUseCase.execute(transactionId, merchantId, request.email());
        return ResponseEntity.accepted().build();
    }

    public record EmailRequest(String email) {}
}
