package orionpay.merchant.domain.excepion;

import orionpay.merchant.domain.model.enums.MerchantStatus;

// Exemplo: Erro de validação de documento
public class InvalidDocumentException extends DomainException {
    public InvalidDocumentException(String document) {
        super("O documento " + document + " não é um CPF ou CNPJ válido.", "INVALID_DOCUMENT");
    }
}

