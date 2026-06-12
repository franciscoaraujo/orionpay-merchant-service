package orionpay.merchant.domain.excepion;

/**
 * Exceção lançada quando ocorre uma falha de segurança durante operações de
 * criptografia/descriptografia. Indica dado inválido, chave incorreta ou
 * qualquer violação criptográfica que não deve ser silenciada em produção.
 */
public class EncryptionException extends DomainException {

    public EncryptionException(String message) {
        super(message, "ENCRYPTION_FAILURE");
    }

    public EncryptionException(String message, Throwable cause) {
        super(message, "ENCRYPTION_FAILURE");
        initCause(cause);
    }
}

