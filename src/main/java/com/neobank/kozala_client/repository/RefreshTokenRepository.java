package com.neobank.kozala_client.repository;

import com.neobank.kozala_client.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByJti(String jti);

    List<RefreshToken> findByClientId(Long clientId);

    void deleteByExpiresAtBefore(Instant instant);
}
