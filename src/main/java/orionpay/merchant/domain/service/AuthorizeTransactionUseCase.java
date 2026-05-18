package orionpay.merchant.domain.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import orionpay.merchant.application.ports.output.*;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.IdempotencyResult;
import orionpay.merchant.domain.model.LedgerAccount;
import orionpay.merchant.domain.model.Merchant;
import orionpay.merchant.domain.model.Transaction;
import orionpay.merchant.domain.model.TransactionSource;
import orionpay.merchant.domain.model.enums.EntryType;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionRequest;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionResponse;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.MerchantRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.PricingRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.TransactionRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthorizeTransactionUseCase {

    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentGatewayPort paymentGateway;
    private final PricingRepository pricingRepository;
    private final IdempotencyService idempotencyService;

    @Transactional
    // Invalida o cache do Dashboard para este lojista, pois o saldo e métricas mudaram
    @CacheEvict(value = "dashboard_summary", key = "#request.merchantId")
    public TransactionResponse execute(TransactionRequest request, String idempotencyKey) {
        // 1. Checagem de Idempotência
        IdempotencyResult cachedResult = idempotencyService.checkAndLock(idempotencyKey);
        if (cachedResult != null) {
            if ("SUCCESS".equals(cachedResult.getStatus())) {
                log.info("Retornando resposta idempotente para chave: {}", idempotencyKey);
                return (TransactionResponse) cachedResult.getResponseBody();
            } else {
                throw new DomainException(cachedResult.getErrorMessage(), "IDEMPOTENCY_ERROR");
            }
        }

        try {
            log.info("Iniciando autorização de transação para o merchantId: {} | Valor: {}", request.merchantId(), request.amount());

            // 2. Buscar e Validar Lojista
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

            transaction.setCardInfo(
                    request.cardBrand(),
                    request.cardBin(),
                    request.cardLastFour(),
                    request.cardHolderName()
            );

            log.info("Enviando transação para o Gateway. TransactionId: {}", transaction.getId());
            GatewayAuthorizationResult authResult = paymentGateway.authorize(transaction, request);

            if (!authResult.isApproved()) {
                log.warn("Transação negada pelo Gateway. Motivo: {}", authResult.getErrorMessage());
                transaction.decline(authResult.getErrorMessage());
                transactionRepository.save(transaction);
                idempotencyService.saveError(idempotencyKey, "Transação negada: " + authResult.getErrorMessage());
                throw new DomainException("Transação negada: " + authResult.getErrorMessage());
            }

            transaction.calculateNetValue(pricing.getMdrPercentage());
            transaction.processApproval(authResult.getNsu(), authResult.getAuthCode());

            // 1. Salva a transação principal no banco de dados.
            transactionRepository.save(transaction);
            log.info("Transação autorizada e persistida com sucesso. ID: {} | NSU: {}", transaction.getId(), transaction.getNsu());

            // 2. Publica o evento para o Transactional Outbox.
            TransactionEvent event = TransactionEvent.builder()
                    .id(UUID.randomUUID())
                    .transactionId(transaction.getId())
                    .merchantId(merchant.getId())
                    .amount(transaction.getAmount())
                    .productType(transaction.getProductType())
                    .installments(request.installments())
                    .status(transaction.getStatus())
                    .occurredAt(LocalDateTime.now())
                    .description("Transação autorizada via Outbox")
                    .build();

            eventPublisher.publish(event); // Apenas salva na tabela core.outbox

            // 3. Publica evento de telemetria (não bloqueante)
            telemetryPublisher.publishTelemetry(
                "orionpay-merchant-service",
                "TRANSACTION_AUTHORIZED",
                Map.of(
                    "transactionId", transaction.getId(),
                    "merchantId", transaction.getMerchant().getId(),
                    "amount", transaction.getAmount(),
                    "productType", transaction.getProductType().name()
                )
            );

            // 4. DISPARO DO EVENTO EM TEMPO REAL (ASSÍNCRONO)
            // Esta chamada não bloqueia a thread e retorna imediatamente.
            liveTransactionBroadcastPort.broadcastTransactionSaved(transaction);

            TransactionResponse response = TransactionResponse.fromDomain(transaction, "Transação aprovada.");
            idempotencyService.saveSuccess(idempotencyKey, response);
            return response;

        } catch (Exception e) {
            // Em caso de erro inesperado, libera o lock para permitir retry
            idempotencyService.releaseLock(idempotencyKey);
            log.error("Erro inesperado ao autorizar transação para merchantId: {}", request.merchantId(), e);
            if (!(e instanceof DomainException)) {
                idempotencyService.releaseLock(idempotencyKey);
            }
            throw e;
        }
    }
}
