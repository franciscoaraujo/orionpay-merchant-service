package orionpay.merchant.application.ports.output;

import orionpay.merchant.domain.model.Authorization;
import orionpay.merchant.domain.model.Refund;

import java.util.Optional;

public interface AcquiringSwitchPort {
    Optional<Authorization.Result> authorizeTransaction(Authorization.Request domainRequest);
    Optional<Refund.Result> refundTransaction(Refund.Request domainRequest);
}
