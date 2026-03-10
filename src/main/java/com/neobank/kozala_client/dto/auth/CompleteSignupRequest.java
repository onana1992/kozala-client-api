package com.neobank.kozala_client.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteSignupRequest {

    @NotBlank(message = "Le token d'inscription est obligatoire")
    private String signupToken;

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    @NotNull(message = "Le genre est obligatoire")
    private String gender; // "male" | "female"

    @NotBlank(message = "La date de naissance est obligatoire")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Format date attendu: YYYY-MM-DD")
    private String birthDate;
}
