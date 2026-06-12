package orionpay.merchant.infrastructure.adapters.output.persistence.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface AgendaItemProjection {
    UUID getIdExt();
    UUID getTransactionId(); // Adicionado para rastreabilidade
    LocalDateTime getSettlementDate();
    LocalDateTime getTransactionDate();
    BigDecimal getGrossAmount();
    BigDecimal getMdrAmount();
    BigDecimal getNetAmount();
    String getStatus();
    LocalDateTime getPaidAt();
    String getNsu();
    String getCardBrand();
    String getCardLastFour();
    String getProductType();
    
    // Novas Flags e Parcelas
    Boolean getBlocked();
    Boolean getAnticipated();
    Integer getInstallmentNumber();
    BigDecimal getMdrPercentage();
    BigDecimal getOriginalAmount();
}
