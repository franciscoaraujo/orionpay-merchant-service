package orionpay.merchant.infrastructure.adapters.output.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orionpay.merchant.application.ports.output.PaymentServicePort;

import java.math.BigDecimal;

@Slf4j
@Service
public class PixPaymentServiceAdapter implements PaymentServicePort {

    @Override
    public boolean processPixPayout(String pixKey, BigDecimal amount) {
        // Lógica de simulação (mock)
        log.info(">>> [MOCK PIX SERVICE] Iniciando transferência de R$ {} para a chave PIX: {}", amount, pixKey);
        
        // Simula uma chamada a um provedor de serviços de pagamento (PSP) real.
        // Em um cenário real, aqui você usaria um cliente HTTP (como RestTemplate ou WebClient)
        // para se comunicar com a API do seu parceiro de pagamentos PIX.
        
        // Para fins de teste, vamos simular sucesso para a maioria dos casos.
        // Podemos simular uma falha para um valor específico para testar o fluxo de erro.
        if (amount.compareTo(new BigDecimal("999.99")) == 0) {
            log.warn(">>> [MOCK PIX SERVICE] Simulação de falha para o valor 999.99.");
            return false;
        }

        log.info(">>> [MOCK PIX SERVICE] Transferência PIX para {} realizada com sucesso!", pixKey);
        return true;
    }
}
