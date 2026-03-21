package orionpay.merchant.domain.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class ExtratoTransactionDetail {
    UUID id;            // ID Interno (image_c3518d.png)
    String externalId;  // tx_123456
    String nsu;
    BigDecimal amount;  // Valor Bruto
    LocalDateTime createdAt;
    String status;      // Capturado, Negado, etc.

    // Dados do Cartão
    String brand;       // VISA, MASTERCARD
    String cardBin;     // 411111
    String lastFour;    // 1234
    String holderName;  // Nome do Portador

    // Dados de Origem (image_c51bc5.png)
    String terminalSerialNumber;
    String entryMode;   // CHIP, CONTACTLESS, MANUAL
    String authCode;    // Código de autorização do adquirente
}