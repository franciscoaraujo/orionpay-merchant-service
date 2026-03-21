package orionpay.merchant.domain.model;

public record TransactionSource(
        String terminalSerialNumber,
        String softwareVersion,
        String entryMode // Ex: CHIP, CONTACTLESS, E-COMMERCE
) {}