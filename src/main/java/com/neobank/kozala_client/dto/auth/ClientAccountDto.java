package com.neobank.kozala_client.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Compte bancaire renvoyé au mobile (synthèse après appel GET /api/client/accounts distant). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientAccountDto {

    private Long id;
    private String accountNumber;
    private String productCode;
    private String productName;
    /** Description commerciale du produit (API distante). */
    private String productDescription;
    /** Ex. CURRENT_ACCOUNT, SAVINGS_ACCOUNT (pour l’icône / regroupement côté app). */
    private String productCategory;
    /** Statut du compte côté core (ex. ACTIVE, CLOSED). */
    private String status;
    /** Date/heure d’ouverture (ISO 8601 renvoyée par l’API distante). */
    private String openedAt;
    private String currency;
    private BigDecimal balance;
    private BigDecimal effectiveAvailableBalance;
}
