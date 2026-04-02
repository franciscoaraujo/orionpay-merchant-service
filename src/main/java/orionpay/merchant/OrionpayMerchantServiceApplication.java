package orionpay.merchant;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling // Habilita o agendamento de tarefas (Necessário p/ o Outbox Relay)
@EnableJpaRepositories(basePackages = "orionpay.merchant.infrastructure.adapters.output.persistence")
@EntityScan(basePackages = "orionpay.merchant.infrastructure.adapters.output.persistence.entity")
@ComponentScan(basePackages = "orionpay.merchant")
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class OrionpayMerchantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrionpayMerchantServiceApplication.class, args);
    }

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
    }

}
