package orionpay.merchant.application.ports.output;

import orionpay.merchant.domain.model.Transaction;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionRequest;

public interface PaymentGatewayPort {
    /**
     * Envia a transação para o autorizador externo (Adquirente/Bandeira).
     */
    GatewayAuthorizationResult authorize(Transaction transaction, TransactionRequest request);

    /**
     * Solicita o estorno de uma transação aprovada junto ao gateway.
     */
    boolean refund(Transaction transaction, String reason);
}
