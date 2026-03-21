package orionpay.merchant.infrastructure.adapters.output.email;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import orionpay.merchant.domain.model.ExtratoTransactionDetail;
import orionpay.merchant.domain.service.GetTransactionDetailUseCase;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SendEmailReceiptUseCase {

    private final GetTransactionDetailUseCase getTransactionDetailUseCase;
    private final EmailService emailService;

    public void execute(UUID transactionId, UUID merchantId, String emailDestino) {
        // 1. Busca os dados reais e validados do banco (comprovante)
        ExtratoTransactionDetail detail = getTransactionDetailUseCase.execute(transactionId, merchantId);

        // 2. Dispara o e-mail
        emailService.sendTransactionReceipt(emailDestino, detail);
    }
}
