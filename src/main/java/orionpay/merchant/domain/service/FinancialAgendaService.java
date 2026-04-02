package orionpay.merchant.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.FinancialAgendaResponse;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.SettlementDetailResponse;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.AgendaItemProjection;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaSettlementEntryRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialAgendaService {

    private final JpaSettlementEntryRepository repository;

    public FinancialAgendaResponse getAgenda(UUID merchantId, int year, int month, int page, int size, String statusStr) {
        log.info("Buscando agenda financeira para merchant: {} | Mês: {}/{}", merchantId, month, year);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        var totalGross = repository.sumGrossAmountByPeriod(merchantId, start, end);
        var totalCommitted = repository.sumCommittedAmountByPeriod(merchantId, start, end);
        var totalAvailable = repository.sumAvailableAmountByPeriod(merchantId, start, end);

        Pageable pageable = PageRequest.of(page, size);
        Page<AgendaItemProjection> entitiesPage = repository.findAgendaByPeriod(merchantId, start, end, statusStr, pageable);

        List<FinancialAgendaResponse.AgendaItem> items = entitiesPage.getContent().stream()
                .map(this::toItem)
                .collect(Collectors.toList());

        return FinancialAgendaResponse.builder()
                .summary(FinancialAgendaResponse.AgendaSummary.builder()
                        .totalGrossAmount(totalGross)
                        .totalCommittedAmount(totalCommitted)
                        .totalAvailableAmount(totalAvailable)
                        .build())
                .items(items)
                .totalPages(entitiesPage.getTotalPages())
                .totalElements(entitiesPage.getTotalElements())
                .build();
    }

    public SettlementDetailResponse getSettlementDetail(UUID id) {
        log.info("Buscando detalhes da liquidação: {}", id);
        
        AgendaItemProjection projection = repository.findDetailById(id)
                .orElseThrow(() -> new DomainException("Detalhamento da liquidação não encontrado.", "SETTLEMENT_NOT_FOUND"));

        return SettlementDetailResponse.builder()
                .idExt(projection.getIdExt())
                .transactionId(projection.getTransactionId())
                .nsu(projection.getNsu())
                .settlementDate(projection.getSettlementDate())
                .grossAmount(projection.getGrossAmount())
                .originalAmount(projection.getOriginalAmount())
                .mdrPercentage(projection.getMdrPercentage())
                .mdrAmount(projection.getMdrAmount())
                .netAmount(projection.getNetAmount())
                .installmentLabel(mapInstallmentLabel(projection))
                .status(mapStatus(projection))
                .titularidade(mapTitularidade(projection))
                .build();
    }

    private String mapInstallmentLabel(AgendaItemProjection projection) {
        if (projection.getInstallmentNumber() == null) return "1/1";
        return projection.getInstallmentNumber().toString();
    }

    private String mapStatus(AgendaItemProjection projection) {
        if (projection.getPaidAt() != null) return "PAGO";
        // CORREÇÃO: SCHEDULED deve ser mapeado como AGENDADO para o lojista
        if ("SCHEDULED".equalsIgnoreCase(projection.getStatus())) return "AGENDADO";
        if ("ANTICIPATED".equalsIgnoreCase(projection.getStatus())) return "ANTECIPADO";
        return "PENDENTE";
    }

    private String mapTitularidade(AgendaItemProjection projection) {
        if (Boolean.TRUE.equals(projection.getBlocked())) return "VINCULADO A GARANTIA";
        if (Boolean.TRUE.equals(projection.getAnticipated()) || "ANTICIPATED".equalsIgnoreCase(projection.getStatus())) return "ANTECIPADO";
        return "DISPONÍVEL";
    }

    private FinancialAgendaResponse.AgendaItem toItem(AgendaItemProjection projection) {
        return FinancialAgendaResponse.AgendaItem.builder()
                .idExt(projection.getIdExt())
                .settlementDate(projection.getSettlementDate())
                .transactionDate(projection.getTransactionDate())
                .grossAmount(projection.getGrossAmount())
                .mdrAmount(projection.getMdrAmount())
                .netAmount(projection.getNetAmount())
                .status(mapStatus(projection))
                .titularidade(mapTitularidade(projection))
                .nsu(projection.getNsu())
                .cardBrand(projection.getCardBrand())
                .cardLastFour(projection.getCardLastFour())
                .productType(projection.getProductType())
                .build();
    }
}
