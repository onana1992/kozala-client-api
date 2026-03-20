package com.neobank.kozala_client.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Résumé des comptes ouverts via l’API distante après inscription (set-password). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenedAccountsDto {

    private Long checkingAccountId;
    private Long savingsAccountId;
    private String checkingAccountNumber;
    private String savingsAccountNumber;
}
