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
public class ResendLoginOtpRequest {

    @NotBlank(message = "Le token de connexion est obligatoire")
    private String loginToken;
}
