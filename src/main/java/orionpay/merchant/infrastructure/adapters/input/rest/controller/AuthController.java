package orionpay.merchant.infrastructure.adapters.input.rest.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import orionpay.merchant.config.AuthenticatedUser;
import orionpay.merchant.domain.service.AuthenticationService;
import orionpay.merchant.domain.service.RegisterMerchantUseCase;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.LoginRequest;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.MerchantRegistrationRequest;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.RefreshTokenRequest;
import orionpay.merchant.infrastructure.adapters.input.rest.dto.TokenResponse;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final RegisterMerchantUseCase registerMerchantUseCase;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authenticationService.login(request, httpRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authenticationService.refresh(request));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody MerchantRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registerMerchantUseCase.execute(request));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Não autenticado"));
        }

        log.info("Usuário autenticado acessando /me: {}", user.getUsername());
        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "role", user.getRole().name(),
                "merchantId", user.getMerchantId(),
                "authorities", user.getAuthorities()
        ));
    }
}
