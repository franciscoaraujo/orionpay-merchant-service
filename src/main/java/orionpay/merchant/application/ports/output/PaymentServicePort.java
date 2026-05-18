package orionpay.merchant.application.ports.output;

import java.math.BigDecimal;

public interface PaymentServicePort {

    /**
     * Processa um pagamento de saque (payout) via PIX.
     *
     * @param pixKey A chave PIX de destino.
     * @param amount O valor a ser transferido.
     * @return true se o pagamento foi iniciado com sucesso, false caso contrário.
     */
    boolean processPixPayout(String pixKey, BigDecimal amount);
}
