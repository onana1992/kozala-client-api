package com.neobank.kozala_client.dto.beneficiary;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LookupRegisteredRequest {

    /** Liste de numéros normalisés (ex. +237600000000). */
    @NotNull(message = "La liste des numéros est requise")
    private List<String> phones;
}
