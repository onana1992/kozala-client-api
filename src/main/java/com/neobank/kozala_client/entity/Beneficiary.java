package com.neobank.kozala_client.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Bénéficiaire lié à un client (owner). Peut référencer un client inscrit (beneficiaryClientId)
 * ou être une entrée manuelle (phone + fullName uniquement).
 */
@Entity
@Table(name = "beneficiaries", indexes = {
        @Index(name = "idx_beneficiaries_owner", columnList = "owner_client_id"),
        @Index(name = "idx_beneficiaries_owner_phone", columnList = "owner_client_id, phone")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_client_id", nullable = false)
    private Long ownerClientId;

    /** Client inscrit correspondant (optionnel). Si présent, phone/fullName peuvent être dérivés du client. */
    @Column(name = "beneficiary_client_id")
    private Long beneficiaryClientId;

    /** Numéro normalisé (ex. +237600000000). Toujours renseigné. */
    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
