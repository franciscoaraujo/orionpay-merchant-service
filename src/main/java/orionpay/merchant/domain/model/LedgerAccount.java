package orionpay.merchant.domain.model;

import lombok.Getter;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.enums.EntryType;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class LedgerAccount {
    private final UUID accountId;
    private final UUID merchantId;
    private BigDecimal balance;
    private final String accountNumber;
    private final Long version; // Adicionado para controle de concorrência (Optimistic Locking)

    // Factory Method para criação de nova conta (versão nula ou 0)
    public static LedgerAccount create(
            UUID accountId,
            UUID merchantId,
            String accountNumber,
            BigDecimal balance
    ) {
        return new LedgerAccount(accountId, merchantId, accountNumber, balance, null);
    }

    // Construtor PÚBLICO para reconstrução (Persistência -> Domínio)
    // MapStruct usará ESTE construtor pois é o único público para reconstrução
    public LedgerAccount(
            UUID accountId,
            UUID merchantId,
            String accountNumber,
            BigDecimal balance,
            Long version
    ) {
        this.accountId = accountId;
        this.merchantId = merchantId;
        this.accountNumber = accountNumber;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.version = version;
    }


    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new DomainException("Crédito deve ser positivo.");
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new DomainException("Débito deve ser positivo.");
        if (this.balance.compareTo(amount) < 0) throw new DomainException("Saldo insuficiente na conta contábil.");
        this.balance = this.balance.subtract(amount);
    }

    /**
     * REGRA DE NEGÓCIO: Processa um lançamento contábil genérico.
     * Utiliza o multiplicador do EntryType para afetar o saldo.
     */
    public void applyEntry(BigDecimal amount, EntryType type) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("O valor do lançamento deve ser maior que zero.");
        }

        BigDecimal effect = amount.multiply(new BigDecimal(type.getMultiplier()));
        BigDecimal newBalance = this.balance.add(effect);

        // Regra de Proteção: Algumas contas não podem ficar negativas
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("Saldo insuficiente para realizar este lançamento de " + type.getDescription());
        }

        this.balance = newBalance;
    }
}