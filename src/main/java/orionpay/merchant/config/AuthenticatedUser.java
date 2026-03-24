package orionpay.merchant.config;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import orionpay.merchant.domain.model.enums.UserRole;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.AuthUserEntity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class AuthenticatedUser implements UserDetails {

    private final UUID userId;
    private final UUID merchantId;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final UserRole role;
    private final List<GrantedAuthority> authorities;

    public AuthenticatedUser(AuthUserEntity user) {
        this.userId = user.getId();
        this.merchantId = user.getMerchantId();
        this.username = user.getEmail();
        this.password = user.getPasswordHash();
        this.enabled = user.isEnabled();
        this.role = user.getRole();
        this.authorities = List.of(new SimpleGrantedAuthority(user.getRole().name()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}

