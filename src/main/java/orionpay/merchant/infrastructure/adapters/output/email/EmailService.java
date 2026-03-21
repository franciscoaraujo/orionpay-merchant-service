package orionpay.merchant.infrastructure.adapters.output.email;

import orionpay.merchant.domain.model.ExtratoTransactionDetail;

public interface EmailService {

    void sendTransactionReceipt(String to, ExtratoTransactionDetail receipt);
}
