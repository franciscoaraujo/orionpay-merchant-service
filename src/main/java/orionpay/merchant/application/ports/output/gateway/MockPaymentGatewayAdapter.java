package orionpay.merchant.application.ports.output.gateway;

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
    public GatewayAuthorizationResult authorize(Transaction transaction, TransactionRequest request) {
        log.info("[GATEWAY MOCK] Iniciando comunicação com a rede para transação: {}", transaction.getId());

        // 1. Simulando a latência da rede (800 milissegundos)
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 2. Simulando uma regra de negócio externa (Ex: Compra negada por falta de limite)
        // Se o valor for maior que R$ 10.000,00, o gateway nega automaticamente.
        if (transaction.getAmount().compareTo(new BigDecimal("10000.00")) > 0) {
            log.warn("[GATEWAY MOCK] Transação negada: Valor excede o limite simulado.");
            return GatewayAuthorizationResult.declined("51 - Saldo insuficiente ou limite excedido.");
        }

        // 3. Simulando sucesso (Gerando NSU e AuthCode amigáveis)
        String mockNsu = String.valueOf(System.currentTimeMillis()).substring(3); // Gera um número de 10 dígitos
        String mockAuthCode = "AUTH" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();

        log.info("[GATEWAY MOCK] Transação APROVADA. NSU: {}, AuthCode: {}", mockNsu, mockAuthCode);

        return GatewayAuthorizationResult.success(mockNsu, mockAuthCode);
    }
}
