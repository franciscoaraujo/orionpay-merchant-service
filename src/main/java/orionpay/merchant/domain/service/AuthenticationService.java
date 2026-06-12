package orionpay.merchant.domain.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import orionpay.merchant.config.AuthenticatedUser;
import orionpay.merchant.config.JwtProperties;
import orionpay.merchant.config.JwtService;
import orionpay.merchant.config.JpaUserDetailsService;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.enums.UserRole;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.LoginRequest;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.RefreshTokenRequest;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TokenResponse;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.AuthUserEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.MerchantEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.RefreshTokenEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaAuthUserRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaMerchantRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaRefreshTokenRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JpaUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final JpaAuthUserRepository authUserRepository;
    private final JpaRefreshTokenRepository refreshTokenRepository;
    private final JpaMerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional
    public void registerMerchantUser(String email, String rawPassword, UUID merchantId) {
        if (authUserRepository.existsByEmailIgnoreCase(email)) {
            throw new DomainException("Já existe usuário com este e-mail.", "USER_ALREADY_EXISTS");
        }

        AuthUserEntity user = new AuthUserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail(email.trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.ROLE_MERCHANT);
        user.setMerchantId(merchantId);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        authUserRepository.save(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String normalizedEmail = request.email().trim().toLowerCase();
        String ipAddress = resolveIp(httpRequest);
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );
            AuthenticatedUser user = (AuthenticatedUser) userDetailsService.loadUserByUsername(normalizedEmail);
            revokeActiveRefreshTokens(user.getUserId());
            TokenResponse response = issueTokens(user);
            auditLogService.logLoginAttempt(normalizedEmail, "LOGIN_SUCCESS", "Autenticação realizada com sucesso", ipAddress);
            return response;
        } catch (BadCredentialsException ex) {
            auditLogService.logLoginAttempt(normalizedEmail, "LOGIN_FAILURE", "Credenciais inválidas", ipAddress);
            throw ex;
        }
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        RefreshTokenEntity storedToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BadCredentialsException("Refresh token inválido"));

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Refresh token expirado ou revogado");
        }

        AuthenticatedUser user = (AuthenticatedUser) userDetailsService.loadUserByUsername(
                authUserRepository.findById(storedToken.getUserId())
                        .orElseThrow(() -> new BadCredentialsException("Usuário do refresh token não encontrado"))
                        .getEmail()
        );

        if (!jwtService.isTokenValid(request.refreshToken(), user, "refresh")) {
            throw new BadCredentialsException("Refresh token inválido");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);
        return issueTokens(user);
    }

    public UUID resolveMerchantIdByEmail(String email) {
        return merchantRepository.findByEmailIgnoreCase(email)
                .map(MerchantEntity::getId)
                .orElse(null);
    }

    private TokenResponse issueTokens(AuthenticatedUser user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        RefreshTokenEntity refreshEntity = new RefreshTokenEntity();
        refreshEntity.setId(UUID.randomUUID());
        refreshEntity.setToken(refreshToken);
        refreshEntity.setUserId(user.getUserId());
        refreshEntity.setRevoked(false);
        refreshEntity.setCreatedAt(LocalDateTime.now());
        refreshEntity.setExpiresAt(LocalDateTime.now().plusDays(jwtProperties.refreshTokenExpirationDays()));
        refreshTokenRepository.save(refreshEntity);

        return new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtProperties.accessTokenExpirationMinutes() * 60,
                jwtProperties.refreshTokenExpirationDays() * 24 * 60 * 60,
                user.getRole().name(),
                user.getMerchantId()
        );
    }

    private void revokeActiveRefreshTokens(UUID userId) {
        refreshTokenRepository.findByUserIdAndRevokedFalse(userId)
                .forEach(token -> token.setRevoked(true));
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

