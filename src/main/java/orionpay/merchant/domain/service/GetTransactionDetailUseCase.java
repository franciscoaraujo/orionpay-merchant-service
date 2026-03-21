package orionpay.merchant.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import orionpay.merchant.domain.excepion.TransactionNotFoundException;
import orionpay.merchant.domain.model.ExtratoTransactionDetail;
import orionpay.merchant.infrastructure.adapters.output.persistence.mapper.TransactionMapper;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.TransactionRepository;

import java.util.UUID;


@Service
@RequiredArgsConstructor
public class GetTransactionDetailUseCase {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;


    public ExtratoTransactionDetail execute(UUID transactionId, UUID merchantId) {
        return transactionRepository.findByIdAndMerchantId(transactionId, merchantId)
                .map(transactionMapper::toDetailDomain)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }
}
