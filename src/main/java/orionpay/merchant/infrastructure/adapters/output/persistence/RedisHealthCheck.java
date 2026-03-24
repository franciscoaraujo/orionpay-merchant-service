package orionpay.merchant.infrastructure.adapters.output.persistence;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthCheck {

    private final RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    public void checkRedisConnection() {
        try {
            log.info("Testando conexão com Redis...");
            
            String key = "health_check_test";
            String value = "Redis está UP! " + LocalDateTime.now();
            
            // Tenta escrever
            redisTemplate.opsForValue().set(key, value);
            log.info("Escrita no Redis: OK");
            
            // Tenta ler
            Object result = redisTemplate.opsForValue().get(key);
            log.info("Leitura no Redis: OK. Valor recuperado: {}", result);
            
            // Tenta deletar
            redisTemplate.delete(key);
            log.info("Deleção no Redis: OK");
            
            log.info("✅ Conexão com Redis estabelecida com sucesso!");
            
        } catch (Exception e) {
            log.error("❌ FALHA ao conectar no Redis: {}", e.getMessage());
            // Não relançamos a exceção para não impedir o boot da aplicação se o Redis for opcional,
            // mas no nosso caso ele é crítico para performance e idempotência.
        }
    }
}