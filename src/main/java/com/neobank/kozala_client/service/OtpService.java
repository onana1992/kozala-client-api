package com.neobank.kozala_client.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * Stockage des codes OTP dans Redis (phone -> code, TTL 5 min).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OtpService {

    private static final String KEY_PREFIX = "otp:";
    private static final int OTP_LENGTH = 6;
    private static final long OTP_VALIDITY_MINUTES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    public String generateAndStore(String phone) {
        String code = generateCode();
        String key = KEY_PREFIX + normalizePhone(phone);
        redisTemplate.opsForValue().set(key, code, OTP_VALIDITY_MINUTES, TimeUnit.MINUTES);
        log.info("OTP generated for phone {} : code={}", maskPhone(phone), code);
        return code;
    }

    public boolean validate(String phone, String code) {
        String key = KEY_PREFIX + normalizePhone(phone);
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            return false;
        }
        boolean valid = stored.equals(code);
        if (valid) {
            redisTemplate.delete(key);
        }
        return valid;
    }

    public void remove(String phone) {
        redisTemplate.delete(KEY_PREFIX + normalizePhone(phone));
    }

    /**
     * Génère un code OTP à 6 chiffres de façon aléatoire et sécurisée (SecureRandom).
     * Chaque chiffre est compris entre 0 et 9.
     */
    private String generateCode() {
        StringBuilder sb = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Normalise un numéro de téléphone au format E.164 pour le Cameroun (+237).
     * - Garde uniquement les chiffres, puis ajoute l'indicatif +237 si absent.
     * - Ex. "600 00 00 00" ou "237600000000" → "+237600000000".
     */
    public static String normalizePhone(String phone) {
        if (phone == null) return "";
        // Ne garder que les chiffres
        String digits = phone.replaceAll("\\D", "");
        // Déjà avec indicatif 237 (9 chiffres ou plus) → préfixer par +
        if (digits.startsWith("237") && digits.length() >= 9) {
            return "+" + digits;
        }
        // 9 chiffres sans indicatif → considérer comme numéro camerounais (+237)
        if (digits.length() == 9 && !digits.startsWith("237")) {
            return "+237" + digits;
        }
        // Sinon retourner la chaîne nettoyée (trim)
        return phone.trim();
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return phone.substring(0, 3) + "***" + phone.substring(phone.length() - 2);
    }
}
