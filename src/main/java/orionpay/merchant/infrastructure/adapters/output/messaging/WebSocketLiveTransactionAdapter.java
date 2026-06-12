package orionpay.merchant.infrastructure.adapters.output.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import orionpay.merchant.application.ports.output.LiveTransactionBroadcastPort;
import orionpay.merchant.domain.model.Transaction;
import orionpay.merchant.infrastructure.adapters.output.messaging.dto.LiveTransactionDTO;

import java.time.LocalDateTime;

/**
 * Adaptador que implementa a porta de broadcasting via WebSockets.
 * Utiliza SimpMessagingTemplate para enviar eventos para um tópico STOMP.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketLiveTransactionAdapter implements LiveTransactionBroadcastPort {

    private static final String LIVE_TRANSACTIONS_TOPIC = "/topic/live-transactions";
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * {@inheritDoc}
     *
     * Este método é anotado com @Async para garantir que a comunicação WebSocket
     * ocorra em uma thread separada, não bloqueando a thread da requisição HTTP original.
     */
    @Async
    @Override
    public void broadcastTransactionSaved(Transaction transaction) {
        try {
            log.info("Broadcasting live event for transactionId: {}", transaction.getId());

            var liveDto = new LiveTransactionDTO(
                transaction.getId(),
                transaction.getMerchant().getId(),
                transaction.getAmount(),
                transaction.getStatus().name(), // ex: "APPROVED"
                "OUTBOX_SAVED",
                LocalDateTime.now()
            );

            messagingTemplate.convertAndSend(LIVE_TRANSACTIONS_TOPIC, liveDto);
            
            log.debug("Live event sent successfully to topic: {}", LIVE_TRANSACTIONS_TOPIC);

        } catch (Exception e) {
            // Loga a falha sem lançar exceção para não afetar o fluxo principal.
            log.error("Failed to broadcast live transaction event for transactionId: {}", transaction.getId(), e);
        }
    }
}
