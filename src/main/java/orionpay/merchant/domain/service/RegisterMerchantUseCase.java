package orionpay.merchant.domain.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import orionpay.merchant.application.ports.input.rest.dto.MerchantResponse;
import orionpay.merchant.domain.event.MerchantCreatedEvent;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.Address;
import orionpay.merchant.domain.model.BankAccount;
import orionpay.merchant.domain.model.LedgerAccount;
import orionpay.merchant.domain.model.Merchant;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.MerchantRegistrationRequest;
import orionpay.merchant.infrastructure.adapters.output.persistence.mapper.MerchantMapper;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.LedgerRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.MerchantRepository;

import java.math.BigDecimal;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class RegisterMerchantUseCase {

    private final MerchantRepository merchantRepository;
    private final ApplicationEventPublisher eventPublisher; // Orquestrador de eventos

    @Transactional
    public MerchantResponse execute(MerchantRegistrationRequest request) {

        // 1. Validar duplicidade
        merchantRepository.findByDocument(request.document())
                .ifPresent(m -> {
                    throw new DomainException("Lojista já cadastrado.", "MERCHANT_ALREADY_EXISTS");
                });

        // 2. Criar o Modelo Rico (Identidade)
        Merchant merchant = Merchant.create(
                UUID.randomUUID(),
                request.name(),
                request.document(),
                request.email()
        );

        // 3. Dados Auxiliares (Endereço e Conta para Saque)
        merchant.changeAddress(new Address(
                request.street(), request.number(), request.complement(),
                request.neighborhood(), request.city(), request.state(), request.zipCode()
        ));

        merchant.updateBankAccount(new BankAccount(
                request.bankCode(), request.branch(), request.account(),
                request.accountDigit(), request.accountType()
        ));

        // 4. Persistir apenas o Lojista
        Merchant savedMerchant = merchantRepository.save(merchant);

        // 5. DISPARAR EVENTO: "Aconteceu algo no meu módulo!"
        eventPublisher.publishEvent(new MerchantCreatedEvent(
                savedMerchant.getId(),
                savedMerchant.getDocument(),
                savedMerchant.getLegalName()
        ));

        log.info("Lojista registrado com sucesso: {} [ID: {}]",
                savedMerchant.getLegalName(), savedMerchant.getId());

        return MerchantResponse.fromDomain(savedMerchant);
    }
}