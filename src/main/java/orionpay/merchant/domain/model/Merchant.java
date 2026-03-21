package orionpay.merchant.domain.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.enums.MerchantStatus;
import orionpay.merchant.domain.model.enums.TerminalStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class Merchant {

    private final UUID id;
    private final String document;
    private String legalName;
    private String email;
    private MerchantStatus status;
    private BankAccount bankAccount;
    private List<MerchantPricing> pricings = new ArrayList<>();
    private List<Terminal> terminals = new ArrayList<>();
    private Address businessAddress;
    private LocalDateTime createdAt; // Renomeado de createAt para createdAt

    // Factory Method para criação de novos Merchants (Regras de Negócio)
    public static Merchant create(
            UUID id,
            String legalName,
            String document,
            String email
    ) {
        return new Merchant(id, legalName, document, email);
    }

    // Construtor privado para uso interno da Factory
    private Merchant(
            UUID id,
            String legalName,
            String document,
            String email
    ) {
        validateDocument(document);
        if (email == null || email.isBlank()) {
             throw new DomainException("E-mail é obrigatório");
        }
        this.id = id;
        this.document = document;
        this.legalName = legalName;
        this.email = email;
        this.status = MerchantStatus.PROVISIONAL;
        this.pricings = new ArrayList<>();
        this.createdAt = LocalDateTime.now(); // Renomeado
    }

    // Construtor PÚBLICO para reconstrução (Persistência -> Domínio)
    public Merchant(
            UUID id,
            String document,
            String legalName,
            String email,
            MerchantStatus status,
            BankAccount bankAccount,
            List<MerchantPricing> pricings,
            List<Terminal> terminals,
            Address businessAddress,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.document = document;
        this.legalName = legalName;
        this.email = email;
        this.status = status;
        this.bankAccount = bankAccount;
        this.pricings = pricings != null ? pricings : new ArrayList<>();
        this.terminals = terminals != null ? terminals : new ArrayList<>();
        this.businessAddress = businessAddress;
        this.createdAt = createdAt; // Renomeado
    }

    // REGRA DE NEGÓCIO: Ativação segura
    public void activate() {
        if (this.bankAccount == null) {
            throw new DomainException("Impossível ativar lojista sem domicílio bancário cadastrado.");
        }
        if (this.pricings.isEmpty()) {
            throw new DomainException("Lojista não possui tabela de taxas configurada.");
        }
        this.status = MerchantStatus.ACTIVE;
    }

    // REGRA DE NEGÓCIO: Bloqueio cautelar
    public void suspend(String reason) {
        if (this.status == MerchantStatus.TERMINATED) {
            throw new DomainException("Não é possível suspender um lojista encerrado.");
        }
        this.status = MerchantStatus.SUSPENDED;
        // Log de motivo seria disparado via evento aqui
    }

    public void updateBankAccount(BankAccount newAccount) {
        if (newAccount == null) throw new DomainException("Conta bancária inválida.");
        this.bankAccount = newAccount;
    }

    private void validateDocument(String doc) {
        if (doc == null || (doc.length() != 11 && doc.length() != 14)) {
            throw new DomainException("Documento (CPF/CNPJ) inválido.");
        }
    }

    // REGRA: Um lojista não pode ter mais de 10 terminais ativos sem análise especial
    public void addTerminal(Terminal terminal) {
        long activeCount = terminals.stream()
                .filter(t -> t.getStatus() == TerminalStatus.ACTIVE)
                .count();

        if (activeCount >= 10) {
            throw new DomainException("Limite de terminais ativos atingido para este lojista.");
        }
        this.terminals.add(terminal);
    }

    public void changeAddress(Address newAddress) {
        // Regra de Compliance: Mudança de endereço pode exigir re-validação de KYC
        this.businessAddress = newAddress;
        if (this.status == MerchantStatus.ACTIVE) {
            this.status = MerchantStatus.PROVISIONAL; // Volta para análise se mudar de morada
        }
    }

}