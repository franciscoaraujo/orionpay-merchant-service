package orionpay.merchant.infrastructure.adapters.input.rest.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import orionpay.merchant.config.AuthenticatedUser;
import orionpay.merchant.domain.excepion.DomainException;

import java.util.UUID;

@Service
public class SecurityContextService {

    public UUID getCurrentMerchantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            UUID merchantId = user.getMerchantId();
            if (merchantId == null) {
                throw new DomainException("Usuário autenticado não possui um Merchant ID vinculado.", "SECURITY_ERROR");
            }
            return merchantId;
        }
        
        throw new DomainException("Não foi possível recuperar o contexto de autenticação.", "UNAUTHORIZED");
    }
}
