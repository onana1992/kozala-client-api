package com.neobank.kozala_client.entity;

/**
 * Statut de revue KYC par dimension (email, profil, identité).
 * Pas de nouvelle table : stocké sur Client (email_review_status, profile_review_status).
 * Identité = Client.status (DRAFT, PENDING_REVIEW, VERIFIED, REJECTED).
 */
public enum ReviewStatus {
    PENDING,
    PENDING_REVIEW,
    APPROVED,
    REJECTED
}
