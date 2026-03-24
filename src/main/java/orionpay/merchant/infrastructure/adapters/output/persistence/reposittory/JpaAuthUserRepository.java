package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.AuthUserEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaAuthUserRepository extends JpaRepository<AuthUserEntity, UUID> {
    Optional<AuthUserEntity> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}

