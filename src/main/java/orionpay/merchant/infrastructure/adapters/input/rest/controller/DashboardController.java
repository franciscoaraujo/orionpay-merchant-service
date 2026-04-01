package orionpay.merchant.infrastructure.adapters.input.rest.controller;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import orionpay.merchant.application.ports.input.rest.dto.DashboardSummaryDto;
import orionpay.merchant.config.AuthenticatedUser;
import orionpay.merchant.domain.excepion.ForbiddenException;
import orionpay.merchant.domain.service.GetDashboardSummaryUseCase;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaDailySummaryRepository;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class DashboardController {

    private final GetDashboardSummaryUseCase dashboardUseCase;
    private final JpaDailySummaryRepository dailySummaryRepository;

    @GetMapping("/{merchantId}/summary")
    public ResponseEntity<DashboardSummaryDto> getSummary(
            @PathVariable UUID merchantId,
            @RequestParam(defaultValue = "hoje") String period,
            Authentication authentication) {
        
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        if (user.getRole().name().equals("ROLE_MERCHANT") && (user.getMerchantId() == null || !user.getMerchantId().equals(merchantId))) {
            throw new ForbiddenException("O lojista só pode visualizar o próprio saldo.");
        }

        DashboardSummaryDto summary = dashboardUseCase.execute(merchantId, period);
        return ResponseEntity.ok(summary);
    }

    /**
     * ENDPOINT DE MANUTENÇÃO: Rebuilda o resumo do dashboard a partir das transações reais.
     * Use isso se o dashboard vier zerado após testes manuais.
     */
    @PostMapping("/{merchantId}/rebuild-summary")
    @Transactional
    public ResponseEntity<String> rebuild(@PathVariable UUID merchantId) {
        dailySummaryRepository.rebuildSummary(merchantId);
        return ResponseEntity.ok("Resumo reconstruído com sucesso para o lojista " + merchantId);
    }
}
