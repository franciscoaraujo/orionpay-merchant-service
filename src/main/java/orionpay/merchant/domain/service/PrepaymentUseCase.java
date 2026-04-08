package orionpay.merchant.domain.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.LedgerAccount;
import orionpay.merchant.domain.model.PrepaymentSimulation;
import orionpay.merchant.domain.model.enums.EntryType;
import orionpay.merchant.domain.model.enums.SettlementStatus;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.AnticipationRequest;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.PricingEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.SettlementEntryEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaPricingRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaSettlementEntryRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.LedgerRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrepaymentUseCase {

    private final JpaSettlementEntryRepository settlementRepository;
    private final JpaPricingRepository pricingRepository;
    private final LedgerRepository ledgerRepository;

    @Transactional(rollbackOn = Exception.class)
    @CacheEvict(value = "dashboard_summary", key = "#merchantId") // CORREÇÃO: Limpa o cache do dashboard
    public void execute(UUID merchantId, AnticipationRequest request) {
        UUID batchId = UUID.randomUUID();
        log.info("Executando Lote de Antecipação: {} | Merchant: {}", batchId, merchantId);

        BigDecimal monthlyFee = getAnticipationFee(merchantId);
        LocalDate today = LocalDate.now();
        
        BigDecimal totalNetToClient = BigDecimal.ZERO;
        BigDecimal totalFeesToOrion = BigDecimal.ZERO;

        for (UUID settlementId : request.settlementIds()) {
            SettlementEntryEntity entry = settlementRepository.findById(settlementId)
                    .orElseThrow(() -> new DomainException("Lançamento não encontrado: " + settlementId));

            validateEligibility(entry, merchantId);

            PrepaymentSimulation simulation = new PrepaymentSimulation(
                    entry.getId(), entry.getNetAmount(), monthlyFee, entry.getExpectedSettlementDate().toLocalDate());

            BigDecimal cost = simulation.calculateCost(today);
            BigDecimal netFinal = simulation.calculateNetFinal(today);

            entry.setAnticipated(true);
            entry.setStatus(SettlementStatus.ANTICIPATED);
            entry.setNetAmount(netFinal);
            entry.setExpectedSettlementDate(LocalDateTime.now());
            settlementRepository.save(entry);

            totalNetToClient = totalNetToClient.add(netFinal);
            totalFeesToOrion = totalFeesToOrion.add(cost);
        }

        if (totalNetToClient.compareTo(BigDecimal.ZERO) > 0) {
            registerLedgerEntries(merchantId, totalNetToClient, totalFeesToOrion, batchId);
        }

        log.info("Lote {} finalizado. Crédito Lojista: R$ {}", batchId, totalNetToClient);
    }

    private void registerLedgerEntries(UUID merchantId, BigDecimal netClient, BigDecimal feeOrion, UUID batchId) {
        LedgerAccount account = ledgerRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new DomainException("Conta contábil não encontrada."));

        // A. Crédito do Valor Líquido (Aumenta saldo disponível)
        account.applyEntry(netClient, EntryType.PREPAYMENT_CREDIT);
        ledgerRepository.saveAccount(account);
        ledgerRepository.saveEntry(account, netClient, EntryType.PREPAYMENT_CREDIT, 
                "Crédito de Antecipação - Lote: " + batchId, batchId, LocalDateTime.now());

        // B. Lançamento da Taxa Retida (Informativo de saída)
        ledgerRepository.saveEntry(account, feeOrion, EntryType.PREPAYMENT_FEE, 
                "Custo de Antecipação - Lote: " + batchId, batchId, LocalDateTime.now());
    }

    private void validateEligibility(SettlementEntryEntity entry, UUID merchantId) {
        if (!entry.getMerchantId().equals(merchantId)) throw new DomainException("Acesso negado.");
        if (Boolean.TRUE.equals(entry.getAnticipated())) throw new DomainException("Já antecipado.");
        if (entry.getStatus() != SettlementStatus.SCHEDULED && entry.getStatus() != SettlementStatus.SETTLED) throw new DomainException("Status inválido.");
    }

    private BigDecimal getAnticipationFee(UUID merchantId) {
        return pricingRepository.findByMerchantId(merchantId).stream()
                .map(PricingEntity::getAnticipationFee)
                .filter(fee -> fee != null && fee.compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .orElseThrow(() -> new DomainException("Taxa de antecipação não definida."));
    }
}
