package com.neobank.kozala_client.dto.beneficiary;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddBeneficiaryRequest {

    @NotBlank(message = "Le numéro de téléphone est requis")
    private String phone;

    @NotBlank(message = "Le nom est requis")
    private String fullName;
}
