package orionpay.merchant.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.IdempotencyResult;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY_PREFIX = "idemp:";
    private static final Duration TTL = Duration.ofHours(24);

    /**
     * Tenta iniciar o processamento para uma chave de idempotência.
     * Retorna o resultado anterior se a chave já existir.
     * Retorna NULL se a chave for nova e o bloqueio for adquirido.
     */
    public IdempotencyResult checkAndLock(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null; // Sem chave, sem idempotência
        }

        String redisKey = KEY_PREFIX + idempotencyKey;

        // Tenta adquirir o lock atomicamente
        // Se a chave não existir, cria com status "PROCESSING"
        IdempotencyResult initialResult = new IdempotencyResult("PROCESSING", null, null);
        Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKey, initialResult, TTL);

        if (Boolean.TRUE.equals(success)) {
            // Lock adquirido, pode processar
            log.info("Chave de idempotência bloqueada: {}", idempotencyKey);
            return null;
        }

        // Chave já existe, retorna o valor atual
        IdempotencyResult existing = (IdempotencyResult) redisTemplate.opsForValue().get(redisKey);
        
        if (existing == null) {
            // Caso raro onde expirou entre o setIfAbsent e o get
            return null; 
        }

        if ("PROCESSING".equals(existing.getStatus())) {
            throw new DomainException("Esta requisição já está sendo processada.", "DUPLICATE_REQUEST_PROCESSING");
        }

        log.info("Retornando resposta em cache para chave: {}", idempotencyKey);
        return existing;
    }

    /**
     * Salva o resultado final com sucesso.
     */
    public void saveSuccess(String idempotencyKey, Object responseBody) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;
        
        String redisKey = KEY_PREFIX + idempotencyKey;
        IdempotencyResult result = new IdempotencyResult("SUCCESS", responseBody, null);
        redisTemplate.opsForValue().set(redisKey, result, TTL);
    }

    /**
     * Salva o resultado de erro (para evitar retry infinito de erros de negócio).
     * Se for erro de sistema (timeout), talvez queiramos deletar a chave para permitir retry.
     * Aqui assumimos que erros de domínio são finais.
     */
    public void saveError(String idempotencyKey, String errorMessage) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;

        String redisKey = KEY_PREFIX + idempotencyKey;
        IdempotencyResult result = new IdempotencyResult("ERROR", null, errorMessage);
        redisTemplate.opsForValue().set(redisKey, result, TTL);
    }
    
    /**
     * Libera a chave em caso de erro transiente/inesperado para permitir retry.
     */
    public void releaseLock(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;
        redisTemplate.delete(KEY_PREFIX + idempotencyKey);
    }
}