package orionpay.merchant.domain.model;

import lombok.Getter;
import orionpay.merchant.domain.excepion.DomainException;
import orionpay.merchant.domain.model.enums.ProductType;
import orionpay.merchant.domain.model.enums.TransactionStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Transaction {
    private final UUID id;
    private final Merchant merchant;
    private final BigDecimal amount;
    private final String currency;
    private String nsu;
    private String authorizationCode;
    private final ProductType productType;
    private final TransactionSource source;
    private TransactionStatus status;
    private final LocalDateTime createdAt;

    private String cardBrand;
    private String cardBin;
    private String cardLastFour;
    private String cardHolderName;

    private BigDecimal netAmount; // Valor após taxas

    // Novo campo sugerido para guardar o motivo de recusa
    private String refusalReason;

    public Transaction(
            UUID id,
            Merchant merchant,
            BigDecimal amount,
            ProductType productType,
            TransactionSource source
    ) {
        this.id = id;
        this.merchant = merchant;
        this.amount = amount;
        this.currency = "BRL";
        this.productType = productType;
        this.source = source;
        this.status = TransactionStatus.PENDING; // Nasce sempre como pendente
        this.createdAt = LocalDateTime.now();
    }

    /**
     * PREENCHIMENTO DE DADOS SEGUROS
     */
    public void setCardInfo(String brand, String bin, String lastFour, String holderName) {
        this.cardBrand = brand;
        this.cardBin = bin;
        this.cardLastFour = lastFour;
        this.cardHolderName = holderName;
    }

    /**
     * REGRA DE NEGÓCIO: Processa a aprovação vinda do autorizador externo (Gateway).
     */
    public void processApproval(String nsu, String authorizationCode) {
        if (this.status != TransactionStatus.PENDING) {
            throw new DomainException("Transação não pode ser aprovada pois está em estado: " + this.status);
        }
        if (nsu == null || nsu.isBlank() || authorizationCode == null || authorizationCode.isBlank()) {
            throw new DomainException("Dados de autorização da rede (NSU/AuthCode) são obrigatórios.");
        }

        this.nsu = nsu;
        this.authorizationCode = authorizationCode;
        this.status = TransactionStatus.APPROVED;
    }

    /**
     * REGRA DE NEGÓCIO: Processa a recusa vinda do autorizador externo (Gateway).
     */
    public void decline(String errorMessage) {
        if (this.status != TransactionStatus.PENDING) {
            throw new DomainException("Transação não pode ser negada pois está em estado: " + this.status);
        }

        this.status = TransactionStatus.DECLINED;
        this.refusalReason = errorMessage; // Salva o motivo (ex: "51 - Saldo Insuficiente")
    }

    /**
     * REGRA DE NEGÓCIO: Estorno da transação.
     */
    public void reverse() {
        if (this.status != TransactionStatus.APPROVED) {
            throw new DomainException("Apenas transações aprovadas podem ser estornadas.");
        }
        this.status = TransactionStatus.REVERSED;
    }

    public void calculateNetValue(BigDecimal mdrPercentage) {
        // Regra: Valor - (Valor * (MDR / 100))
        BigDecimal fee = this.amount.multiply(mdrPercentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        this.netAmount = this.amount.subtract(fee);
    }
}