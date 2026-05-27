package orionpay.merchant.infrastructure.adapters.output.grpc;

import com.google.protobuf.util.JsonFormat;
import com.orionpay.merchant.grpc.acquirerswitch.v1.AcquiringSwitchServiceGrpc;
import com.orionpay.merchant.grpc.acquirerswitch.v1.AuthorizeRequest;
import com.orionpay.merchant.grpc.acquirerswitch.v1.AuthorizeResponse;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import orionpay.merchant.application.ports.output.AcquiringSwitchPort;
import orionpay.merchant.domain.model.Authorization;
import orionpay.merchant.domain.model.Refund;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AcquiringSwitchGrpcAdapter implements AcquiringSwitchPort {

    @GrpcClient("adquiring-switch-service")
    private AcquiringSwitchServiceGrpc.AcquiringSwitchServiceBlockingStub blockingStub;

    @Override
    public Optional<Authorization.Result> authorizeTransaction(Authorization.Request domainRequest) {
        AuthorizeRequest grpcRequest = toGrpcRequest(domainRequest);
        try {
            log.info("Sending gRPC AuthorizeRequest to switch: {}", JsonFormat.printer().omittingInsignificantWhitespace().print(grpcRequest));
            
            AuthorizeResponse grpcResponse = blockingStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .authorizeTransaction(grpcRequest);
            
            return Optional.of(toDomainResult(grpcResponse));
        } catch (StatusRuntimeException e) {
            log.error("gRPC error during authorization: {}", e.getStatus(), e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error processing gRPC call or message formatting", e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Refund.Result> refundTransaction(Refund.Request domainRequest) {
        log.warn("A função de estorno (refund) não está implementada no contrato gRPC atual.");
        throw new UnsupportedOperationException("Refund functionality is not implemented in the current gRPC contract.");
    }

    // --- Mappers Atualizados ---

    private AuthorizeRequest toGrpcRequest(Authorization.Request r) {
        AuthorizeRequest.Builder builder = AuthorizeRequest.newBuilder();

        if (r.getId() != null) {
            builder.setId(r.getId().toString());
        } else {
            builder.setId("");
        }
        
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId().toString());
        } else {
            builder.setMerchantId("");
        }
        
        if (r.getTerminalId() != null) {
            builder.setTerminalId(r.getTerminalId());
        } else {
            builder.setTerminalId("");
        }
        
        if (r.getPanMasked() != null) {
            builder.setPanMasked(r.getPanMasked());
        } else {
            builder.setPanMasked("");
        }
        
        if (r.getPinBlock() != null) {
            builder.setPinBlockTerminal(r.getPinBlock());
        } else {
            builder.setPinBlockTerminal("");
        }

        builder.setAmount(String.valueOf(r.getAmountInCents()));
        builder.setCurrencyCode(r.getCurrencyCode());

        return builder.build();
    }

    private Authorization.Result toDomainResult(AuthorizeResponse r) {
        // CORREÇÃO TEMPORÁRIA: Usando statusCode para nsuAcquirer e nsuBrand
        // para evitar "value too long for type character varying(20)"
        // O ideal seria que o .proto e/ou o banco de dados tivessem campos adequados.
        String shortCode = r.getStatusCode() != null && r.getStatusCode().length() <= 20 ? r.getStatusCode() : "UNKNOWN";

        return Authorization.Result.builder()
                .id(UUID.fromString(r.getTransactionId()))
                .responseCode(r.getStatusCode())
                .nsuAcquirer(shortCode) // Usando statusCode, que é curto
                .nsuBrand(shortCode)    // Usando statusCode, que é curto
                .authorizedAt(Instant.now())
                .build();
    }
}
