package orionpay.merchant.domain.model;

import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.enums.ChargebackStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Chargeback {
    private final UUID id;
    private final UUID transactionId;
    private final BigDecimal amount;
    private ChargebackStatus status;
    private final LocalDateTime settlementDate;
    private String reasonCode;

    public Chargeback(UUID id, UUID transactionId, BigDecimal amount, String reasonCode) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Valor do chargeback deve ser positivo.");
        }
        this.id = id;
        this.transactionId = transactionId;
        this.amount = amount;
        this.reasonCode = reasonCode;
        this.status = ChargebackStatus.OPEN;
        this.settlementDate = LocalDateTime.now();
    }

    // REGRA: Lojista pode apresentar defesa se estiver dentro do prazo (ex: 7 dias)
    public void presentDefense(List<String> evidenceUrls) {
        if (!this.status.canPresentDefense()) {
            throw new DomainException("Não é mais possível apresentar defesa para esta disputa.");
        }

        if (evidenceUrls == null || evidenceUrls.isEmpty()) {
            throw new DomainException("É necessário ao menos um documento comprobatório para a defesa.");
        }

        this.status = ChargebackStatus.UNDER_REVIEW;
    }

    public void closeAsLost() {
        // Regra: Uma disputa vencida não pode ser marcada como perdida depois
        if (this.status == ChargebackStatus.REVERSED) {
            throw new DomainException("Uma disputa já vencida não pode ser encerrada como perdida.");
        }
        this.status = ChargebackStatus.LOST;
    }
}