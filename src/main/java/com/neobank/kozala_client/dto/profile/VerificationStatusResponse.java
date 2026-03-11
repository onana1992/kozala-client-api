package com.neobank.kozala_client.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationStatusResponse {

    /** Email vérifié (email réel, pas l'email temporaire signup). */
    private boolean emailVerified;
    /** Profil complété (prénom et nom renseignés). */
    private boolean profileCompleted;
    /** Vérification d'identité (KYC) complétée. */
    private boolean identityCompleted;
}
