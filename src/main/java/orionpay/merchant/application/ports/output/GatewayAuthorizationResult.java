package orionpay.merchant.application.ports.output;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GatewayAuthorizationResult {
    private final boolean approved;
    private final String nsu;
    private final String authCode;
    private final String errorMessage;

    // Métodos estáticos para facilitar a criação e deixar o código mais legível
    public static GatewayAuthorizationResult success(String nsu, String authCode) {
        return GatewayAuthorizationResult.builder()
                .approved(true)
                .nsu(nsu)
                .authCode(authCode)
                .build();
    }

    public static GatewayAuthorizationResult declined(String errorMessage) {
        return GatewayAuthorizationResult.builder()
                .approved(false)
                .errorMessage(errorMessage)
                .build();
    }
}