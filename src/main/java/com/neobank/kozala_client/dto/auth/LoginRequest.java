package com.neobank.kozala_client.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    private String phone;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}
