package com.neobank.kozala_client.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Réponse du login : soit tokens (appareil déjà de confiance), soit demande OTP (nouvel appareil).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    /** True si un code OTP doit être saisi (nouvel appareil). */
    private boolean requiresOtp;
    /** Token à envoyer à verify-login-otp avec le code (quand requiresOtp). */
    private String loginToken;
    /** Validité du code OTP en secondes (quand requiresOtp). */
    private Long otpExpiresIn;

    /** Champs Auth (quand !requiresOtp). */
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private Long refreshExpiresIn;
    private String displayName;
    private String phone;
    private String profilePhotoUrl;
    /** Comptes distants (même sémantique que {@link AuthResponse#getAccounts()}). */
    private List<ClientAccountDto> accounts;
}
