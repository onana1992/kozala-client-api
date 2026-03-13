package com.neobank.kozala_client.dto.beneficiary;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeneficiaryResponse {

    private Long id;
    private String phone;
    private String fullName;
    /** Id du client inscrit si le bénéficiaire est un utilisateur de la plateforme. */
    private Long beneficiaryClientId;
    private LocalDateTime createdAt;
}
