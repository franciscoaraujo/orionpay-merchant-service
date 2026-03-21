package orionpay.merchant.domain.excepion;

import orionpay.merchant.domain.model.enums.MerchantStatus;

// Exemplo: Erro de transição de status
public class IllegalStatusTransitionException extends DomainException {
    public IllegalStatusTransitionException(MerchantStatus from, MerchantStatus to) {
        super("Não é permitido alterar o status de " + from + " para " + to, "INVALID_STATUS_TRANSITION");
    }
}
