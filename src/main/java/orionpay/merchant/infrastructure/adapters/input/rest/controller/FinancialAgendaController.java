package orionpay.merchant.infrastructure.adapters.input.rest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import orionpay.merchant.domain.service.FinancialAgendaService;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.FinancialAgendaResponse;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.SettlementDetailResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class FinancialAgendaController {

    private final FinancialAgendaService service;

    @GetMapping("/{id}/agenda")
    public ResponseEntity<FinancialAgendaResponse> getAgenda(
            @PathVariable UUID id,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        FinancialAgendaResponse response = service.getAgenda(id, year, month, page, size, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/agenda/{settlementId}/detail")
    public ResponseEntity<SettlementDetailResponse> getSettlementDetail(
            @PathVariable UUID settlementId
    ) {
        SettlementDetailResponse response = service.getSettlementDetail(settlementId);
        return ResponseEntity.ok(response);
    }
}
