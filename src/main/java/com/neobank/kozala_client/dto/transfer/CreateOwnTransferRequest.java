package com.neobank.kozala_client.dto.transfer;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOwnTransferRequest {

    @NotNull(message = "Le compte à débiter est obligatoire")
    private Long fromAccountId;

    @NotNull(message = "Le compte à créditer est obligatoire")
    private Long toAccountId;

    @NotNull(message = "Le montant est obligatoire")
    @Positive(message = "Le montant doit être strictement positif")
    private BigDecimal amount;

    private String currency;

    private String description;
}
