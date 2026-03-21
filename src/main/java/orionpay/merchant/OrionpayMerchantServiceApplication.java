package orionpay.merchant;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import java.util.TimeZone;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "orionpay.merchant.infrastructure.adapters.output.persistence")
@EntityScan(basePackages = "orionpay.merchant.infrastructure.adapters.output.persistence.entity")
@ComponentScan(basePackages = "orionpay.merchant")
// Adiciona suporte a serialização estável de Paginação
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class OrionpayMerchantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrionpayMerchantServiceApplication.class, args);
    }

    // Adicione este bloco! Ele roda assim que o Spring Boot sobe.
    @PostConstruct
    public void init() {
        // Força o fuso horário padrão para o de Brasília (GMT-3)
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
    }

}
