package com.neobank.kozala_client.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration Redis pour l'API Kozala.
 * <p>
 * Redis est utilisé pour les endpoints de signup :
 * <ul>
 *   <li><b>OTP</b> (OtpService) : clés {@code otp:{phone}}, TTL 5 min. Utilisé par send-otp et verify-otp.</li>
 *   <li><b>Signup tokens</b> (SignupTokenStore) : clés {@code signup:{token}}, TTL 30 min. Utilisé par complete-signup et set-password.</li>
 * </ul>
 * La connexion est configurée via {@code spring.data.redis.*} dans application.properties.
 * Le bean {@code StringRedisTemplate} est auto-configuré par Spring Boot (spring-boot-starter-data-redis).
 * <p>
 * Santé Redis : exposée via Actuator ({@code GET /actuator/health}, entrée {@code redis}).
 */
@Configuration
public class RedisConfig {
}
