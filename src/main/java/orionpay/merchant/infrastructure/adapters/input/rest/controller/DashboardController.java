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
import orionpay.merchant.infrastructure.adapters.input.rest.dto.DashboardMetricsDTO;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.OutboxEventEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.TransactionSummaryProjection;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaDailySummaryRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaOutboxRepository;
import orionpay.merchant.infrastructure.adapters.input.rest.security.SecurityContextService;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Permite acesso de qualquer origem
public class DashboardController {

    private final GetDashboardSummaryUseCase dashboardUseCase;
    private final JpaDailySummaryRepository dailySummaryRepository;
    private final JpaOutboxRepository outboxRepository;
    private final SecurityContextService securityContextService;

    @GetMapping("/metrics/today")
    public ResponseEntity<DashboardMetricsDTO> getTodayMetrics() {
        UUID merchantId = securityContextService.getCurrentMerchantId();
        LocalDate today = LocalDate.now();

        // 1. Busca o resumo consolidado do dia
        TransactionSummaryProjection summary = dailySummaryRepository.findConsolidatedSummaryByPeriod(
                merchantId,
                today,
                today
        );

        // 2. Conta os eventos pendentes no outbox
        Integer pendingOutboxEvents = outboxRepository.countByStatus(OutboxEventEntity.OutboxStatus.PENDING);

        // 3. Constrói o DTO de resposta
        var metrics = new DashboardMetricsDTO(
                summary.getApprovedCount(),
                summary.getTotalVolume(),
                pendingOutboxEvents
        );

        return ResponseEntity.ok(metrics);
    }

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
