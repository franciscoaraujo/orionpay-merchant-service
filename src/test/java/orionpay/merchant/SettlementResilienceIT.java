package orionpay.merchant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import orionpay.merchant.domain.model.TransactionEvent;
import orionpay.merchant.domain.model.enums.ProductType;
import orionpay.merchant.domain.model.enums.SettlementStatus;
import orionpay.merchant.domain.model.enums.TransactionStatus;
import orionpay.merchant.domain.service.SettlementService;
import orionpay.merchant.infrastructure.adapters.output.messaging.OutboxRelay;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.OutboxEventEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.SettlementEntryEntity;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaOutboxRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaSettlementEntryRepository;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.LedgerRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public class SettlementResilienceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withInitScript("init-test-db.sql");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private JpaSettlementEntryRepository settlementRepository;

    @Autowired
    private JpaOutboxRepository outboxRepository;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private LedgerRepository ledgerRepository;

    @SpyBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setup() {
        settlementRepository.deleteAll();
        outboxRepository.deleteAll();
        reset(rabbitTemplate);
    }

    @Test
    @DisplayName("Cenário 2: Idempotência de Banco de Dados - Inserção Simultânea")
    void testDatabaseIdempotencyWithConcurrentCalls() {
        UUID transactionId = UUID.randomUUID();
        TransactionEvent event = createEvent(transactionId);

        // Dispara duas chamadas simultâneas
        CompletableFuture<Void> call1 = CompletableFuture.runAsync(() -> 
            settlementService.processSingleInstallment(event, createCalculated(1), BigDecimal.valueOf(3.5), 1));
        
        CompletableFuture<Void> call2 = CompletableFuture.runAsync(() -> 
            settlementService.processSingleInstallment(event, createCalculated(1), BigDecimal.valueOf(3.5), 1));

        // Aguarda a conclusão. Uma delas deve lançar CompletionException causada por DataIntegrityViolationException
        try {
            CompletableFuture.allOf(call1, call2).get();
        } catch (Exception ignored) {
            // Ignoramos a exceção pois ela é o comportamento esperado de trava do banco
        }

        // Validação Final: Independente de qual thread ganhou, só pode existir 1 registro no banco
        List<SettlementEntryEntity> entries = settlementRepository.findAll();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getTransactionId()).isEqualTo(transactionId);
    }

    @Test
    @DisplayName("Cenário 3: Resiliência do Outbox Relay com Falha na Mensageria")
    void testOutboxRelayResilienceWithBrokerFailure() throws Exception {
        UUID transactionId = UUID.randomUUID();
        TransactionEvent event = createEvent(transactionId);

        OutboxEventEntity outboxEvent = OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(transactionId)
                .type("TRANSACTION_AUTHORIZED")
                .payload(objectMapper.writeValueAsString(event))
                .status(OutboxEventEntity.OutboxStatus.PENDING)
                .build();
        outboxRepository.save(outboxEvent);

        doThrow(new RuntimeException("Connection Timeout")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        outboxRelay.processOutbox();
        
        OutboxEventEntity afterFailure = outboxRepository.findById(outboxEvent.getId()).get();
        assertThat(afterFailure.getStatus()).isEqualTo(OutboxEventEntity.OutboxStatus.FAILED);

        reset(rabbitTemplate);
        afterFailure.setStatus(OutboxEventEntity.OutboxStatus.PENDING);
        outboxRepository.save(afterFailure);

        outboxRelay.processOutbox();

        await().atMost(10, SECONDS).untilAsserted(() -> {
            OutboxEventEntity finalEvent = outboxRepository.findById(outboxEvent.getId()).get();
            assertThat(finalEvent.getStatus()).isEqualTo(OutboxEventEntity.OutboxStatus.PROCESSED);
        });
    }

    @Test
    @DisplayName("Cenário 4: Circuit Breaker - Fallback para PENDING em falha do Ledger")
    void testCircuitBreakerFallbackToPending() {
        UUID transactionId = UUID.randomUUID();
        TransactionEvent event = createEvent(transactionId);

        doThrow(new RuntimeException("Ledger Timeout")).when(ledgerRepository).findByMerchantId(any());

        settlementService.processSingleInstallment(event, createCalculated(1), BigDecimal.valueOf(3.5), 1);

        Optional<SettlementEntryEntity> entry = settlementRepository.findByTransactionIdAndInstallmentNumber(transactionId, 1);
        assertThat(entry).isPresent();
        assertThat(entry.get().getStatus()).isEqualTo(SettlementStatus.PENDING);
    }

    private TransactionEvent createEvent(UUID transactionId) {
        return TransactionEvent.builder()
                .transactionId(transactionId)
                .merchantId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(100))
                .productType(ProductType.CREDIT_A_VISTA)
                .status(TransactionStatus.APPROVED)
                .occurredAt(LocalDateTime.now())
                .build();
    }

    private orionpay.merchant.domain.service.SettlementCalculator.CalculatedInstallment createCalculated(int number) {
        return orionpay.merchant.domain.service.SettlementCalculator.CalculatedInstallment.builder()
                .installmentNumber(number)
                .grossAmount(BigDecimal.valueOf(100))
                .mdrAmount(BigDecimal.valueOf(3.5))
                .netAmount(BigDecimal.valueOf(96.5))
                .expectedSettlementDate(LocalDateTime.now().plusDays(30))
                .build();
    }
}
