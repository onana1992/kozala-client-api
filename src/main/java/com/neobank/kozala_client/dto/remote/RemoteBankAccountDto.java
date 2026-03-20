package com.neobank.kozala_client.dto.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Compte (courant ou épargne) dans la réponse de
 * POST /api/client/accounts/open-checking-and-savings.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteBankAccountDto {

    private Long id;
    private Long clientId;
    private Long productId;
    private RemoteAccountProductDto product;
    private String accountNumber;
    private String status;
    private String currency;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private BigDecimal openingAmount;
    private BigDecimal interestRate;
    private Long periodId;
    private String maturityDate;
    private String openedAt;
    private String closedAt;
    private String closedReason;
    private Long openedBy;
    private String disbursedAt;
    private String createdAt;
    private String updatedAt;
    private BigDecimal effectiveAvailableBalance;
    private Integer periodMonths;
}
