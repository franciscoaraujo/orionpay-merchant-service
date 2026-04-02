package orionpay.merchant.domain.service;

import lombok.Builder;
import lombok.Data;
import orionpay.merchant.domain.model.TransactionEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        
        BigDecimal mdrRate = mdrPercentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal totalMdrAmount = event.amount().multiply(mdrRate).setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalNetAmount = event.amount().subtract(totalMdrAmount).setScale(4, RoundingMode.HALF_UP);

        List<CalculatedInstallment> calculated = new ArrayList<>();

        for (int i = 1; i <= installmentsCount; i++) {
            BigDecimal installmentAmount = event.amount().divide(BigDecimal.valueOf(installmentsCount), 4, RoundingMode.HALF_UP);
            BigDecimal installmentNetAmount = totalNetAmount.divide(BigDecimal.valueOf(installmentsCount), 4, RoundingMode.HALF_UP);
            BigDecimal installmentMdrAmount = totalMdrAmount.divide(BigDecimal.valueOf(installmentsCount), 4, RoundingMode.HALF_UP);

            // REGRA FINANCEIRA: Vencimento no mesmo dia dos meses subsequentes (D+30, D+60...)
            // Usar plusMonths garante que se a venda foi dia 01/04, as parcelas serão 01/05, 01/06, etc.
            LocalDateTime expectedDate = event.occurredAt().plusMonths(i);

            calculated.add(CalculatedInstallment.builder()
                    .installmentNumber(i)
                    .grossAmount(installmentAmount)
                    .mdrAmount(installmentMdrAmount)
                    .netAmount(installmentNetAmount)
                    .expectedSettlementDate(expectedDate)
                    .build());
        }

        return calculated;
    }
}
