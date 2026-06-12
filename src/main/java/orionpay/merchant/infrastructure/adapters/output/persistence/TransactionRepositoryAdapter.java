package orionpay.merchant.infrastructure.adapters.output.persistence;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import orionpay.merchant.domain.model.ExtratoTransaction;
import orionpay.merchant.domain.model.Transaction;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.MerchantEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.TransactionEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.mapper.TransactionMapper;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.BrandDistributionProjection;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.HourlySalesProjection;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.TransactionSummaryProjection;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaMerchantRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaTerminalRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaTransactionRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.TransactionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final JpaTransactionRepository jpaTransactionRepository;
    private final JpaMerchantRepository jpaMerchantRepository;
    private final JpaTerminalRepository jpaTerminalRepository;
    private final TransactionMapper mapper;

    @Override
    public Transaction save(Transaction transaction) {
        // 1. Converte campos simples (amount, status, nsu, card info...)
        TransactionEntity entity = mapper.toEntity(transaction);

        // 2. Busca a referência do Merchant
        MerchantEntity merchantRef = jpaMerchantRepository.getReferenceById(transaction.getMerchant().getId());
        entity.setMerchant(merchantRef);

        // 3. Vínculo opcional do Terminal (se o Serial Number for informado)
        if (transaction.getSource() != null && transaction.getSource().terminalSerialNumber() != null) {
            jpaTerminalRepository.findBySerialNumber(transaction.getSource().terminalSerialNumber())
                    .ifPresent(entity::setTerminal);
        }

        // 4. Persiste no banco
        val savedEntity = jpaTransactionRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return jpaTransactionRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Transaction> findByMerchantId(UUID merchantId) {
        return jpaTransactionRepository.findByMerchantId(merchantId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Transaction> findByNsu(String nsu) {
        return jpaTransactionRepository.findByNsu(nsu).map(mapper::toDomain);
    }

    @Override
    public TransactionSummaryProjection getSummaryByPeriod(UUID merchantId, LocalDateTime startDate, LocalDateTime endDate) {
        return jpaTransactionRepository.getSummaryByPeriod(merchantId, startDate, endDate);
    }

    @Override
    public TransactionSummaryProjection getDailySummary(UUID merchantId, LocalDateTime startOfDay) {
        return jpaTransactionRepository.getDailySummary(merchantId, startOfDay);
    }

    @Override
    public List<HourlySalesProjection> getHourlyTrend(UUID merchantId, LocalDateTime targetDate) {
        return jpaTransactionRepository.getHourlyTrend(merchantId, targetDate);
    }

    @Override
    public List<BrandDistributionProjection> getBrandDistribution(UUID merchantId, LocalDateTime startOfDay) {
        return jpaTransactionRepository.getBrandDistribution(merchantId, startOfDay);
    }

    @Override
    public Page<ExtratoTransaction> findCustomExtrato(UUID merchantId, String search, Pageable pageable) {
        String searchQuery = (search == null || search.trim().isEmpty()) ? null : search;
        var entities = jpaTransactionRepository.findAllByMerchantIdAndFilter(merchantId, searchQuery, pageable);
        return entities.map(mapper::toExtratoDomain);
    }

    @Override
    public Optional<Transaction> findByIdAndMerchantId(UUID transactionId, UUID merchantId) {
        return jpaTransactionRepository.findByIdAndMerchantId(transactionId, merchantId)
                .map(mapper::toDomain);
    }
}
