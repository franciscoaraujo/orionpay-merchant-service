package orionpay.merchant.application.ports.output;

import orionpay.merchant.domain.model.TransactionEvent;

public interface EventPublisherPort {
    void publish(TransactionEvent event);
}
