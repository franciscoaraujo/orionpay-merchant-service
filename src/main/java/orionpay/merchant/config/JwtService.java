package orionpay.merchant.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        String encodedSecret = Base64.getEncoder().encodeToString(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(encodedSecret));
    }

    public String generateAccessToken(AuthenticatedUser user) {
        return buildToken(user, "access", Instant.now().plus(jwtProperties.accessTokenExpirationMinutes(), ChronoUnit.MINUTES));
    }

    public String generateRefreshToken(AuthenticatedUser user) {
        return buildToken(user, "refresh", Instant.now().plus(jwtProperties.refreshTokenExpirationDays(), ChronoUnit.DAYS));
    }

    public boolean isTokenValid(String token, AuthenticatedUser user, String expectedType) {
        String username = extractUsername(token);
        String tokenType = extractClaim(token, claims -> claims.get("type", String.class));
        return username.equalsIgnoreCase(user.getUsername())
                && expectedType.equals(tokenType)
                && !isTokenExpired(token);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractMerchantId(String token) {
        String value = extractClaim(token, claims -> claims.get("merchantId", String.class));
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private String buildToken(AuthenticatedUser user, String type, Instant expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("userId", user.getUserId().toString());
        claims.put("type", type);
        if (user.getMerchantId() != null) {
            claims.put("merchantId", user.getMerchantId().toString());
        }

        return Jwts.builder()
                .subject(user.getUsername())
                .claims(claims)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiration))
                .signWith(signingKey)
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
