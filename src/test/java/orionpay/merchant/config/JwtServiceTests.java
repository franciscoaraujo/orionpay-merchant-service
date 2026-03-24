package orionpay.merchant.config;

import org.junit.jupiter.api.Test;
import orionpay.merchant.domain.model.enums.UserRole;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.AuthUserEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTests {

    @Test
    void shouldGenerateAndValidateAccessToken() {
        JwtProperties properties = new JwtProperties("orionpay-super-secret-key-change-me-orionpay-super-secret-key-change-me", 15, 7);
        JwtService jwtService = new JwtService(properties);

        AuthUserEntity entity = new AuthUserEntity();
        entity.setId(UUID.randomUUID());
        entity.setEmail("merchant@orionpay.com");
        entity.setPasswordHash("hash");
        entity.setRole(UserRole.ROLE_MERCHANT);
        entity.setMerchantId(UUID.randomUUID());
        entity.setEnabled(true);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        AuthenticatedUser user = new AuthenticatedUser(entity);
        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertEquals(user.getUsername(), jwtService.extractUsername(token));
        assertEquals(user.getRole().name(), jwtService.extractRole(token));
        assertEquals(user.getMerchantId(), jwtService.extractMerchantId(token));
        assertTrue(jwtService.isTokenValid(token, user, "access"));
        assertFalse(jwtService.isTokenValid(token, user, "refresh"));
    }
}

