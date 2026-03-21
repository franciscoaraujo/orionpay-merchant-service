package orionpay.merchant.domain.service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import orionpay.merchant.domain.event.MerchantCreatedEvent;
import orionpay.merchant.domain.model.LedgerAccount;
import orionpay.merchant.domain.model.MerchantPricing;
import orionpay.merchant.domain.model.enums.Brands;
import orionpay.merchant.domain.model.enums.ProductType;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.LedgerRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.PricingRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantOnboardingListener {

    private final LedgerRepository ledgerRepository;
    private final PricingRepository pricingRepository;

    /**
     * O @TransactionalEventListener garante que a conta só será criada
     * se o "save" do Merchant não der erro no banco de dados.
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void provisionLedgerAccount(MerchantCreatedEvent event) {
        log.info("[ONBOARDING] Provisionando conta contábil para o novo lojista: {}", event.merchantId());

        // Gera um número de conta amigável e único
        String accountNumber = "CC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Cria a conta zerada
        LedgerAccount newAccount = new LedgerAccount(
                UUID.randomUUID(),      // ID da Conta
                event.merchantId(),     // Vincula ao lojista do evento
                accountNumber,          // Código da conta
                BigDecimal.ZERO,        // Saldo inicial
                0L                      // Versão inicial (Optimistic Locking)
        );

        // Salva a conta no banco
        ledgerRepository.saveAccount(newAccount);

        log.info("[ONBOARDING] Conta {} criada com sucesso para o Lojista {}.", accountNumber, event.merchantId());
    }

    // Você precisará injetar o seu repository de taxas no topo da classe, ex:
    // private final MerchantFeeRepository feeRepository;

    /**
     * Segundo Ouvinte: Aplica as taxas padrão da OrionPay para o novo lojista
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void provisionDefaultFees(MerchantCreatedEvent event) {
        log.info("[ONBOARDING] Configurando taxas padrão para o lojista: {}", event.merchantId());
        var merchantId = event.merchantId();
        // Cria a taxa padrão para Crédito à Vista (Ex: 3.5%)
        MerchantPricing creditFee = new MerchantPricing(
                merchantId,
                Brands.VISA.name(),
                ProductType.CREDIT_A_VISTA,
                new BigDecimal("3.50"),
                LocalDate.now()
        );

        // Cria a taxa padrão para Débito (Ex: 1.5%)
        MerchantPricing debitFee = new MerchantPricing(
                merchantId,
                Brands.MASTER_CARD.name(),
                ProductType.DEBIT,
                new BigDecimal("1.50"),
                LocalDate.now()
        );

        MerchantPricing creditParc = new MerchantPricing(
                merchantId,
                Brands.VISA.name(),
                ProductType.CREDIT_PARCELADO,
                new BigDecimal("4.50"),
                LocalDate.now()
        );
        debitFee.validateMdrLimit();
        // Salva as taxas no banco (ajuste conforme o seu Repository)
        pricingRepository.saveAll(List.of(creditFee, debitFee, creditParc));

        log.info("[ONBOARDING] Taxas configuradas com sucesso.");
    }
}