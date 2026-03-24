package orionpay.merchant.infrastructure.adapters.input.rest.dto;

import java.util.UUID;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresInSeconds,
        long refreshTokenExpiresInSeconds,
        String role,
        UUID merchantId
) {
}

