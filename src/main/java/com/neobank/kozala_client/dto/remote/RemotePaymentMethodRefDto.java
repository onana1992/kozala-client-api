package com.neobank.kozala_client.dto.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** Référence moyen de paiement (imbriqué dans {@link RemoteAccountPaymentMethodLinkDto}). */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemotePaymentMethodRefDto {

    private Long id;
    private String code;
    private String name;
    private Boolean isActive;
    private String createdAt;
    private String updatedAt;
}
