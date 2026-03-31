package orionpay.merchant.infrastructure.adapters.input.rest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import orionpay.merchant.domain.service.AnticipationService;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.AnticipationRequest;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.AnticipationResponse;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class AnticipationController {

    private final AnticipationService service;

    @GetMapping("/{id}/anticipation/available")
    public ResponseEntity<AnticipationResponse> getAvailableForAnticipation(@PathVariable UUID id) {
        AnticipationResponse response = service.getAvailableSettlements(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/anticipation/pricing")
    public ResponseEntity<Map<String, BigDecimal>> getAnticipationPricing(@PathVariable UUID id) {
        BigDecimal fee = service.getAnticipationFee(id);
        return ResponseEntity.ok(Map.of("anticipationFee", fee));
    }

    @PostMapping("/{id}/anticipation/execute")
    public ResponseEntity<Void> executeAnticipation(
            @PathVariable UUID id,
            @RequestBody AnticipationRequest request) {
        service.executeAnticipation(id, request);
        return ResponseEntity.ok().build();
    }
}
