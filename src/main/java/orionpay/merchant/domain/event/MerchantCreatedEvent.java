package orionpay.merchant.domain.event;

import java.util.UUID;

// Em DDD, eventos são sempre nomeados no passado (O que aconteceu?)
public record MerchantCreatedEvent(
        UUID merchantId,
        String document,
        String businessName
) {}