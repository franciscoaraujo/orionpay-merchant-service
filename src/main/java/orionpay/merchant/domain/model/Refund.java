package orionpay.merchant.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

public final class Refund {

    private Refund() {} // Utility class

    @Value
    @Builder
    public static class Request {
        UUID originalTransactionId;
        UUID merchantId;
        long amountInCents;
        String reason; // <-- CAMPO ADICIONADO
    }

    @Value
    @Builder
    public static class Result {
        UUID refundId;
        UUID originalTransactionId;
        String responseCode;
        String nsuAcquirer;
        Instant refundedAt;
    }
}
