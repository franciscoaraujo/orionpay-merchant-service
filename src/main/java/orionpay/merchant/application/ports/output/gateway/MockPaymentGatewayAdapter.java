package orionpay.merchant.application.ports.output.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orionpay.merchant.application.ports.output.GatewayAuthorizationResult;
import orionpay.merchant.application.ports.output.PaymentGatewayPort;
import orionpay.merchant.domain.model.Transaction;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionRequest;

import java.util.UUID;

@Slf4j
@Service
public class MockPaymentGatewayAdapter implements PaymentGatewayPort {

    @Override
    public GatewayAuthorizationResult authorize(Transaction transaction, TransactionRequest request) {
        log.info(">>> [MOCK GATEWAY] Autorizando transação de R$ {} p/ Cartão final {}", 
                transaction.getAmount(), transaction.getCardLastFour());
        
        // Simulação: Aprovamos quase tudo, exceto valores redondos de 999.00
        boolean approved = transaction.getAmount().doubleValue() != 999.00;

        return GatewayAuthorizationResult.builder()
                .approved(approved)
                .nsu(approved ? String.valueOf(System.currentTimeMillis()).substring(3) : null)
                .authCode(approved ? "AUTH-" + UUID.randomUUID().toString().substring(0, 6) : null)
                .errorMessage(approved ? null : "Cartão sem limite ou bloqueado")
                .build();
    }

    @Override
    public boolean refund(Transaction transaction, String reason) {
        log.info(">>> [MOCK GATEWAY] Solicitando estorno da transação {} | Motivo: {}", 
                transaction.getId(), reason);
        
        // Simulação: Sucesso imediato
        return true;
    }
}
