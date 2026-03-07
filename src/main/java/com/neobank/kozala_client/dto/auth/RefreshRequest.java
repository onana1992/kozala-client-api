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
public class RefreshRequest {

    @NotBlank(message = "Le refresh token est obligatoire")
    private String refreshToken;
}
