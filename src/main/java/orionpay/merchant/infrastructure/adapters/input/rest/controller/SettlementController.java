package orionpay.merchant.infrastructure.adapters.input.rest.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.service.GetSettlementDayDetailUseCase;
import orionpay.merchant.domain.service.GetSettlementScheduleUseCase;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.SettlementDayDetailResponse;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.SettlementScheduleResponse;
import orionpay.merchant.infrastructure.adapters.input.rest.security.SecurityContextService;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Controller responsável por operações relacionadas à agenda de liquidação (Settlement Schedule).
 *
 * Endpoints:
 * - GET /api/v1/settlements/schedule: Retorna agenda financeira detalhada agrupada por data de liquidação esperada
 * - GET /api/v1/settlements/schedule/{date}: Retorna detalhes de transações de um dia específico
 */
@Log4j2
@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final GetSettlementScheduleUseCase scheduleUseCase;
    private final GetSettlementDayDetailUseCase dayDetailUseCase;
    private final SecurityContextService securityContextService;

    /**
     * Retorna a agenda financeira detalhada da liquidação.
     *
     * Os dados são agrupados por data esperada de liquidação (expected_settlement_date).
     * Para cada data, retorna:
     * - total_gross: Soma de tudo naquela data
     * - total_net: Valor líquido após MDR
     * - status_summary: Lista dos status presentes (ex: PENDING, SCHEDULED, SETTLED)
     * - transactionCount: Quantidade de transações naquela data
     * - mdrAmount: Valor total de MDR (juros/taxas)
     * - dailyAverageTransaction: Ticket médio do dia
     *
     * @param startDate Data inicial do período (obrigatório)
     * @param endDate Data final do período (obrigatório)
     * @param status Status específico para filtrar (opcional). Se null, retorna todos os status
     * @return ResponseEntity com a agenda agrupada por dia
     *
     * @example GET /api/v1/settlements/schedule?startDate=2024-01-01&endDate=2024-01-31&status=SCHEDULED
     */
    @GetMapping("/schedule")
    public ResponseEntity<SettlementScheduleResponse> getSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status
    ) {
        try {
            UUID merchantId = securityContextService.getCurrentMerchantId();

            SettlementScheduleResponse response = scheduleUseCase.execute(merchantId, startDate, endDate, status);

            return ResponseEntity.ok(response);

        } catch (DomainException e) {
            log.warn("Erro de domínio ao consultar agenda: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao consultar agenda de liquidação", e);
            throw new DomainException("Erro ao consultar agenda de liquidação", "SETTLEMENT_SCHEDULE_ERROR");
        }
    }

    /**
     * Retorna o detalhe detalhado de todas as transações/parcelas de um dia específico.
     *
     * Útil para visualizar o breakdown completo de uma data de liquidação esperada.
     *
     * @param settlementDate Data de liquidação desejada (obrigatório)
     * @param status Status específico para filtrar (opcional)
     * @param page Número da página (default: 0)
     * @param size Tamanho da página (default: 50)
     * @return ResponseEntity com detalhes agregados e paginados das transações do dia
     *
     * @example GET /api/v1/settlements/schedule/2024-01-15?status=SCHEDULED&page=0&size=20
     */
    @GetMapping("/schedule/{settlementDate}")
    public ResponseEntity<SettlementDayDetailResponse> getScheduleDayDetail(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate settlementDate,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        try {
            UUID merchantId = securityContextService.getCurrentMerchantId();

            SettlementDayDetailResponse response = dayDetailUseCase.execute(
                    merchantId, settlementDate, status, null
            );

            return ResponseEntity.ok(response);

        } catch (DomainException e) {
            log.warn("Erro de domínio ao consultar detalhe do dia: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao consultar detalhe do dia de liquidação", e);
            throw new DomainException("Erro ao consultar detalhe do dia", "SETTLEMENT_DAY_DETAIL_ERROR");
        }
    }
}
