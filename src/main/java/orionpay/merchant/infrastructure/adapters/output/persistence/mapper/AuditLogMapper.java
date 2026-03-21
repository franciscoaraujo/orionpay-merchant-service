package orionpay.merchant.infrastructure.adapters.output.persistence.mapper;

import org.mapstruct.Mapper;
import orionpay.merchant.domain.model.AuditLog;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.AuditLogEntity;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {
    // Converte a entidade de auditoria para o Record de domínio
    AuditLog toDomain(AuditLogEntity entity);

    // Mapeia o Record para a Entity antes de salvar no schema 'audit'
    AuditLogEntity toEntity(AuditLog domain);
}