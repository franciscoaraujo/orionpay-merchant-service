package orionpay.merchant.infrastructure.adapters.output.persistence;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import orionpay.merchant.domain.model.MerchantPricing;
import orionpay.merchant.domain.model.enums.ProductType;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.MerchantEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.PricingEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.mapper.MerchantPricingMapper;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaMerchantRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaPricingRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.PricingRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PricingRepositoryAdapter implements PricingRepository {

    private final JpaPricingRepository jpaPricingRepository;
    private final JpaMerchantRepository jpaMerchantRepository;
    private final MerchantPricingMapper mapper;

    @Override
    @Cacheable(value = "merchant_pricing", key = "#merchantId")
    public Optional<MerchantPricing> findCurrentPricing(UUID merchantId) {
        return jpaPricingRepository.findFirstByMerchantIdOrderByEffectiveDateDesc(merchantId)
                .map(mapper::toDomain);
    }

    @Override
    @Cacheable(value = "merchant_pricing_product", key = "#merchantId + '_' + #productType")
    public Optional<MerchantPricing> findCurrentPricing(UUID merchantId, ProductType productType) {
        Optional<PricingEntity> entity = jpaPricingRepository
                .findFirstByMerchantIdAndProductTypeOrderByEffectiveDateDesc(merchantId, productType);
        if (entity.isEmpty()) return Optional.empty();
        return entity.map(mapper::toDomain);
    }

    @Override
    @Transactional
    // Invalida ambos os caches relacionados ao lojista quando há atualização de taxas
    @CacheEvict(value = {"merchant_pricing", "merchant_pricing_product"}, allEntries = true) 
    // Nota: allEntries é um pouco agressivo, mas seguro. Idealmente usaríamos key = "#domainList[0].merchantId"
    // mas a lista pode ser vazia ou mista (embora não deva).
    public void saveAll(List<MerchantPricing> domainList) {
        if (domainList == null || domainList.isEmpty()) return;

        // 1. Pegamos o ID do Lojista (assumindo que todos na lista pertencem ao mesmo lojista)
        UUID merchantId = domainList.get(0).getMerchantId();

        // 2. Buscamos a referência do lojista no banco (Proxy do Hibernate)
        // Isso evita um SELECT desnecessário para cada item da lista
        MerchantEntity merchantRef = jpaMerchantRepository.getReferenceById(merchantId);

        // 3. Convertemos a lista de Domínio para lista de Entity via Mapper
        List<PricingEntity> entities = mapper.toEntityList(domainList);

        // 4. Vínculo Manual: Percorremos as entidades para setar o Merchant
        entities.forEach(entity -> entity.setMerchant(merchantRef));

        // 5. Salva tudo de uma vez (Batch Insert)
        jpaPricingRepository.saveAll(entities);
    }

}
