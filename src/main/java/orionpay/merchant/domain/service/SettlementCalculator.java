package orionpay.merchant.domain.service;

import lombok.Builder;
import lombok.Data;
import orionpay.merchant.domain.model.TransactionEvent;
import orionpay.merchant.domain.model.enums.ProductType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculador de parcelas com ajuste de precisão financeira e respeito aos prazos de liquidação (D+N).
 */
public class SettlementCalculator {

    @Data
    @Builder
    public static class CalculatedInstallment {
        private int installmentNumber;
        private BigDecimal grossAmount;
        private BigDecimal mdrAmount;
        private BigDecimal netAmount;
        private LocalDateTime expectedSettlementDate;
    }

    public List<CalculatedInstallment> calculate(TransactionEvent event, BigDecimal mdrPercentage) {
        int installmentsCount = (event.installments() != null && event.installments() > 0) ? event.installments() : 1;
        
        if (installmentsCount < 1) {
            throw new IllegalArgumentException("O número de parcelas deve ser maior ou igual a 1.");
        }

        BigDecimal mdrRate = mdrPercentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal totalMdrAmount = event.amount().multiply(mdrRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalNetAmount = event.amount().subtract(totalMdrAmount).setScale(2, RoundingMode.HALF_UP);

        List<CalculatedInstallment> calculated = new ArrayList<>();
        
        BigDecimal accumulatedGross = BigDecimal.ZERO;
        BigDecimal accumulatedMdr = BigDecimal.ZERO;
        BigDecimal accumulatedNet = BigDecimal.ZERO;

        for (int i = 1; i <= installmentsCount; i++) {
            BigDecimal installmentGross;
            BigDecimal installmentMdr;
            BigDecimal installmentNet;

            if (i < installmentsCount) {
                installmentGross = event.amount().divide(BigDecimal.valueOf(installmentsCount), 2, RoundingMode.DOWN);
                installmentMdr = totalMdrAmount.divide(BigDecimal.valueOf(installmentsCount), 2, RoundingMode.DOWN);
                installmentNet = totalNetAmount.divide(BigDecimal.valueOf(installmentsCount), 2, RoundingMode.DOWN);

                accumulatedGross = accumulatedGross.add(installmentGross);
                accumulatedMdr = accumulatedMdr.add(installmentMdr);
                accumulatedNet = accumulatedNet.add(installmentNet);
            } else {
                installmentGross = event.amount().subtract(accumulatedGross);
                installmentMdr = totalMdrAmount.subtract(accumulatedMdr);
                installmentNet = totalNetAmount.subtract(accumulatedNet);
            }

            // CORREÇÃO DA DATA DE LIQUIDAÇÃO:
            // 1. Pega os dias padrão do produto (DEBIT = 1, CREDIT = 30)
            int baseDays = event.productType().getSettlementDays();
            
            // 2. Calcula a data base (D+1 ou D+30)
            LocalDateTime baseDate = event.occurredAt().plusDays(baseDays);
            
            // 3. Se for parcelado (i > 1), adicionamos meses extras sobre a data base
            LocalDateTime expectedDate = baseDate.plusMonths(i - 1L);

            calculated.add(CalculatedInstallment.builder()
                    .installmentNumber(i)
                    .grossAmount(installmentGross)
                    .mdrAmount(installmentMdr)
                    .netAmount(installmentNet)
                    .expectedSettlementDate(expectedDate)
                    .build());
        }

        return calculated;
    }
}
