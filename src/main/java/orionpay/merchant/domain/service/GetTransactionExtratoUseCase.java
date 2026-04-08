package orionpay.merchant.domain.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import orionpay.merchant.domain.model.ExtratoTransaction;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TransactionExtratoResponse; // Importar o DTO
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.TransactionRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetTransactionExtratoUseCase {

    private final TransactionRepository transactionRepository;

    @Transactional
    public Page<TransactionExtratoResponse> execute(UUID merchantId, Pageable pageable) {
        // Adaptação: Assumindo que findCustomExtrato retorna Page<ExtratoTransaction>
        // e precisamos mapear para Page<TransactionExtratoResponse>
        return transactionRepository.findCustomExtrato(merchantId, null, pageable)
                .map(this::mapToDto);
    }

    private TransactionExtratoResponse mapToDto(ExtratoTransaction extrato) {
        return TransactionExtratoResponse.builder()
                .id(extrato.getId())
                .amount(extrato.getAmount())
                .netAmount(extrato.getNetAmount())
                .productType(extrato.getProductType())
                .status(extrato.getStatus())
                .nsu(extrato.getNsu())
                .authCode(extrato.getAuthCode())
                .cardBrand(extrato.getCardBrand())
                .cardLastFour(extrato.getCardLastFour())
                .createdAt(extrato.getCreatedAt())
                .errorMessage(extrato.getErrorMessage())
                .build();
    }
}
