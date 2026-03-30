package orionpay.merchant.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TRANSACTION_AUTHORIZED_EXCHANGE = "payment.v1.transaction-authorized";
    public static final String SETTLEMENT_PROCESS_QUEUE = "settlement.process-queue";
    public static final String SETTLEMENT_PROCESS_DLQ = "settlement.process-queue.dlq"; // AJUSTADO para o nome existente
    public static final String SETTLEMENT_ROUTING_KEY = "settlement.process";

    // --- Exchange (Fanout compatível) ---
    @Bean
    public FanoutExchange transactionAuthorizedExchange() {
        return new FanoutExchange(TRANSACTION_AUTHORIZED_EXCHANGE);
    }

    // --- Queues ---
    @Bean
    public Queue settlementProcessQueue() {
        return QueueBuilder.durable(SETTLEMENT_PROCESS_QUEUE)
                .withArgument("x-dead-letter-exchange", "") 
                .withArgument("x-dead-letter-routing-key", SETTLEMENT_PROCESS_DLQ)
                .build();
    }

    @Bean
    public Queue settlementProcessDlq() {
        return new Queue(SETTLEMENT_PROCESS_DLQ);
    }

    // --- Binding ---
    @Bean
    public Binding settlementBinding(Queue settlementProcessQueue, FanoutExchange transactionAuthorizedExchange) {
        return BindingBuilder.bind(settlementProcessQueue)
                .to(transactionAuthorizedExchange);
    }

    // --- Message Converter ---
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // --- RabbitTemplate ---
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
