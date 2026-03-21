package orionpay.merchant.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import orionpay.merchant.domain.model.enums.TransactionStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "transaction_event", schema = "core")
@Getter
@Setter
public class TransactionEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    private TransactionStatus type;

    private String description;

    @JdbcTypeCode(SqlTypes.JSON) // Armazena metadados variáveis como JSONB no Postgres
    private Map<String, String> metadata;

    private LocalDateTime occurredAt;
}