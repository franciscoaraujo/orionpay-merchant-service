package orionpay.merchant.domain.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal; // Importar BigDecimal
import java.time.Instant;
import java.util.UUID;

public final class Authorization {

    private Authorization() {} // Utility class

    @Value
    @Builder
    public static class Request {
        UUID id;
        UUID merchantId;
        String terminalId;
        BigDecimal amount; // Alterado de long amountInCents para BigDecimal amount
        int currencyCode;
        String pinBlock;
        String panMasked;
        String cardHolderName;
    }

    @Value
    @Builder
    public static class Result {
        UUID id;
        String responseCode;
        String nsuAcquirer;
        String nsuBrand;
        Instant authorizedAt;
    }
}