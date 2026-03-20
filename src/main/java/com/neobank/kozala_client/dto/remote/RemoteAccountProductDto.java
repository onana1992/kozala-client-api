package com.neobank.kozala_client.dto.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/** Produit bancaire tel que renvoyé dans {@link RemoteBankAccountDto#product}. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteAccountProductDto {

    private Long id;
    private String code;
    private String name;
    private String description;
    private String category;
    private String status;
    private String currency;
    private BigDecimal minBalance;
    private BigDecimal maxBalance;
    private BigDecimal defaultInterestRate;
    private String createdAt;
    private String updatedAt;
    private Long createdBy;
}
