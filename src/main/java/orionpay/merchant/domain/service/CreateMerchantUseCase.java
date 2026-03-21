package orionpay.merchant.domain.service;


import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orionpay.merchant.application.ports.input.rest.dto.MerchantResponse;
import orionpay.merchant.domain.event.MerchantCreatedEvent;
import orionpay.merchant.domain.model.Address;
import orionpay.merchant.domain.model.BankAccount;
import orionpay.merchant.domain.model.Merchant;
import orionpay.merchant.domain.model.enums.MerchantStatus;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.MerchantRegistrationRequest;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.MerchantRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
// ... outros imports

@Service
@RequiredArgsConstructor
public class CreateMerchantUseCase {

    private final MerchantRepository merchantRepository;
    private final ApplicationEventPublisher eventPublisher; // Ferramenta nativa do Spring

    @Transactional
    public MerchantResponse execute(MerchantRegistrationRequest request) {

        // 1. Valida e cria a entidade de domínio
        Merchant merchant = new Merchant(
                UUID.randomUUID(),
                request.name(),
                request.document(),
                request.email(),
                MerchantStatus.PROVISIONAL,
                new BankAccount(
                        request.bankCode(),
                        request.branch(),
                        request.account(),
                        request.accountDigit(),
                        request.accountType()
                ),
                new ArrayList<>(),
                new ArrayList<>(),
                new Address(
                        request.street(),
                        request.number(),
                        request.complement(),
                        request.neighborhood(),
                        request.city(),
                        request.state(),
                        request.zipCode()
                ),
                LocalDateTime.now()
        );

        // 2. Persiste o lojista no banco
        merchantRepository.save(merchant);

        // 3. O Pulo do Gato: Dispara o evento para o resto do sistema
        eventPublisher.publishEvent(new MerchantCreatedEvent(
                merchant.getId(),
                merchant.getDocument(),
                merchant.getLegalName()
        ));

        return MerchantResponse.fromDomain(merchant);
    }
}