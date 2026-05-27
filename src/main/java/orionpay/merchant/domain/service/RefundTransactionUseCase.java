package orionpay.merchant.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orionpay.merchant.application.ports.output.AcquiringSwitchPort;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.LedgerAccount;
import orionpay.merchant.domain.model.Refund;
import orionpay.merchant.domain.model.Transaction;
import orionpay.merchant.domain.model.enums.EntryType;
import orionpay.merchant.domain.model.enums.TransactionStatus;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.RefundRequest;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.LedgerRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundTransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final AcquiringSwitchPort acquiringSwitch;

    @CacheEvict(value = "dashboard_summary", key = "#merchantId")
    public void execute(UUID merchantId, RefundRequest request) {
        log.info("Iniciando solicitação de estorno p/ transação: {}", request.transactionId());

        Transaction transaction = transactionRepository.findById(request.transactionId())
                .orElseThrow(() -> new DomainException("Transação original não encontrada.", "TRANSACTION_NOT_FOUND"));

        validateRefundEligibility(transaction, merchantId);

        // A lógica de estorno foi desativada no contrato gRPC atual.
        // Lançamos uma exceção para indicar que a operação não é suportada.
        throw new UnsupportedOperationException("A funcionalidade de estorno não está implementada no contrato gRPC atual.");
        
        /*
        // CÓDIGO ANTIGO DESATIVADO
        try {
            reserveRefundBalance(transaction);

            log.info("Invocando Switch gRPC para estorno. Ref: {}", transaction.getNsu());
            
            Refund.Request refundRequest = toRefundRequest(transaction, request.reason());
            Optional<Refund.Result> refundResultOpt = acquiringSwitch.refundTransaction(refundRequest);

            boolean gatewaySuccess = refundResultOpt.isPresent() && "00".equals(refundResultOpt.get().getResponseCode());

            if (gatewaySuccess) {
                confirmRefund(transaction, request.reason());
            } else {
                String reason = refundResultOpt.map(r -> "Código: " + r.getResponseCode()).orElse("Falha de comunicação");
                revertRefund(transaction, "Gateway recusou a operação. " + reason);
                throw new DomainException("Gateway de pagamento recusou o estorno. " + reason);
            }

        } catch (Exception e) {
            log.error("Erro crítico ao processar estorno {}: {}", transaction.getId(), e.getMessage());
            if (!(e instanceof DomainException)) {
                 revertRefund(transaction, e.getMessage());
            }
            throw new DomainException("Não foi possível concluir o estorno: " + e.getMessage());
        }
        */
    }

    private void validateRefundEligibility(Transaction transaction, UUID merchantId) {
        if (!transaction.getMerchant().getId().equals(merchantId)) {
            throw new DomainException("Acesso negado: Transação não pertence a este lojista.");
        }
        if (transaction.getStatus() != TransactionStatus.APPROVED) {
            throw new DomainException("Apenas transações APROVADAS podem ser estornadas. Status atual: " + transaction.getStatus());
        }
        
        long daysSinceSale = ChronoUnit.DAYS.between(transaction.getCreatedAt(), LocalDateTime.now());
        if (daysSinceSale > 90) {
            throw new DomainException("Prazo limite para estorno expirado (90 dias).");
        }

        var available = ledgerRepository.findRealAvailableBalance(merchantId);
        if (available.compareTo(transaction.getAmount()) < 0) {
            throw new DomainException("Saldo insuficiente para realizar o estorno.");
        }
    }

    @Transactional
    public void reserveRefundBalance(Transaction transaction) {
        LedgerAccount account = ledgerRepository.findByMerchantId(transaction.getMerchant().getId())
                .orElseThrow(() -> new DomainException("Conta contábil não encontrada."));
        account.applyEntry(transaction.getAmount(), EntryType.REFUND_HOLD);
        ledgerRepository.saveAccount(account);
        ledgerRepository.saveEntry(account, transaction.getAmount(), EntryType.REFUND_HOLD, "Reserva p/ Estorno - Transação: " + transaction.getNsu(), transaction.getId(), LocalDateTime.now());
    }

    @Transactional
    public void confirmRefund(Transaction transaction, String reason) {
        transaction.setStatus(TransactionStatus.REVERSED);
        transactionRepository.save(transaction);
        LedgerAccount account = ledgerRepository.findByMerchantId(transaction.getMerchant().getId()).get();
        ledgerRepository.saveEntry(account, transaction.getAmount(), EntryType.REFUND_DEBIT, "Estorno Confirmado: " + reason, transaction.getId(), LocalDateTime.now());
        log.info("Estorno concluído com sucesso p/ transação {}", transaction.getId());
    }

    @Transactional
    public void revertRefund(Transaction transaction, String reason) {
        LedgerAccount account = ledgerRepository.findByMerchantId(transaction.getMerchant().getId()).get();
        account.applyEntry(transaction.getAmount(), EntryType.REFUND_REVERSAL);
        ledgerRepository.saveAccount(account);
        ledgerRepository.saveEntry(account, transaction.getAmount(), EntryType.REFUND_REVERSAL, "Reversão de Estorno (Falha na Adquirente): " + reason, transaction.getId(), LocalDateTime.now());
        log.warn("Estorno cancelado e saldo devolvido p/ transação {}", transaction.getId());
    }

    private Refund.Request toRefundRequest(Transaction transaction, String reason) {
        long amountInCents = transaction.getAmount().multiply(new BigDecimal("100")).longValue();
        return Refund.Request.builder()
                .originalTransactionId(transaction.getId())
                .merchantId(transaction.getMerchant().getId())
                .amountInCents(amountInCents)
                .reason(reason)
                .build();
    }
}
