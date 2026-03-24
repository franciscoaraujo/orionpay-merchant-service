package orionpay.merchant.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.IdempotencyResult;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY_PREFIX = "idemp:";
    private static final Duration TTL = Duration.ofHours(24);

    public IdempotencyResult checkAndLock(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }

        String redisKey = KEY_PREFIX + idempotencyKey;

        IdempotencyResult initialResult = new IdempotencyResult("PROCESSING", null, null);
        Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKey, initialResult, TTL);

        if (Boolean.TRUE.equals(success)) {
            log.info("Chave de idempotência bloqueada: {}", idempotencyKey);
            return null;
        }

        IdempotencyResult existing = (IdempotencyResult) redisTemplate.opsForValue().get(redisKey);
        
        if (existing == null) {
            return null; 
        }

        if ("PROCESSING".equals(existing.getStatus())) {
            throw new DomainException("Esta requisição já está sendo processada.", "DUPLICATE_REQUEST_PROCESSING");
        }

        log.info("Retornando resposta em cache para chave: {}", idempotencyKey);
        return existing;
    }

    public void saveSuccess(String idempotencyKey, Object responseBody) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;
        
        String redisKey = KEY_PREFIX + idempotencyKey;
        IdempotencyResult result = new IdempotencyResult("SUCCESS", responseBody, null);
        redisTemplate.opsForValue().set(redisKey, result, TTL);
    }

    public void saveError(String idempotencyKey, String errorMessage) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;

        String redisKey = KEY_PREFIX + idempotencyKey;
        IdempotencyResult result = new IdempotencyResult("ERROR", null, errorMessage);
        redisTemplate.opsForValue().set(redisKey, result, TTL);
    }
    
    public void releaseLock(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;
        redisTemplate.delete(KEY_PREFIX + idempotencyKey);
    }
}