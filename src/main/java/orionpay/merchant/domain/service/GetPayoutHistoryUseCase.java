package orionpay.merchant.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.PayoutHistoryResponse;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.PayoutEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaPayoutRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetPayoutHistoryUseCase {

    private final JpaPayoutRepository payoutRepository;

    public Page<PayoutHistoryResponse> execute(UUID merchantId, Pageable pageable) {
        Page<PayoutEntity> payouts = payoutRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, pageable);
        return payouts.map(this::mapToDto);
    }

    private PayoutHistoryResponse mapToDto(PayoutEntity entity) {
        // Mapeamento semântico p/ o Frontend (Status compatível com as Cores da Interface)
        String statusLabel = switch (entity.getStatus()) {
            case COMPLETED, SUCCESS -> "CONCLUIDO"; // Verde
            case FAILED -> "FALHOU"; // Vermelho
            case WAITING_BANK_CONFIRMATION, PENDING, PROCESSING -> "PROCESSANDO"; // Amarelo
        };

        return PayoutHistoryResponse.builder()
                .id(entity.getId())
                .amount(entity.getAmount())
                .pixKey(maskPixKey(entity.getPixKey()))
                .status(statusLabel)
                .createdAt(entity.getCreatedAt())
                .errorMessage(entity.getStatus() == PayoutEntity.PayoutStatus.FAILED ? "O banco recusou o pagamento." : null)
                .build();
    }

    private String maskPixKey(String pixKey) {
        if (pixKey == null || pixKey.length() < 5) return "***";
        // Mascaramento corporativo: mostra os primeiros 3 e os últimos 2
        return pixKey.substring(0, 3) + "********" + pixKey.substring(pixKey.length() - 2);
    }
}
