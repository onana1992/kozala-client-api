package com.neobank.kozala_client.service;

import com.neobank.kozala_client.config.JwtProperties;
import com.neobank.kozala_client.entity.Client;
import com.neobank.kozala_client.entity.RefreshToken;
import com.neobank.kozala_client.repository.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";
    private static final String CLAIM_JTI = "jti";

    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    public String generateAccessToken(Client client) {
        SecretKey key = keyFrom(jwtProperties.getSecret());
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessExpirationMs());
        return Jwts.builder()
                .subject(String.valueOf(client.getId()))
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    @Transactional
    public String generateRefreshToken(Client client) {
        SecretKey key = refreshKey();
        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getRefreshExpirationMs());
        String token = Jwts.builder()
                .subject(String.valueOf(client.getId()))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .claim(CLAIM_JTI, jti)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
        RefreshToken entity = RefreshToken.builder()
                .jti(jti)
                .clientId(client.getId())
                .expiresAt(expiry.toInstant())
                .revoked(false)
                .build();
        refreshTokenRepository.save(entity);
        return token;
    }

    public Claims validateAccessToken(String token) {
        SecretKey key = keyFrom(jwtProperties.getSecret());
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new InvalidTokenException("Token invalide (type attendu: access)");
        }
        return claims;
    }

    public Claims validateRefreshToken(String token) {
        SecretKey key = refreshKey();
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new InvalidTokenException("Token invalide (type attendu: refresh)");
        }
        String jti = claims.get(CLAIM_JTI, String.class);
        RefreshToken stored = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new InvalidTokenException("Refresh token inconnu ou révoqué"));
        if (!stored.isValid()) {
            throw new InvalidTokenException("Refresh token révoqué ou expiré");
        }
        return claims;
    }

    @Transactional
    public void revokeRefreshTokenByJti(String jti) {
        refreshTokenRepository.findByJti(jti).ifPresent(rt -> {
            rt.setRevoked(true);
            rt.setRevokedAt(Instant.now());
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional
    public void revokeAllRefreshTokensForClient(Long clientId) {
        refreshTokenRepository.findByClientId(clientId).forEach(rt -> {
            rt.setRevoked(true);
            rt.setRevokedAt(Instant.now());
            refreshTokenRepository.save(rt);
        });
    }

    private SecretKey keyFrom(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret doit faire au moins 32 octets pour HS256");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    private SecretKey refreshKey() {
        String s = jwtProperties.getRefreshSecret() != null && !jwtProperties.getRefreshSecret().isEmpty()
                ? jwtProperties.getRefreshSecret()
                : jwtProperties.getSecret();
        return keyFrom(s);
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
}
