package orionpay.merchant.infrastructure.adapters.input.rest.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import orionpay.merchant.application.ports.input.rest.dto.MerchantResponse;
import orionpay.merchant.domain.service.RegisterMerchantUseCase;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.MerchantRegistrationRequest;

@RestController
@RequestMapping("/api/v1/merchants")
@CrossOrigin(origins = "*", allowedHeaders = "*") // Permite CORS para todos (dev) ou ajuste para localhost:3000
@RequiredArgsConstructor
public class MerchantController {

    private final RegisterMerchantUseCase merchantUseCase;


    @PostMapping("/onboarding")
    public ResponseEntity<MerchantResponse> onboarding(@Valid @RequestBody MerchantRegistrationRequest request) {
        MerchantResponse response = merchantUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
