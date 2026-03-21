package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import orionpay.merchant.domain.model.Merchant;

import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository {
    Optional<Merchant> findById(UUID id);

    Optional<Merchant> findByDocument(String document);

    Merchant save(Merchant merchant);
}