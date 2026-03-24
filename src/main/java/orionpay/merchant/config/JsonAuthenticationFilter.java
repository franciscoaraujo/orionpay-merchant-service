package orionpay.merchant.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Slf4j
public class JsonAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        log.info("Tentativa de autenticação. Content-Type: {}", request.getContentType());

        if (request.getContentType() != null && request.getContentType().contains("application/json")) {
            try {
                // Lê o corpo da requisição e mapeia para o DTO
                LoginRequest loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);
                
                log.info("Login via JSON recebido para usuário: {}", loginRequest.getUsername());

                UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(), loginRequest.getPassword());
                
                setDetails(request, authRequest);
                
                return this.getAuthenticationManager().authenticate(authRequest);
            } catch (IOException e) {
                log.error("Erro ao ler JSON de login", e);
                throw new AuthenticationServiceException("Falha ao ler JSON de login", e);
            }
        }
        
        log.info("Login via Form-Data (padrão)");
        // Se não for JSON, tenta o padrão (form-data)
        return super.attemptAuthentication(request, response);
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true) // Ignora campos extras no JSON
    private static class LoginRequest {
        
        @JsonAlias("email") // Aceita tanto "username" quanto "email" no JSON
        private String username;

        private String password;
    }
}
