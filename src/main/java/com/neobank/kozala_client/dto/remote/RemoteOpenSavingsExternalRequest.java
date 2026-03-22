package com.neobank.kozala_client.dto.remote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Corps JSON pour POST /api/client/accounts/open-savings sur l’API distante.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoteOpenSavingsExternalRequest {

    private Long clientId;
    private String savingsAccountProductCode;
    private BigDecimal openingAmount;
    private String currency;
    private Long sourceAccountId;
    private String accountLabel;
}
