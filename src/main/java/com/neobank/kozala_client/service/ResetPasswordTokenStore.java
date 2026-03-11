package com.neobank.kozala_client.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Stockage du token de réinitialisation mot de passe dans Redis (token -> phone, TTL 15 min).
 */
@Component
@RequiredArgsConstructor
public class ResetPasswordTokenStore {

    private static final String KEY_PREFIX = "reset:";
    private static final long TOKEN_VALIDITY_MINUTES = 15;

    private final StringRedisTemplate redisTemplate;

    public String createToken(String phone) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = KEY_PREFIX + token;
        redisTemplate.opsForValue().set(key, phone, TOKEN_VALIDITY_MINUTES, TimeUnit.MINUTES);
        return token;
    }

    public Optional<String> getPhone(String token) {
        String key = KEY_PREFIX + token;
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    /** Récupère le téléphone et supprime le token (usage unique). */
    public Optional<String> getAndRemove(String token) {
        String key = KEY_PREFIX + token;
        String phone = redisTemplate.opsForValue().get(key);
        if (phone == null) {
            return Optional.empty();
        }
        redisTemplate.delete(key);
        return Optional.of(phone);
    }
}
