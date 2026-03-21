package orionpay.merchant.infrastructure.adapters.output.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import orionpay.merchant.domain.model.Merchant;
import orionpay.merchant.infrastructure.adapters.output.persistence.mapper.MerchantMapper;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaMerchantRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.MerchantRepository;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MerchantRepositoryAdapter implements MerchantRepository {

    private final JpaMerchantRepository jpaMerchantRepository;
    private final MerchantMapper merchantMapper;

    @Override
    public Optional<Merchant> findById(UUID id) {
        return jpaMerchantRepository.findById(id).map(merchantMapper::toDomain);
    }

    @Override
    public Optional<Merchant> findByDocument(String document) {
        return jpaMerchantRepository.findByDocument(document).map(merchantMapper::toDomain);
    }

    @Override
    public Merchant save(Merchant merchant) {
        var entity = merchantMapper.toEntity(merchant);
        var savedEntity = jpaMerchantRepository.save(entity);
        return merchantMapper.toDomain(savedEntity);
    }
}
