package orionpay.merchant.domain.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orionpay.merchant.application.ports.output.PaymentServicePort;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.LedgerAccount;
import orionpay.merchant.domain.model.enums.EntryType;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.WithdrawRequest;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.PayoutEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaPayoutRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.LedgerRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawMoneyUseCase {

    private final LedgerRepository ledgerRepository;
    private final JpaPayoutRepository payoutRepository;
    private final PaymentServicePort paymentService;

    @Transactional
    public void execute(WithdrawRequest request) {
        log.info("Iniciando solicitação de saque para merchantId: {} | Valor: {}", request.merchantId(), request.amount());

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("O valor do saque deve ser positivo.");
        }

        // ---------------------------------------------------------------------
        // TRAVA DE SAQUE: Validação contra Saldo "Líquido e Certo"
        // ---------------------------------------------------------------------
        // O método findRealAvailableBalance executa uma query que soma APENAS:
        // 1. Créditos cuja data de disponibilidade (available_at) já passou ou é hoje.
        // 2. Menos TODOS os Débitos (saques anteriores, estornos, taxas).
        // Isso garante que o lojista não saque dinheiro de vendas futuras (D+30).
        BigDecimal realAvailableBalance = ledgerRepository.findRealAvailableBalance(request.merchantId());
        
        log.debug("Saldo Real Disponível: {} | Valor Solicitado: {}", realAvailableBalance, request.amount());

        if (realAvailableBalance.compareTo(request.amount()) < 0) {
            log.warn("Bloqueio de Saque: Saldo insuficiente. Real: {} | Solicitado: {}", realAvailableBalance, request.amount());
            throw new DomainException("Saldo disponível insuficiente para saque imediato (Vendas futuras não incluídas).");
        }

        // Se passou da trava, segue o fluxo normal
        LedgerAccount account = ledgerRepository.findByMerchantId(request.merchantId())
                .orElseThrow(() -> new DomainException("Conta contábil não encontrada."));

        PayoutEntity payout = new PayoutEntity();
        payout.setMerchantId(request.merchantId());
        payout.setAmount(request.amount());
        payout.setPixKey(request.pixKey());
        payout.setStatus(PayoutEntity.PayoutStatus.PENDING);
        
        payoutRepository.save(payout);

        account.debit(request.amount()); 
        ledgerRepository.saveAccount(account);

        ledgerRepository.saveEntry(
                account,
                request.amount(),
                EntryType.DEBIT,
                "Saque Pix - ID: " + payout.getId(),
                payout.getId(),
                LocalDateTime.now() // Débito é imediato
        );

        boolean success = paymentService.processPixPayout(request.pixKey(), request.amount());

        if (success) {
            payout.setStatus(PayoutEntity.PayoutStatus.COMPLETED);
            payout.setCompletedAt(LocalDateTime.now());
            payoutRepository.save(payout);
            log.info("Saque realizado com sucesso. PayoutId: {}", payout.getId());
        } else {
            log.error("Falha no processamento do pagamento externo. Realizando rollback.");
            throw new DomainException("Falha ao processar pagamento externo. Tente novamente.");
        }
    }
}