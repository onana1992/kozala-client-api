package com.neobank.kozala_client.service;

/**
 * Résultat de la comparaison document (visage) ↔ selfie.
 */
public enum FaceVerificationResult {
    /** Les deux visages correspondent (même personne). */
    MATCH,
    /** Aucun visage détecté sur la selfie. */
    NO_FACE_IN_SELFIE,
    /** Aucun visage détecté sur le document. */
    NO_FACE_IN_DOCUMENT,
    /** Visages détectés mais pas la même personne (similarité insuffisante). */
    NO_MATCH,
    /** Vérification désactivée (provider=none ou non configuré). */
    DISABLED
}
