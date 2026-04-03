package orionpay.merchant.infrastructure.adapters.output.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import orionpay.merchant.application.ports.output.PaymentServicePort;

import java.math.BigDecimal;

@Slf4j
@Component
public class MockPaymentServiceAdapter implements PaymentServicePort {

    @Override
    public boolean processPixPayout(String pixKey, BigDecimal amount) {
        log.info(">>> [SIMULAÇÃO] Iniciando transferência Pix para chave: {}", pixKey);
        
        // Simulação de latência de rede bancária
        try {
            Thread.sleep(50000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulação: Valores acima de 1 milhão falham (regra de teste)
        if (amount.compareTo(new BigDecimal("1000000")) > 0) {
            log.error(">>> [SIMULAÇÃO] Falha no Pix: Valor excede limite de simulação.");
            return false;
        }

        log.info(">>> [SIMULAÇÃO] Pix de R$ {} realizado com sucesso!", amount);
        return true; // Sucesso
    }
}