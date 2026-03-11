package com.neobank.kozala_client.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyLoginOtpRequest {

    @NotBlank(message = "Le token de connexion est obligatoire")
    private String loginToken;

    @NotBlank(message = "Le code OTP est obligatoire")
    @Pattern(regexp = "^[0-9]{6}$", message = "Le code doit contenir 6 chiffres")
    private String code;

    /** Identifiant de l'appareil à marquer comme de confiance (optionnel). */
    private String deviceId;
}
