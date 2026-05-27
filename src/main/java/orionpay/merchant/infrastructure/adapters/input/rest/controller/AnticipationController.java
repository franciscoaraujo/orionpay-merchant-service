package orionpay.merchant.infrastructure.adapters.input.rest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import orionpay.merchant.domain.service.AnticipationService;
import orionpay.merchant.domain.service.PrepaymentUseCase;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.AnticipationRequest;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.AnticipationResponse;
import orionpay.merchant.infrastructure.adapters.input.rest.security.SecurityContextService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class AnticipationController {

    private final AnticipationService service;
    private final PrepaymentUseCase prepaymentUseCase;
    private final SecurityContextService securityContextService;

    /**
     * Endpoint p/ listar recebíveis disponíveis para antecipação.
     * @param id Extraído da URL (prioridade p/ testes) ou do Contexto de Segurança.
     */
    @GetMapping("/{id}/anticipation/available")
    public ResponseEntity<AnticipationResponse> getAvailableForAnticipation(@PathVariable UUID id) {
        // Para testes manuais, usamos o ID da URL. 
        // Em produção, o ideal é validar se o ID da URL == ID do Token.
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
        prepaymentUseCase.execute(id, request);
        return ResponseEntity.ok().build();
    }
}
