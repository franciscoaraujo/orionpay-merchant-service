package orionpay.merchant.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import orionpay.merchant.domain.model.AuditLog;
import orionpay.merchant.infrastructure.adapters.output.persistence.mapper.AuditLogMapper;
import orionpay.merchant.infrastructure.adapters.output.persistence.reposittory.JpaAuditLogRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final JpaAuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    public void logLoginAttempt(String email, String action, String details, String ipAddress) {
        AuditLog entry = new AuditLog(
                UUID.randomUUID(),
                email != null ? email : "anonymous",
                action,
                "AUTH",
                details,
                ipAddress,
                LocalDateTime.now()
        );
        auditLogRepository.save(auditLogMapper.toEntity(entry));
    }
}

