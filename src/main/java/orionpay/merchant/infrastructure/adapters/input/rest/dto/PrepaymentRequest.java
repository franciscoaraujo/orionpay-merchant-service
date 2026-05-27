package orionpay.merchant.infrastructure.adapters.input.rest.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PrepaymentRequest {
    private List<UUID> settlementIds;
}
