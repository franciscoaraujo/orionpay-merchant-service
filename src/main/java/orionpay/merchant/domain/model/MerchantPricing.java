package orionpay.merchant.domain.model;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.enums.ProductType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter // Adicionado para permitir desserialização e mapeamento
@NoArgsConstructor // Necessário para Jackson (Redis)
public class MerchantPricing {
    private UUID merchantId;
    private String brand;
    private ProductType productType;
    private BigDecimal mdrPercentage;
    private LocalDate effectiveDate;

    // Factory Method para criação de novo pricing
    public static MerchantPricing create(String brand, ProductType productType) {
        return new MerchantPricing(null, brand, productType, null, null);
    }

    // Construtor completo para MapStruct e reconstrução
    public MerchantPricing(
            UUID merchantId,
            String brand,
            ProductType productType,
            BigDecimal mdrPercentage,
            LocalDate effectiveDate
    ) {
        this.merchantId = merchantId;
        this.brand = brand;
        this.productType = productType;
        this.mdrPercentage = mdrPercentage;
        this.effectiveDate = effectiveDate;
    }

    // REGRA DE NEGÓCIO: Validação de Taxas Financeiras
    public void updateMdr(BigDecimal newMdr) {
        if (newMdr.compareTo(BigDecimal.ZERO) < 0 || newMdr.compareTo(new BigDecimal("30")) > 0) {
            throw new DomainException("MDR fora dos limites operacionais (0% a 30%).");
        }
        this.mdrPercentage = newMdr;
    }

    public void validateMdrLimit() {
        // REGRA: O MDR de débito nunca deve ser maior que o de crédito
        if (this.productType == ProductType.DEBIT && this.mdrPercentage.compareTo(new BigDecimal("3.00")) > 0) {
            throw new DomainException("Taxa de débito acima do limite permitido pela política comercial.");
        }
    }

    public boolean isApplicable(LocalDate date) {
        return !date.isBefore(effectiveDate);
    }
}