package com.neobank.kozala_client.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenSavingsRequest {

    @NotBlank
    private String productCode;

    @NotBlank
    private String accountLabel;

    @NotNull
    @PositiveOrZero
    private BigDecimal initialAmount;

    @NotNull
    private Long debitAccountId;
}
