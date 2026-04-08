package orionpay.merchant.infrastructure.adapters.output.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orionpay.merchant.application.ports.output.PaymentServicePort;

import java.math.BigDecimal;

@Slf4j
@Service
public class MockPaymentServiceAdapter implements PaymentServicePort {

    @Override
    public boolean processPixPayout(String pixKey, BigDecimal amount) {
        log.info(">>> [MOCK PIX SERVICE] Iniciando transferência de R$ {} para chave: {}", amount, pixKey);
        
        // Simulação de sucesso imediato
        log.info(">>> [MOCK PIX SERVICE] Pix realizado com sucesso!");
        return true;
    }
}
