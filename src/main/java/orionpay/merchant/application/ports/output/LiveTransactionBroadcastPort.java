package orionpay.merchant.application.ports.output;

import orionpay.merchant.domain.model.Transaction;

/**
 * Porta de saída para o broadcasting de eventos de transação em tempo real.
 */
@FunctionalInterface
public interface LiveTransactionBroadcastPort {
    
    /**
     * Envia um evento de transação que acabou de ser salva no Outbox.
     * A implementação deve ser assíncrona para não impactar a performance da requisição principal.
     *
     * @param transaction O objeto de domínio da transação que foi processada.
     */
    void broadcastTransactionSaved(Transaction transaction);
}
