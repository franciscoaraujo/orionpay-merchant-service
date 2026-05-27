package orionpay.merchant.domain.model;

import lombok.Builder;
import lombok.Value;

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
        long amountInCents;
        int currencyCode;
        String pinBlock;
        String panMasked;
        String cardHolderName; // <-- CAMPO ADICIONADO
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
