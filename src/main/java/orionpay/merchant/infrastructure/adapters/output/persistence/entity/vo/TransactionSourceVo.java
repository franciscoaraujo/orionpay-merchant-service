package orionpay.merchant.infrastructure.adapters.output.persistence.entity.vo;



import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor // Exigido pelo JPA
public class TransactionSourceVo {

    @Column(name = "source_terminal_sn", length = 50)
    private String terminalSerialNumber;

    @Column(name = "source_software_version", length = 20)
    private String softwareVersion;

    @Column(name = "source_entry_mode", length = 20)
    private String entryMode; // CHIP, CONTACTLESS, MANUAL, ECOMMERCE

    @Column(name = "source_ip_address", length = 45)
    private String ipAddress;

    public TransactionSourceVo(String terminalSerialNumber, String softwareVersion, String entryMode, String ipAddress) {
        this.terminalSerialNumber = terminalSerialNumber;
        this.softwareVersion = softwareVersion;
        this.entryMode = entryMode;
        this.ipAddress = ipAddress;
    }
}