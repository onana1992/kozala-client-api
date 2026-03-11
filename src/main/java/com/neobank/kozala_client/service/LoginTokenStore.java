package com.neobank.kozala_client.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Token de connexion en attente d'OTP (nouvel appareil). Redis : login:token -> clientId, TTL 5 min.
 */
@Component
@RequiredArgsConstructor
public class LoginTokenStore {

    private static final String KEY_PREFIX = "login:";
    private static final long TOKEN_VALIDITY_MINUTES = 5;

    private final StringRedisTemplate redisTemplate;

    public String createToken(Long clientId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = KEY_PREFIX + token;
        redisTemplate.opsForValue().set(key, String.valueOf(clientId), TOKEN_VALIDITY_MINUTES, TimeUnit.MINUTES);
        return token;
    }

    /** Récupère le clientId sans supprimer le token (pour renvoyer l'OTP). */
    public Optional<Long> getClientId(String token) {
        String key = KEY_PREFIX + token;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) return Optional.empty();
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** Récupère le clientId et supprime le token (usage unique). */
    public Optional<Long> getAndRemove(String token) {
        String key = KEY_PREFIX + token;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) return Optional.empty();
        redisTemplate.delete(key);
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
