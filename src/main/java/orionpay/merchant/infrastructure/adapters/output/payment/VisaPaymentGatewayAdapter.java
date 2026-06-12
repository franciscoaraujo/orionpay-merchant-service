package orionpay.merchant.infrastructure.adapters.output.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import orionpay.merchant.application.ports.output.GatewayAuthorizationResult;
import orionpay.merchant.application.ports.output.PaymentGatewayPort;
import orionpay.merchant.domain.model.Transaction;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionRequest;

import java.util.UUID;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class VisaPaymentGatewayAdapter implements PaymentGatewayPort {

    private final VisaGatewayClientService visaGatewayClientService;

    @Override
    public GatewayAuthorizationResult authorize(Transaction transaction, TransactionRequest request) {
        log.info(">>> [VISA GATEWAY] Autorizando transação de R$ {} p/ Cartão final {}",
                transaction.getAmount(), transaction.getCardLastFour());

        String pan = request.cardNumber();
        String terminalId = request.terminalSn(); 
        String merchantId = transaction.getMerchant().getId().toString();
        
        String responseCode = visaGatewayClientService.authorizeTransaction(pan, transaction.getAmount(), terminalId, merchantId);

        boolean approved = "APPROVED".equals(responseCode);

        return GatewayAuthorizationResult.builder()
                .approved(approved)
                .nsu(approved ? String.valueOf(System.currentTimeMillis()).substring(3) : null)
                .authCode(approved ? "AUTH-" + UUID.randomUUID().toString().substring(0, 6) : null)
                .errorMessage(approved ? null : getErrorMessage(responseCode))
                .build();
    }

    @Override
    public boolean refund(Transaction transaction, String reason) {
        log.info(">>> [VISA GATEWAY] Solicitando estorno da transação {} | Motivo: {}", 
                transaction.getId(), reason);
        // A implementação do estorno não faz parte desta correção.
        return true; 
    }
    
    private String getErrorMessage(String code) {
        switch (code) {
             case "INVALID_TRANSACTION": return "Transação Inválida";
             case "INSUFFICIENT_FUNDS": return "Saldo insuficiente";
             case "INVALID_CARD": return "Cartão inválido";
             case "INCORRECT_PIN": return "Senha incorreta";
             case "ISSUER_INOPERATIVE": return "Emissor inoperante";
             default: return "Erro desconhecido: " + code;
        }
    }
}
