package orionpay.merchant.infrastructure.adapters.input.rest.dto;

import java.util.List;
import java.util.UUID;

public record AnticipationRequest(List<UUID> settlementIds) {}
