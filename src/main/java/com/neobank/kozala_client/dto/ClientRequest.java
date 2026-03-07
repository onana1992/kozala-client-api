package com.neobank.kozala_client.dto;

import com.neobank.kozala_client.entity.ClientType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientRequest {

    @NotNull(message = "Le type est obligatoire")
    private ClientType type;

    @NotBlank(message = "Le nom d'affichage est obligatoire")
    private String displayName;

    private String firstName;
    private String lastName;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    private String email;

    @NotBlank(message = "Le téléphone est obligatoire")
    private String phone;
}
