package com.neobank.kozala_client.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clients", indexes = {
        @Index(name = "idx_clients_email", columnList = "email"),
        @Index(name = "idx_clients_phone", columnList = "phone"),
        @Index(name = "idx_clients_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClientType type;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "first_name", length = 150)
    private String firstName;

    @Column(name = "last_name", length = 150)
    private String lastName;

    /** Genre: male, female */
    @Column(length = 20)
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    /** Situation familiale: single, married, divorced, widowed */
    @Column(name = "marital_status", length = 20)
    private String maritalStatus;

    /** Null jusqu'à vérification email (étape vérification). */
    @Column(unique = true, length = 255)
    private String email;

    @Column(nullable = false, unique = true, length = 50)
    private String phone;

    /** Hash BCrypt du mot de passe pour l'authentification (un client = un compte de connexion). */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ClientStatus status = ClientStatus.DRAFT;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "pep_flag", nullable = false)
    @Builder.Default
    private Boolean pepFlag = false;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /** Nom du fichier photo de profil stocké (ex: uuid.jpg). Null si pas de photo. */
    @Column(name = "profile_photo_path", length = 255)
    private String profilePhotoPath;

    /** Revue email : PENDING puis APPROVED dès que le code email est vérifié (pas de revue manuelle). */
    @Enumerated(EnumType.STRING)
    @Column(name = "email_review_status", nullable = false, length = 20)
    @Builder.Default
    private ReviewStatus emailReviewStatus = ReviewStatus.PENDING;

    /** Revue profil : PENDING → PENDING_REVIEW quand profil complété → APPROVED/REJECTED par KYC. */
    @Enumerated(EnumType.STRING)
    @Column(name = "profile_review_status", nullable = false, length = 20)
    @Builder.Default
    private ReviewStatus profileReviewStatus = ReviewStatus.PENDING;

    /** Revue identité (documents + selfie) : PENDING_REVIEW après selfie OK → APPROVED/REJECTED par reviewer. Dissocié de status (client accepté = manuel). */
    @Enumerated(EnumType.STRING)
    @Column(name = "identity_review_status", nullable = false, length = 20)
    @Builder.Default
    private ReviewStatus identityReviewStatus = ReviewStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RelatedPerson> relatedPersons = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<KycCheck> kycChecks = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Met à jour status à partir des statuts de revue (email, profil, identité).
     * PENDING_REVIEW si email=APPROVED et profile et identity sont PENDING_REVIEW ; sinon DRAFT.
     * Ne modifie pas le status si le client est déjà VERIFIED, REJECTED ou BLOCKED (décision manuelle).
     */
    public void updateStatusFromReviewStatuses() {
        if (status == ClientStatus.VERIFIED || status == ClientStatus.REJECTED || status == ClientStatus.BLOCKED) {
            return;
        }
        boolean emailApproved = emailReviewStatus != null && emailReviewStatus == ReviewStatus.APPROVED;
        boolean profilePendingReview = profileReviewStatus != null && profileReviewStatus == ReviewStatus.PENDING_REVIEW;
        boolean identityPendingReview = identityReviewStatus != null && identityReviewStatus == ReviewStatus.PENDING_REVIEW;
        if (emailApproved && profilePendingReview && identityPendingReview) {
            setStatus(ClientStatus.PENDING_REVIEW);
        } else {
            setStatus(ClientStatus.DRAFT);
        }
    }
}
