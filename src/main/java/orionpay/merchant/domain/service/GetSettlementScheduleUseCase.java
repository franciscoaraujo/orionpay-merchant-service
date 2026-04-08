package orionpay.merchant.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.SettlementScheduleResponse;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.DailyScheduleProjection;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaSettlementEntryRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetSettlementScheduleUseCase {

    private final JpaSettlementEntryRepository repository;

    public SettlementScheduleResponse execute(UUID merchantId, LocalDate start, LocalDate end, String status) {
        // Converter status do frontend (português) para o banco de dados (inglês)
        String statusInEnglish = convertStatusToEnglish(status);

        List<DailyScheduleProjection> projections = repository.findDailySchedule(merchantId, start, end, statusInEnglish);

        BigDecimal totalPeriodGross = BigDecimal.ZERO;
        BigDecimal totalPeriodNet = BigDecimal.ZERO;
        Integer totalTransactionsInPeriod = 0;

        List<SettlementScheduleResponse.DailySchedule> dailySchedules = new ArrayList<>();

        for (DailyScheduleProjection p : projections) {
            BigDecimal grossAmount = p.getTotalGross() != null ? p.getTotalGross() : BigDecimal.ZERO;
            BigDecimal netAmount = p.getTotalNet() != null ? p.getTotalNet() : BigDecimal.ZERO;
            Long count = p.getCount() != null ? p.getCount() : 0L;

            totalPeriodGross = totalPeriodGross.add(grossAmount);
            totalPeriodNet = totalPeriodNet.add(netAmount);
            totalTransactionsInPeriod += count.intValue();

            // Calcular MDR amount (gross - net)
            BigDecimal mdrAmount = grossAmount.subtract(netAmount);

            // Calcular média por transação naquele dia
            BigDecimal dailyAverageTransaction = count > 0
                    ? grossAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            SettlementScheduleResponse.DailySchedule daily = SettlementScheduleResponse.DailySchedule.builder()
                    .date(p.getSettlementDate())
                    .totalGross(grossAmount)
                    .totalNet(netAmount)
                    .mdrAmount(mdrAmount)
                    .statusSummary(parseStatuses(p.getStatuses()))
                    .transactionCount(count.intValue())
                    .dailyAverageTransaction(dailyAverageTransaction)
                    .build();

            dailySchedules.add(daily);
        }

        return SettlementScheduleResponse.builder()
                .periodStart(start)
                .periodEnd(end)
                .totalPeriodGross(totalPeriodGross)
                .totalPeriodNet(totalPeriodNet)
                .totalTransactionsInPeriod(totalTransactionsInPeriod)
                .schedule(dailySchedules)
                .build();
    }

    private Set<String> parseStatuses(String statuses) {
        if (statuses == null || statuses.isBlank()) return Collections.emptySet();
        return Arrays.stream(statuses.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    private String convertStatusToEnglish(String status) {
        if (status == null) {
            return null;
        }

        // Lógica para converter o status do português para o inglês
        // Isso é apenas um exemplo, a conversão real dependerá dos requisitos do seu aplicativo
        switch (status.toUpperCase()) {
            case "PENDENTE":
                return "PENDING";
            case "AGENDADO":
                return "SCHEDULED";
            case "ANTECIPADO":
                return "ANTICIPATED";
            case "BLOQUEADO":
                return "BLOCKED";
            case "LIQUIDADO":
            case "SETTLED":
                return "SETTLED";
            case "PAGO":
            case "PAID":
                return "PAID";
            case "DISPUTA":
                return "DISPUTE";
            case "FALHA":
            case "FAILED":
                return "FAILED";
            case "PRÉ-PAGO":
            case "PREPAID":
                return "PREPAID";
            default:
                return status.toUpperCase(); // Mantém o valor original se não reconhecer
        }
    }
}
