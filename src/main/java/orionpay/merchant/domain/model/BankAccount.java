package orionpay.merchant.domain.model;

import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.enums.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

public class BankAccount {
    private final String bankCode;    // Código COMPE (Ex: 001, 237, 341)
    private final String branch;      // Agência
    private final String account;     // Número da conta
    private final String accountDigit; // Dígito verificador
    private final AccountType type;    // Corrente ou Poupança
    private boolean verified;          // Indica se a conta passou por validação (ex: test deposit)

    public BankAccount(
            String bankCode,
            String branch,
            String account,
            String accountDigit,
            AccountType type
    ) {
        validateStructure(bankCode, branch, account);
        this.bankCode = bankCode;
        this.branch = branch;
        this.account = account;
        this.accountDigit = accountDigit;
        this.type = type;
        this.verified = false;
    }

    // REGRA DE NEGÓCIO: Validação básica de estrutura bancária brasileira
    private void validateStructure(String bank, String branch, String acc) {
        if (bank == null || bank.length() != 3) {
            throw new DomainException("Código do banco deve ter 3 dígitos (Padrão COMPE).");
        }
        if (branch == null || branch.isEmpty()) {
            throw new DomainException("Agência é obrigatória.");
        }
        if (acc == null || acc.isEmpty()) {
            throw new DomainException("Número da conta é obrigatório.");
        }
    }

    // REGRA DE NEGÓCIO: Marca a conta como verificada após validação externa
    public void markAsVerified() {
        this.verified = true;
    }

    // Getters manuais para garantir que o MapStruct consiga ler os campos
    public String getBankCode() {
        return bankCode;
    }

    public String getBranch() {
        return branch;
    }

    public String getAccount() {
        return account;
    }

    public String getAccountDigit() {
        return accountDigit;
    }

    public AccountType getType() {
        return type;
    }

    public boolean isVerified() {
        return verified;
    }

    // Método auxiliar existente
    public String getFullAccountNumber() {
        return this.account + "-" + this.accountDigit;
    }

    public void validateAnticipationEligibility() {
        if (!this.type.allowsAnticipation()) {
            throw new DomainException(
                    "Contas do tipo " + this.type.getDescription() + " não são elegíveis para antecipação."
            );
        }
    }
}