package orionpay.merchant.application.ports.output;

import java.math.BigDecimal;

public interface PaymentServicePort {
    /**
     * Processa um pagamento externo (Pix, TED, etc).
     * @param pixKey Chave de destino
     * @param amount Valor a ser transferido
     * @return true se sucesso, false se falha
     */
    boolean processPixPayout(String pixKey, BigDecimal amount);
}