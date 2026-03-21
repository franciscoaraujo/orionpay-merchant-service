package orionpay.merchant.domain.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import orionpay.merchant.application.ports.output.GatewayAuthorizationResult;
import orionpay.merchant.application.ports.output.PaymentGatewayPort;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.LedgerAccount;
import orionpay.merchant.domain.model.Merchant;
import orionpay.merchant.domain.model.Transaction;
import orionpay.merchant.domain.model.TransactionSource;
import orionpay.merchant.domain.model.enums.EntryType;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionRequest;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionResponse;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.LedgerRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.MerchantRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.PricingRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.TransactionRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthorizeTransactionUseCase {

    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final PaymentGatewayPort paymentGateway;
    private final PricingRepository pricingRepository;

    @Transactional
    public TransactionResponse execute(TransactionRequest request) {
        log.info("Iniciando autorização de transação para o merchantId: {} | Valor: {}", request.merchantId(), request.amount());

        try {
            // 1. Buscar e Validar Lojista
            Merchant merchant = merchantRepository.findById(request.merchantId())
                    .orElseThrow(() -> {
                        log.warn("Merchant não encontrado: {}", request.merchantId());
                        return new DomainException("Lojista não encontrado.", "MERCHANT_NOT_FOUND");
                    });

            // --- NOVA VALIDAÇÃO: Buscar Precificação antes de autorizar ---
            log.debug("Buscando precificação para merchantId: {} e produto: {}", merchant.getId(), request.productType());
            var pricing = pricingRepository.findCurrentPricing(merchant.getId(), request.productType())
                    .orElseThrow(() -> {
                        log.error("Precificação não encontrada para merchantId: {} e produto: {}", merchant.getId(), request.productType());
                        return new DomainException("Lojista sem configuração de taxas para " + request.productType());
                    });

            // 2. Criar Transação
            Transaction transaction = new Transaction(
                    UUID.randomUUID(),
                    merchant,
                    request.amount(),
                    request.productType(),
                    new TransactionSource(request.terminalSn(), "v1.0", request.entryMode())
            );

            // 3. Preencher dados seguros
            transaction.setCardInfo(request.cardBrand(), request.cardBin(), request.cardLastFour(), request.cardHolderName());

            // 4. Gateway (Rede)
            log.info("Enviando transação para o Gateway. TransactionId: {}", transaction.getId());
            GatewayAuthorizationResult authResult = paymentGateway.authorize(transaction, request);

            // 5. Processar Resposta
            if (!authResult.isApproved()) {
                log.warn("Transação negada pelo Gateway. Motivo: {}", authResult.getErrorMessage());
                transaction.decline(authResult.getErrorMessage());
                transactionRepository.save(transaction);
                throw new DomainException("Transação negada: " + authResult.getErrorMessage());
            }

            // --- CÁLCULO DO VALOR LÍQUIDO ---
            log.debug("Calculando valor líquido com taxa: {}%", pricing.getMdrPercentage());
            transaction.calculateNetValue(pricing.getMdrPercentage());

            transaction.processApproval(authResult.getNsu(), authResult.getAuthCode());

            // 6. Persistir Transação
            transactionRepository.save(transaction);
            log.info("Transação autorizada e persistida com sucesso. ID: {} | NSU: {}", transaction.getId(), transaction.getNsu());

            // 7. Movimentar Financeiro
            if (transaction.getStatus().isEligibleForSettlement()) {
                log.debug("Processando liquidação financeira para conta do merchantId: {}", merchant.getId());
                LedgerAccount account = ledgerRepository.findByMerchantId(merchant.getId())
                        .orElseThrow(() -> {
                            log.error("Conta contábil não encontrada para merchantId: {}", merchant.getId());
                            return new DomainException("Conta contábil não encontrada.");
                        });

                // Lógica de Liquidação (D+1 ou D+30)
                int daysToSettle = request.productType().getSettlementDays();
                LocalDateTime availableAt = LocalDateTime.now().plusDays(daysToSettle);

                // IMPORTANTE: No Ledger, creditamos o valor LÍQUIDO
                account.applyEntry(transaction.getNetAmount(), EntryType.CREDIT);
                ledgerRepository.saveAccount(account);

                ledgerRepository.saveEntry(
                        account,
                        transaction.getNetAmount(),
                        EntryType.CREDIT,
                        "Venda Cartão - NSU: " + transaction.getNsu(),
                        transaction.getId(),
                        availableAt // Data de liquidação calculada
                );
                log.info("Movimentação financeira realizada. Disponível em: {}", availableAt);
            }

            return TransactionResponse.fromDomain(transaction, "Transação aprovada.");

        } catch (Exception e) {
            log.error("Erro inesperado ao autorizar transação para merchantId: {}", request.merchantId(), e);
            throw e;
        }
    }
}