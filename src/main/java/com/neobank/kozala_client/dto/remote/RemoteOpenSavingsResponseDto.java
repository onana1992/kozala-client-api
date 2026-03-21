package com.neobank.kozala_client.dto.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** Réponse distante POST ouverture compte épargne (OPENED / PENDING / FAILED). */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteOpenSavingsResponseDto {

    private String status;
    private String accountNumber;
    private String effectiveAt;
    private String message;
}
