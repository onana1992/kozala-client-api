package com.neobank.kozala_client.dto.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** Lien compte ↔ moyen de paiement (élément de {@code paymentMethods} sur GET /api/client/accounts). */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteAccountPaymentMethodLinkDto {

    private Long id;
    private RemotePaymentMethodRefDto paymentMethod;
    private Boolean allowedDeposit;
    private Boolean allowedWithdrawal;
    private Boolean allowedLoanRepayment;
    private Integer displayOrder;
    private String createdAt;
    private String updatedAt;
}
