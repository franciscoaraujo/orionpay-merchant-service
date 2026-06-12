package orionpay.merchant.infrastructure.adapters.output.persistence.reposittory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orionpay.merchant.infrastructure.adapters.output.persistence.entity.RefreshTokenEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaRefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByToken(String token);
    List<RefreshTokenEntity> findByUserIdAndRevokedFalse(UUID userId);
}

