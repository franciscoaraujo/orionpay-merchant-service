package orionpay.merchant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configura o WebSocket para broadcasting de eventos em tempo real.
 * Habilita o broker de mensagens STOMP sobre SockJS para máxima compatibilidade.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configura o broker de mensagens.
     * - Define o prefixo "/topic" para mensagens destinadas a todos os clientes inscritos.
     * - Define o prefixo "/app" para mensagens enviadas de clientes para o servidor.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Registra o endpoint STOMP que os clientes usarão para se conectar ao servidor.
     * - O SockJS é usado como um fallback para navegadores que não suportam WebSockets nativamente.
     * - CORS é configurado para permitir conexões de qualquer origem, facilitando o desenvolvimento do frontend.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-orionpay-live")
                .setAllowedOriginPatterns("*") // Permite conexões de qualquer origem (ótimo para dev local)
                .withSockJS(); // Usa SockJS para garantir a conectividade
    }
}
