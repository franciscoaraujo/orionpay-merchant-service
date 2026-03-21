package orionpay.merchant.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log", schema = "audit")
@Getter
@Setter
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String userEmail;
    private String action; // Ex: RATE_CHANGE, LOGIN_SUCCESS
    private String resource; // Ex: MERCHANT_PRICING

    @Column(columnDefinition = "TEXT")
    private String details; // JSON com o "antes" e "depois"

    private String ipAddress;
    private LocalDateTime timestamp;
}