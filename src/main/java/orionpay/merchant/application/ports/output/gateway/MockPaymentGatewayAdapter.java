package orionpay.merchant.application.ports.output.gateway;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import orionpay.merchant.application.ports.output.GatewayAuthorizationResult;
import orionpay.merchant.application.ports.output.PaymentGatewayPort;
import orionpay.merchant.domain.model.Transaction;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionRequest;

import java.math.BigDecimal;
import java.util.UUID;

@Log4j2
@Component
public class MockPaymentGatewayAdapter implements PaymentGatewayPort {

    @Override
    @Retry(name = "adquirenteService")
    @CircuitBreaker(name = "adquirenteService", fallbackMethod = "authorizeFallback")
    public GatewayAuthorizationResult authorize(Transaction transaction, TransactionRequest request) {
        log.info("[GATEWAY MOCK] Iniciando comunicação com a rede para transação: {}", transaction.getId());

        // 1. Simulando a latência da rede (800 milissegundos)
        try {
            // Simulação de erro intermitente para testar o retry/circuit breaker
            if (Math.random() > 0.8) {
                log.error("[GATEWAY MOCK] Simulação de erro de timeout!");
                throw new java.util.concurrent.TimeoutException("Timeout na comunicação com a adquirente");
            }
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (java.util.concurrent.TimeoutException e) {
             throw new RuntimeException(e); // Resilience4j needs a RuntimeException or configured checked exception
        }

        // 2. Simulando uma regra de negócio externa
        if (transaction.getAmount().compareTo(new BigDecimal("10000.00")) > 0) {
            log.warn("[GATEWAY MOCK] Transação negada: Valor excede o limite simulado.");
            return GatewayAuthorizationResult.declined("51 - Saldo insuficiente ou limite excedido.");
        }

        // 3. Simulando sucesso
        String mockNsu = String.valueOf(System.currentTimeMillis()).substring(3);
        String mockAuthCode = "AUTH" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();

        log.info("[GATEWAY MOCK] Transação APROVADA. NSU: {}, AuthCode: {}", mockNsu, mockAuthCode);

        return GatewayAuthorizationResult.success(mockNsu, mockAuthCode);
    }

    /**
     * Fallback para quando o circuito estiver aberto ou falhar após as retentativas.
     */
    public GatewayAuthorizationResult authorizeFallback(Transaction transaction, TransactionRequest request, Throwable t) {
        log.error("[GATEWAY FALLBACK] Circuito aberto ou erro crítico para transação: {}. Motivo: {}", transaction.getId(), t.getMessage());
        return GatewayAuthorizationResult.declined("99 - Sistema Adquirente Indisponível (Circuit Breaker)");
    }
}
