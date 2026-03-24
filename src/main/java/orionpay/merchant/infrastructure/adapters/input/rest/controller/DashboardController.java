package orionpay.merchant.infrastructure.adapters.input.rest.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import orionpay.merchant.application.ports.input.rest.dto.DashboardSummaryDto;
import orionpay.merchant.config.AuthenticatedUser;
import orionpay.merchant.domain.excepion.ForbiddenException;
import orionpay.merchant.domain.service.GetDashboardSummaryUseCase;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*") // Permite CORS para todos (dev) ou ajuste para localhost:3000
public class DashboardController {

    private final GetDashboardSummaryUseCase dashboardUseCase;

    @GetMapping("/{merchantId}/summary")
    public ResponseEntity<DashboardSummaryDto> getSummary(@PathVariable UUID merchantId, Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        if (user.getRole().name().equals("ROLE_MERCHANT") && (user.getMerchantId() == null || !user.getMerchantId().equals(merchantId))) {
            throw new ForbiddenException("O lojista só pode visualizar o próprio saldo.");
        }

        // O UseCase orquestra a busca nos schemas core, ops e accounting
        DashboardSummaryDto summary = dashboardUseCase.execute(merchantId);
        return ResponseEntity.ok(summary);
    }
}
