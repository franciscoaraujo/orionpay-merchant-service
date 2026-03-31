package orionpay.merchant.domain.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.LedgerAccount;
import orionpay.merchant.domain.model.enums.EntryType;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.AnticipationRequest;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.AnticipationResponse;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.PricingEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.SettlementEntryEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaPricingRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaSettlementEntryRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.LedgerRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnticipationService {

    private final JpaSettlementEntryRepository settlementRepository;
    private final JpaPricingRepository pricingRepository;
    private final LedgerRepository ledgerRepository;

    public BigDecimal getAnticipationFee(UUID merchantId) {
        return pricingRepository.findByMerchantId(merchantId).stream()
                .map(PricingEntity::getAnticipationFee)
                .filter(fee -> fee != null && fee.compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .orElseThrow(() -> new DomainException("Lojista não possui taxa de antecipação configurada.", "PRICING_NOT_FOUND"));
    }

    public AnticipationResponse getAvailableSettlements(UUID merchantId) {
        log.info("Simulando antecipação para Merchant: {}", merchantId);

        BigDecimal monthlyFee = getAnticipationFee(merchantId);
        BigDecimal dailyFee = monthlyFee.divide(BigDecimal.valueOf(30), 8, RoundingMode.HALF_UP)
                                       .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);

        List<SettlementEntryEntity> availableEntries = settlementRepository.findAvailableForAnticipation(merchantId);
        LocalDate today = LocalDate.now();

        List<AnticipationResponse.AvailableSettlement> items = availableEntries.stream()
                .map(entry -> {
                    // USO DE LOCALDATE: Resolve o problema de vir "0 dias" por causa do horário
                    long daysToAnticipate = ChronoUnit.DAYS.between(today, entry.getExpectedSettlementDate().toLocalDate());
                    if (daysToAnticipate <= 0) daysToAnticipate = 0;

                    BigDecimal cost = entry.getNetAmount()
                            .multiply(dailyFee)
                            .multiply(BigDecimal.valueOf(daysToAnticipate))
                            .setScale(2, RoundingMode.HALF_UP);

                    return AnticipationResponse.AvailableSettlement.builder()
                            .settlementId(entry.getId())
                            .originalSettlementDate(entry.getExpectedSettlementDate())
                            .grossAmount(entry.getAmount())
                            .netAmount(entry.getNetAmount())
                            .anticipationCost(cost)
                            .daysToAnticipate((int) daysToAnticipate)
                            .build();
                }).collect(Collectors.toList());

        BigDecimal totalGross = items.stream().map(AnticipationResponse.AvailableSettlement::getGrossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = items.stream().map(AnticipationResponse.AvailableSettlement::getAnticipationCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNetOriginal = items.stream().map(AnticipationResponse.AvailableSettlement::getNetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return AnticipationResponse.builder()
                .totalGrossToAnticipate(totalGross)
                .totalCost(totalCost)
                .totalNetToReceive(totalNetOriginal.subtract(totalCost))
                .items(items)
                .build();
    }

    @Transactional
    public void executeAnticipation(UUID merchantId, AnticipationRequest request) {
        log.info("Executando antecipação para merchant: {} | Itens: {}", merchantId, request.settlementIds().size());
        
        BigDecimal monthlyFee = getAnticipationFee(merchantId);
        BigDecimal dailyFee = monthlyFee.divide(BigDecimal.valueOf(30), 8, RoundingMode.HALF_UP)
                                       .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        
        LocalDate today = LocalDate.now();
        BigDecimal totalAnticipatedNet = BigDecimal.ZERO;

        for (UUID settlementId : request.settlementIds()) {
            SettlementEntryEntity entry = settlementRepository.findById(settlementId)
                    .orElseThrow(() -> new DomainException("Lançamento não encontrado: " + settlementId));

            if (Boolean.TRUE.equals(entry.getAnticipated())) continue;

            long days = ChronoUnit.DAYS.between(today, entry.getExpectedSettlementDate().toLocalDate());
            BigDecimal cost = entry.getNetAmount().multiply(dailyFee).multiply(BigDecimal.valueOf(Math.max(0, days)))
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal netAfterAnticipation = entry.getNetAmount().subtract(cost);

            // 1. Atualiza o registro de liquidação
            entry.setAnticipated(true);
            entry.setNetAmount(netAfterAnticipation); // Agora o valor líquido reflete o custo
            entry.setStatus(SettlementEntryEntity.SettlementStatus.SETTLED);
            entry.setExpectedSettlementDate(LocalDateTime.now()); // Antecipado para hoje!
            settlementRepository.save(entry);

            totalAnticipatedNet = totalAnticipatedNet.add(netAfterAnticipation);
        }

        // 2. Escrituração Contábil: Crédito imediato no saldo do lojista
        if (totalAnticipatedNet.compareTo(BigDecimal.ZERO) > 0) {
            processAccounting(merchantId, totalAnticipatedNet);
        }
    }

    private void processAccounting(UUID merchantId, BigDecimal amount) {
        LedgerAccount account = ledgerRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new DomainException("Conta contábil não encontrada."));

        account.applyEntry(amount, EntryType.CREDIT);
        ledgerRepository.saveAccount(account);

        ledgerRepository.saveEntry(
                account,
                amount,
                EntryType.CREDIT,
                "Recebimento de Antecipação de Vendas",
                UUID.randomUUID(),
                LocalDateTime.now()
        );
    }
}
