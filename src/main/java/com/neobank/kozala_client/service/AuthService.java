package com.neobank.kozala_client.service;

import com.neobank.kozala_client.config.JwtProperties;
import com.neobank.kozala_client.dto.auth.*;
import com.neobank.kozala_client.entity.Client;
import com.neobank.kozala_client.entity.ClientStatus;
import com.neobank.kozala_client.entity.ClientType;
import com.neobank.kozala_client.repository.ClientRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final OtpService otpService;
    private final SignupTokenStore signupTokenStore;

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

    // ---------- Signup (Redis : OTP + signup tokens) ----------

    /**
     * Envoie un code OTP au numéro. Le code est stocké dans Redis (clé otp:phone, TTL 5 min).
     * À brancher : envoi SMS (Twilio, etc.).
     */
    public SendOtpResponse sendOtp(String phone) {
        String normalized = OtpService.normalizePhone(phone);
        otpService.generateAndStore(normalized);
        // TODO: envoyer SMS (Twilio, etc.). En dev le code est loggé par OtpService.
        return SendOtpResponse.builder().success(true).build();
    }

    /**
     * Vérifie le code OTP (Redis), puis crée un signup token stocké dans Redis (TTL 30 min).
     */
    public VerifyOtpResponse verifyOtp(String phone, String code) {
        String normalized = OtpService.normalizePhone(phone);
        if (!otpService.validate(normalized, code)) {
            throw new InvalidOtpException("Code invalide ou expiré");
        }
        String signupToken = signupTokenStore.createToken(normalized);
        return VerifyOtpResponse.builder().signupToken(signupToken).build();
    }

    /**
     * Complète l'inscription avec le signup token (Redis). Crée ou met à jour le Client,
     * puis associe le clientId au token dans Redis.
     */
    @Transactional
    public void completeSignup(String signupToken, String firstName, String lastName, String gender, String birthDate) {
        String phone = signupTokenStore.getPhone(signupToken)
                .orElseThrow(() -> new InvalidSignupTokenException("Token d'inscription invalide ou expiré"));

        Client client = clientRepository.findByPhone(phone).orElse(null);
        if (client == null) {
            String displayName = (firstName + " " + lastName).trim();
            String email = "signup-" + UUID.randomUUID() + "@temp.kozala.local";
            client = Client.builder()
                    .type(ClientType.PERSON)
                    .displayName(displayName.isEmpty() ? "Client" : displayName)
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .phone(phone)
                    .passwordHash(null)
                    .status(ClientStatus.DRAFT)
                    .pepFlag(false)
                    .build();
            client = clientRepository.save(client);
        } else {
            client.setFirstName(firstName);
            client.setLastName(lastName);
            client.setDisplayName((firstName + " " + lastName).trim());
            clientRepository.save(client);
        }
        signupTokenStore.associateClientId(signupToken, client.getId());
    }

    /**
     * Définit le mot de passe du compte. Valide le signup token (Redis), récupère le client,
     * met à jour le hash, supprime le token Redis, puis émet les tokens JWT de session.
     */
    @Transactional
    public AuthResponse setPassword(String signupToken, String password) {
        SignupTokenStore.SignupEntry entry = signupTokenStore.getAndRemove(signupToken)
                .orElseThrow(() -> new InvalidSignupTokenException("Token d'inscription invalide ou expiré"));

        Client client;
        if (entry.clientId() != null) {
            client = clientRepository.findById(entry.clientId())
                    .orElseThrow(() -> new InvalidSignupTokenException("Client introuvable"));
        } else {
            client = clientRepository.findByPhone(entry.phone())
                    .orElseThrow(() -> new InvalidSignupTokenException("Client introuvable"));
        }

        client.setPasswordHash(passwordEncoder.encode(password));
        client.setStatus(ClientStatus.DRAFT); // ou PENDING_REVIEW selon la règle métier
        clientRepository.save(client);

        signupTokenStore.remove(signupToken);

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

    public static class BadCredentialsException extends RuntimeException {
        public BadCredentialsException(String message) {
            super(message);
        }
    }

    public static class InvalidOtpException extends RuntimeException {
        public InvalidOtpException(String message) {
            super(message);
        }
    }

    public static class InvalidSignupTokenException extends RuntimeException {
        public InvalidSignupTokenException(String message) {
            super(message);
        }
    }
}
