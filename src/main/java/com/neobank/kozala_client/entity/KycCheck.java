package com.neobank.kozala_client.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_checks", indexes = {
        @Index(name = "idx_kyc_checks_client", columnList = "client_id"),
        @Index(name = "idx_kyc_checks_type", columnList = "type"),
        @Index(name = "idx_kyc_checks_result", columnList = "result")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false, foreignKey = @ForeignKey(name = "fk_kyc_checks_client"))
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycCheckType type;

    @Column(length = 100)
    private String provider;

    @Column(name = "request_ref", length = 100)
    private String requestRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycCheckResult result;

    private Integer score;

    @Column(name = "raw_json", columnDefinition = "LONGTEXT")
    private String rawJson;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    @PrePersist
    protected void onCreate() {
        if (checkedAt == null) {
            checkedAt = LocalDateTime.now();
        }
    }
}
