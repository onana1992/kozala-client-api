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

    /** Statut revue email : pending, approved. Dès que le code est vérifié → approved. */
    private String emailReviewStatus;
    /** Statut revue profil : pending, pending_review, approved, rejected. */
    private String profileReviewStatus;
    /** Statut revue identité (= Client.status) : pending, pending_review, approved, rejected. */
    private String identityReviewStatus;
}
