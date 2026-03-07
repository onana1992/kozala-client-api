package com.neobank.kozala_client.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    /** Durée de vie de l'access token en secondes. */
    private long expiresIn;
    /** Durée de vie du refresh token en secondes. */
    private long refreshExpiresIn;
}
