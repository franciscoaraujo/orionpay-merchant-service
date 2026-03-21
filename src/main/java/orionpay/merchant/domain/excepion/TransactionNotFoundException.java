package orionpay.merchant.domain.excepion;

import java.util.UUID;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(UUID id) {
        super("Transação não encontrada com o ID: " + id);
    }

    public TransactionNotFoundException(String message) {
        super(message);
    }
}