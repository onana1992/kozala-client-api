package com.neobank.kozala_client.dto.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Réponse brute du core pour un transfert créé (champs inconnus ignorés). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteOpsTransferResponseDto {

    private Long id;
    private String transferNumber;
    private String status;
    private BigDecimal amount;
    private String currency;
}
