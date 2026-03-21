package orionpay.merchant.domain.service;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import orionpay.merchant.domain.model.ExtratoTransaction;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.TransactionRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetTransactionExtratoUseCase {

    private final TransactionRepository transactionRepository;

    @Transactional
    public Page<ExtratoTransaction> execute(UUID merchantId, String search, Pageable pageable) {
        return transactionRepository.findCustomExtrato(merchantId, search, pageable);
    }
}
