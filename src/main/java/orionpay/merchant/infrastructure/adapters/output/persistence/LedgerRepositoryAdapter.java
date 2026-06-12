package orionpay.merchant.infrastructure.adapters.output.persistence;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.LedgerAccount;
import orionpay.merchant.domain.model.enums.EntryType;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.JournalEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.LedgerAccountEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.LedgerEntryEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.mapper.LedgerMapper;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.LedgerBalanceProjection;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaJournalRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaLedgerAccountRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaLedgerEntryRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.LedgerRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Component
@RequiredArgsConstructor
public class LedgerRepositoryAdapter implements LedgerRepository {

    private final JpaLedgerAccountRepository jpaLedgerAccountRepository;
    private final JpaLedgerEntryRepository jpaLedgerEntryRepository;
    private final JpaJournalRepository jpaJournalRepository;
    private final LedgerMapper ledgerMapper;

    @Override
    public Optional<LedgerAccount> findByMerchantId(UUID merchantId) {
        return jpaLedgerAccountRepository.findByMerchantId(merchantId).map(ledgerMapper::toDomain);
    }

    @Override
    public void saveAccount(LedgerAccount account) {
        LedgerAccountEntity entity = jpaLedgerAccountRepository
                .findById(account.getAccountId())
                .map(existingEntity -> {
                    ledgerMapper.updateEntityFromDomain(account, existingEntity);
                    return existingEntity;
                })
                .orElseGet(() -> ledgerMapper.toEntity(account));

        jpaLedgerAccountRepository.save(entity);
    }

    @Override
    @Transactional
    public void saveEntry(
            LedgerAccount account,
            BigDecimal amount,
            EntryType type,
            String description,
            UUID correlationId,
            LocalDateTime availableAt
    ) {
        JournalEntity journal = jpaJournalRepository.findByReferenceId(correlationId)
                .orElseGet(() -> {
                    JournalEntity newJournal = new JournalEntity();
                    newJournal.setId(UUID.randomUUID());
                    newJournal.setCreatedAt(LocalDateTime.now());
                    newJournal.setDescription("Transação: " + description);
                    newJournal.setReferenceId(correlationId);
                    newJournal.setReferenceType("TRANSACTION");
                    return jpaJournalRepository.save(newJournal);
                });

        LedgerEntryEntity entryEntity = new LedgerEntryEntity();
        entryEntity.setAmount(amount);
        entryEntity.setType(type);
        entryEntity.setDescription(description);
        entryEntity.setCorrelationId(correlationId);
        entryEntity.setCreatedAt(LocalDateTime.now());
        entryEntity.setAvailableAt(availableAt);
        entryEntity.setJournal(journal);

        LedgerAccountEntity accountEntity = jpaLedgerAccountRepository
                .findById(account.getAccountId())
                .orElseThrow(() -> new DomainException("Conta contábil não encontrada."));

        entryEntity.setLedgerAccount(accountEntity);

        jpaLedgerEntryRepository.save(entryEntity);

        log.info("Lançamento de {} registrado para a conta {} [Journal: {}] | Disponível em: {}",
                type, account.getAccountNumber(), journal.getId(), availableAt);
    }

    @Override
    public BigDecimal findAvailableBalance(UUID merchantId) {
        BigDecimal credits = jpaLedgerEntryRepository.sumAvailableCredits(merchantId);
        BigDecimal debits = jpaLedgerEntryRepository.sumDebits(merchantId);
        return credits != null ? credits.subtract(debits != null ? debits : BigDecimal.ZERO) : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal findFutureReceivables(UUID merchantId) {
        return jpaLedgerEntryRepository.sumFutureReceivables(merchantId);
    }

    @Override
    public BigDecimal findRealAvailableBalance(UUID merchantId) {
        return jpaLedgerEntryRepository.calculateRealAvailableBalance(merchantId);
    }

    @Override
    public LedgerBalanceProjection getLedgerBalances(UUID merchantId) {
        return jpaLedgerEntryRepository.getBalances(merchantId);
    }
}
