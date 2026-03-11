package com.neobank.kozala_client.dto.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyEmailCodeRequest {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    private String email;

    @NotBlank(message = "Le code est obligatoire")
    @Pattern(regexp = "^[0-9]{6}$", message = "Le code doit contenir 6 chiffres")
    private String code;
}
