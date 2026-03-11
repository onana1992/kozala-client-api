package com.neobank.kozala_client.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Appareils de confiance par client. Redis : trusted:clientId:deviceId -> "1", TTL 90 jours.
 * Un appareil déjà marqué comme confiance ne déclenche pas l'OTP au login.
 */
@Component
@RequiredArgsConstructor
public class TrustedDeviceStore {

    private static final String KEY_PREFIX = "trusted:";
    private static final long TRUST_DAYS = 90;

    private final StringRedisTemplate redisTemplate;

    public boolean isTrusted(Long clientId, String deviceId) {
        if (clientId == null || deviceId == null || deviceId.isBlank()) return false;
        String key = KEY_PREFIX + clientId + ":" + deviceId.trim();
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void markTrusted(Long clientId, String deviceId) {
        if (clientId == null || deviceId == null || deviceId.isBlank()) return;
        String key = KEY_PREFIX + clientId + ":" + deviceId.trim();
        redisTemplate.opsForValue().set(key, "1", TRUST_DAYS, TimeUnit.DAYS);
    }

    /** Supprime tous les appareils de confiance du client (ex. après réinitialisation du mot de passe). */
    public void removeAllForClient(Long clientId) {
        if (clientId == null) return;
        String pattern = KEY_PREFIX + clientId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
