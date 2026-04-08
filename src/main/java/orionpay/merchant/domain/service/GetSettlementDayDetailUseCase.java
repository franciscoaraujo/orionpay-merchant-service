package orionpay.merchant.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.SettlementDayDetailResponse;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.AgendaItemProjection;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaSettlementEntryRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UseCase para recuperar detalhes das transações de um dia específico na agenda de liquidação.
 * Retorna lista detalhada de todas as transações/parcelas liquidáveis naquela data.
 */
@Service
@RequiredArgsConstructor
public class GetSettlementDayDetailUseCase {

    private final JpaSettlementEntryRepository repository;

    /**
     * Recupera o detalhe de todas as transações de um dia específico.
     *
     * @param merchantId ID do lojista
     * @param settlementDate Data de liquidação (sem hora, apenas data)
     * @param status Status específico para filtrar (opcional)
     * @param pageable Paginação para limitar resultados
     * @return Detalhes agregados do dia com lista de transações
     */
    public SettlementDayDetailResponse execute(
            UUID merchantId,
            LocalDate settlementDate,
            String status,
            Pageable pageable
    ) {
        if (merchantId == null || settlementDate == null) {
            throw new DomainException("Lojista e data de liquidação são obrigatórios", "INVALID_PARAMS");
        }

        // Conversão para intervalo de tempo (início e fim do dia)
        LocalDateTime dayStart = settlementDate.atStartOfDay();
        LocalDateTime dayEnd = settlementDate.atTime(23, 59, 59);

        // Recuperar dados paginados
        Page<AgendaItemProjection> transactionsPage = repository.findAgendaByPeriod(
                merchantId, dayStart, dayEnd, status, pageable
        );

        List<AgendaItemProjection> transactions = transactionsPage.getContent();

        // Calcular agregações
        BigDecimal totalGross = transactions.stream()
                .map(AgendaItemProjection::getGrossAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMdr = transactions.stream()
                .map(AgendaItemProjection::getMdrAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNet = transactions.stream()
                .map(AgendaItemProjection::getNetAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Contadores por status
        Map<String, Long> statusCount = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getStatus() != null ? t.getStatus() : "UNKNOWN",
                        Collectors.counting()
                ));

        // Contar bloqueados e antecipados
        long blockedCount = transactions.stream()
                .filter(t -> Boolean.TRUE.equals(t.getBlocked()))
                .count();

        long anticipatedCount = transactions.stream()
                .filter(t -> Boolean.TRUE.equals(t.getAnticipated()))
                .count();

        // Média por transação
        BigDecimal averageTransaction = transactions.size() > 0
                ? totalGross.divide(BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<SettlementDayDetailResponse.TransactionDetail> transactionDetails = transactions.stream()
                .map(this::mapToTransactionDetail)
                .collect(Collectors.toList());

        return SettlementDayDetailResponse.builder()
                .settlementDate(settlementDate)
                .totalGross(totalGross)
                .totalMdr(totalMdr)
                .totalNet(totalNet)
                .averageTransaction(averageTransaction)
                .totalCount(transactions.size())
                .blockedCount((int) blockedCount)
                .anticipatedCount((int) anticipatedCount)
                .statusBreakdown(statusCount)
                .transactions(transactionDetails)
                .pageNumber(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalPages(transactionsPage.getTotalPages())
                .totalElements(transactionsPage.getTotalElements())
                .build();
    }

    private SettlementDayDetailResponse.TransactionDetail mapToTransactionDetail(AgendaItemProjection projection) {
        return SettlementDayDetailResponse.TransactionDetail.builder()
                .idExt(projection.getIdExt())
                .transactionId(projection.getTransactionId())
                .nsu(projection.getNsu())
                .transactionDate(projection.getTransactionDate())
                .settlementDate(projection.getSettlementDate())
                .grossAmount(projection.getGrossAmount())
                .originalAmount(projection.getOriginalAmount())
                .mdrPercentage(projection.getMdrPercentage())
                .mdrAmount(projection.getMdrAmount())
                .netAmount(projection.getNetAmount())
                .status(projection.getStatus())
                .paidAt(projection.getPaidAt())
                .cardBrand(projection.getCardBrand())
                .cardLastFour(projection.getCardLastFour())
                .productType(projection.getProductType())
                .blocked(projection.getBlocked() != null && projection.getBlocked())
                .anticipated(projection.getAnticipated() != null && projection.getAnticipated())
                .installmentNumber(projection.getInstallmentNumber())
                .installmentLabel(formatInstallment(projection.getInstallmentNumber()))
                .build();
    }

    private String formatInstallment(Integer installmentNumber) {
        return installmentNumber != null ? installmentNumber.toString() : "À vista";
    }
}

