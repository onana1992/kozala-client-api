package com.neobank.kozala_client.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Stockage des signup tokens dans Redis (token -> phone + clientId, TTL 30 min).
 */
@Component
@RequiredArgsConstructor
public class SignupTokenStore {

    private static final String KEY_PREFIX = "signup:";
    private static final long SIGNUP_TOKEN_VALIDITY_MINUTES = 30;
    private static final String SEP = "|";

    private final StringRedisTemplate redisTemplate;

    public String createToken(String phone) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = KEY_PREFIX + token;
        redisTemplate.opsForValue().set(key, phone + SEP, SIGNUP_TOKEN_VALIDITY_MINUTES, TimeUnit.MINUTES);
        return token;
    }

    public void associateClientId(String token, Long clientId) {
        String key = KEY_PREFIX + token;
        String existing = redisTemplate.opsForValue().get(key);
        if (existing != null) {
            String phone = existing.contains(SEP) ? existing.substring(0, existing.indexOf(SEP)) : existing;
            redisTemplate.opsForValue().set(key, phone + SEP + clientId, SIGNUP_TOKEN_VALIDITY_MINUTES, TimeUnit.MINUTES);
        }
    }

    public Optional<String> getPhone(String token) {
        return getEntry(token).map(SignupEntry::phone);
    }

    public Optional<Long> getClientId(String token) {
        return getEntry(token).map(SignupEntry::clientId);
    }

    /**
     * Lit le token sans le supprimer (ex. set-password : réessai si l’API comptes distante échoue).
     */
    public Optional<SignupEntry> readSignupEntry(String token) {
        return getEntry(token);
    }

    public Optional<SignupEntry> getAndRemove(String token) {
        String key = KEY_PREFIX + token;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        redisTemplate.delete(key);
        return parseEntry(value).map(entry -> new SignupEntry(entry.phone(), entry.clientId(), System.currentTimeMillis() + SIGNUP_TOKEN_VALIDITY_MINUTES * 60 * 1000));
    }

    public void remove(String token) {
        redisTemplate.delete(KEY_PREFIX + token);
    }

    private Optional<SignupEntry> getEntry(String token) {
        String key = KEY_PREFIX + token;
        String value = redisTemplate.opsForValue().get(key);
        return parseEntry(value);
    }

    private Optional<SignupEntry> parseEntry(String value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        int sep = value.indexOf(SEP);
        String phone = sep >= 0 ? value.substring(0, sep) : value;
        Long clientId = null;
        if (sep >= 0 && sep < value.length() - 1) {
            String rest = value.substring(sep + 1).trim();
            if (!rest.isEmpty()) {
                try {
                    clientId = Long.parseLong(rest);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return Optional.of(new SignupEntry(phone, clientId, 0L));
    }

    public record SignupEntry(String phone, Long clientId, long expiresAt) {}
}
