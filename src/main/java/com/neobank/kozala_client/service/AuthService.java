package com.neobank.kozala_client.service;

import com.neobank.kozala_client.config.JwtProperties;
import com.neobank.kozala_client.dto.auth.AuthResponse;
import com.neobank.kozala_client.entity.Client;
import com.neobank.kozala_client.repository.ClientRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    /**
     * Connexion par téléphone + mot de passe. Émet access + refresh tokens et persiste le refresh (révocable).
     */
    @Transactional
    public AuthResponse login(String phone, String password) {
        Client client = clientRepository.findByPhone(phone)
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));
        if (client.getPasswordHash() == null || client.getPasswordHash().isEmpty()) {
            throw new BadCredentialsException("Identifiants invalides");
        }
        if (!passwordEncoder.matches(password, client.getPasswordHash())) {
            throw new BadCredentialsException("Identifiants invalides");
        }
        String accessToken = jwtService.generateAccessToken(client);
        String refreshToken = jwtService.generateRefreshToken(client);
        long expiresInSec = jwtProperties.getAccessExpirationMs() / 1000;
        long refreshExpiresInSec = jwtProperties.getRefreshExpirationMs() / 1000;
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(TOKEN_TYPE)
                .expiresIn(expiresInSec)
                .refreshExpiresIn(refreshExpiresInSec)
                .build();
    }

    /**
     * Rafraîchit l'access token avec le refresh token. Rotation : l'ancien refresh est révoqué, un nouveau est émis.
     */
    @Transactional
    public AuthResponse refresh(String refreshTokenValue) {
        Claims claims = jwtService.validateRefreshToken(refreshTokenValue);
        String jti = claims.get("jti", String.class);
        Long clientId = Long.parseLong(claims.getSubject());
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new JwtService.InvalidTokenException("Client introuvable"));
        jwtService.revokeRefreshTokenByJti(jti);
        String accessToken = jwtService.generateAccessToken(client);
        String newRefreshToken = jwtService.generateRefreshToken(client);
        long expiresInSec = jwtProperties.getAccessExpirationMs() / 1000;
        long refreshExpiresInSec = jwtProperties.getRefreshExpirationMs() / 1000;
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .tokenType(TOKEN_TYPE)
                .expiresIn(expiresInSec)
                .refreshExpiresIn(refreshExpiresInSec)
                .build();
    }

    /**
     * Révoque le refresh token fourni (logout).
     */
    @Transactional
    public void logout(String refreshTokenValue) {
        try {
            Claims claims = jwtService.validateRefreshToken(refreshTokenValue);
            String jti = claims.get("jti", String.class);
            jwtService.revokeRefreshTokenByJti(jti);
        } catch (JwtService.InvalidTokenException ignored) {
            // Déjà invalide ou révoqué : rien à faire
        }
    }

    /**
     * Révoque tous les refresh tokens du client (ex. changement de mot de passe).
     */
    @Transactional
    public void revokeAllForClient(Long clientId) {
        jwtService.revokeAllRefreshTokensForClient(clientId);
    }

    public static class BadCredentialsException extends RuntimeException {
        public BadCredentialsException(String message) {
            super(message);
        }
    }
}
