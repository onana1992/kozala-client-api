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
    /** Nom d'affichage du client (pour l'onglet Compte). */
    private String displayName;
    /** Téléphone du client. */
    private String phone;
    /** URL relative de la photo de profil (ex: /api/profile/photos/uuid.jpg), null si aucune. */
    private String profilePhotoUrl;
}
