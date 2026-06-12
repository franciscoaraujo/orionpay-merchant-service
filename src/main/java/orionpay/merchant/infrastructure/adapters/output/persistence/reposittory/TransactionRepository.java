package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import orionpay.merchant.domain.model.ExtratoTransaction;
import orionpay.merchant.domain.model.Transaction;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.BrandDistributionProjection;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.HourlySalesProjection;
import orionpay.merchant.infrastructure.adapters.output.persistence.projection.TransactionSummaryProjection;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    Optional<Transaction> findById(UUID id);

    List<Transaction> findByMerchantId(UUID merchantId);

    // Para conciliação e busca por comprovante (NSU)
    Optional<Transaction> findByNsu(String nsu);

    TransactionSummaryProjection getSummaryByPeriod(UUID merchantId, LocalDateTime startDate, LocalDateTime endDate);

    TransactionSummaryProjection getDailySummary(UUID merchantId, LocalDateTime startOfDay);

    List<HourlySalesProjection> getHourlyTrend(UUID merchantId, LocalDateTime startOfDay);

    List<BrandDistributionProjection> getBrandDistribution(UUID merchantId, LocalDateTime startOfDay);

    Page<ExtratoTransaction> findCustomExtrato(UUID merchantId, String search, Pageable pageable);

    Optional<Transaction> findByIdAndMerchantId(UUID transactionId, UUID merchantId);
}
