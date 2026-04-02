package orionpay.merchant.domain.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import orionpay.merchant.application.ports.output.EventPublisherPort;
import orionpay.merchant.application.ports.output.GatewayAuthorizationResult;
import orionpay.merchant.application.ports.output.PaymentGatewayPort;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.*;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionRequest;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionResponse;
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
    private final PaymentGatewayPort paymentGateway;
    private final PricingRepository pricingRepository;
    private final IdempotencyService idempotencyService;
    private final EventPublisherPort eventPublisher; // CORRIGIDO: Injeta a porta e não o RabbitTemplate

    @Transactional
    @CacheEvict(value = "dashboard_summary", allEntries = true) 
    public TransactionResponse execute(TransactionRequest request, String idempotencyKey) {
        IdempotencyResult cachedResult = idempotencyService.checkAndLock(idempotencyKey);
        if (cachedResult != null) {
            if ("SUCCESS".equals(cachedResult.getStatus())) {
                log.info("Requisição de autorização idempotente (já processada). Chave: {}", idempotencyKey);
                return (TransactionResponse) cachedResult.getResponseBody();
            } else {
                throw new DomainException(cachedResult.getErrorMessage(), "IDEMPOTENCY_ERROR");
            }
        }

        try {
            log.info("Iniciando autorização de transação para o merchantId: {} | Valor: {}", request.merchantId(), request.amount());

            Merchant merchant = merchantRepository.findById(request.merchantId())
                    .orElseThrow(() -> new DomainException("Lojista não encontrado.", "MERCHANT_NOT_FOUND"));

            var pricing = pricingRepository.findCurrentPricing(merchant.getId(), request.productType())
                    .orElseThrow(() -> new DomainException("Lojista sem configuração de taxas para " + request.productType()));

            Transaction transaction = new Transaction(
                    UUID.randomUUID(),
                    merchant,
                    request.amount(),
                    request.productType(),
                    new TransactionSource(request.terminalSn(), "v1.0", request.entryMode())
            );

            transaction.setCardInfo(request.cardBrand(), request.cardBin(), request.cardLastFour(), request.cardHolderName());

            log.info("Enviando transação para o Gateway. TransactionId: {}", transaction.getId());
            GatewayAuthorizationResult authResult = paymentGateway.authorize(transaction, request);

            if (!authResult.isApproved()) {
                log.warn("Transação negada pelo Gateway. Motivo: {}", authResult.getErrorMessage());
                transaction.decline(authResult.getErrorMessage());
                transactionRepository.save(transaction);
                
                String errorMsg = "Transação negada: " + authResult.getErrorMessage();
                idempotencyService.saveError(idempotencyKey, errorMsg);
                throw new DomainException(errorMsg);
            }

            transaction.calculateNetValue(pricing.getMdrPercentage());
            transaction.processApproval(authResult.getNsu(), authResult.getAuthCode());

            transactionRepository.save(transaction);
            log.info("Transação autorizada e persistida com sucesso. ID: {} | NSU: {}", transaction.getId(), transaction.getNsu());

            // 7. Publicar Evento via OUTBOX (Garante que se salvou a venda, o evento será enviado)
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

            TransactionResponse response = TransactionResponse.fromDomain(transaction, "Transação aprovada.");
            idempotencyService.saveSuccess(idempotencyKey, response);
            return response;

        } catch (Exception e) {
            log.error("Erro inesperado ao autorizar transação para merchantId: {}", request.merchantId(), e);
            if (!(e instanceof DomainException)) {
                idempotencyService.releaseLock(idempotencyKey);
            }
            throw e;
        }
    }
}
