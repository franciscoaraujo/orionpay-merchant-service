package orionpay.merchant.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orionpay.merchant.application.ports.output.PaymentGatewayPort;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.LedgerAccount;
import orionpay.merchant.domain.model.Transaction;
import orionpay.merchant.domain.model.enums.EntryType;
import orionpay.merchant.domain.model.enums.TransactionStatus;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.RefundRequest;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.LedgerRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.TransactionRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundTransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final PaymentGatewayPort paymentGateway;

    @CacheEvict(value = "dashboard_summary", key = "#merchantId")
    public void execute(UUID merchantId, RefundRequest request) {
        log.info("Iniciando solicitação de estorno p/ transação: {}", request.transactionId());

        // 1. Busca e valida transação original
        Transaction transaction = transactionRepository.findById(request.transactionId())
                .orElseThrow(() -> new DomainException("Transação original não encontrada.", "TRANSACTION_NOT_FOUND"));

        validateRefundEligibility(transaction, merchantId);

        try {
            // 2. FASE 1: Reserva de Saldo (Transacional)
            // Bloqueia o valor no Ledger antes de chamar o banco
            reserveRefundBalance(transaction);

            // 3. FASE 2: Chamada ao Gateway Bancário (Fora da transação longa)
            log.info("Invocando Gateway para estorno no cartão. Ref: {}", transaction.getNsu());
            boolean gatewaySuccess = paymentGateway.refund(transaction, request.reason());

            // 4. FASE 3: Finalização (Transacional)
            if (gatewaySuccess) {
                confirmRefund(transaction, request.reason());
            } else {
                revertRefund(transaction, "Gateway recusou a operação.");
            }

        } catch (Exception e) {
            log.error("Erro crítico ao processar estorno {}: {}", transaction.getId(), e.getMessage());
            revertRefund(transaction, e.getMessage());
            throw new DomainException("Não foi possível concluir o estorno: " + e.getMessage());
        }
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

        // Valida se há saldo disponível real para cobrir o estorno
        var available = ledgerRepository.findRealAvailableBalance(merchantId);
        if (available.compareTo(transaction.getAmount()) < 0) {
            throw new DomainException("Saldo insuficiente para realizar o estorno.");
        }
    }

    @Transactional
    public void reserveRefundBalance(Transaction transaction) {
        LedgerAccount account = ledgerRepository.findByMerchantId(transaction.getMerchant().getId())
                .orElseThrow(() -> new DomainException("Conta contábil não encontrada."));

        // Bloqueia o saldo (Débito preventivo)
        account.applyEntry(transaction.getAmount(), EntryType.REFUND_HOLD);
        ledgerRepository.saveAccount(account);

        ledgerRepository.saveEntry(
                account,
                transaction.getAmount(),
                EntryType.REFUND_HOLD,
                "Reserva p/ Estorno - Transação: " + transaction.getNsu(),
                transaction.getId(),
                LocalDateTime.now()
        );
    }

    @Transactional
    public void confirmRefund(Transaction transaction, String reason) {
        transaction.setStatus(TransactionStatus.REVERSED);
        transactionRepository.save(transaction);

        LedgerAccount account = ledgerRepository.findByMerchantId(transaction.getMerchant().getId()).get();
        
        ledgerRepository.saveEntry(
                account,
                transaction.getAmount(),
                EntryType.REFUND_DEBIT,
                "Estorno Confirmado: " + reason,
                transaction.getId(),
                LocalDateTime.now()
        );
        log.info("Estorno concluído com sucesso p/ transação {}", transaction.getId());
    }

    @Transactional
    public void revertRefund(Transaction transaction, String reason) {
        LedgerAccount account = ledgerRepository.findByMerchantId(transaction.getMerchant().getId()).get();
        
        // Devolve o saldo (Crédito de reversão)
        account.applyEntry(transaction.getAmount(), EntryType.REFUND_REVERSAL);
        ledgerRepository.saveAccount(account);

        ledgerRepository.saveEntry(
                account,
                transaction.getAmount(),
                EntryType.REFUND_REVERSAL,
                "Reversão de Estorno (Falha na Adquirente): " + reason,
                transaction.getId(),
                LocalDateTime.now()
        );
        log.warn("Estorno cancelado e saldo devolvido p/ transação {}", transaction.getId());
    }
}
