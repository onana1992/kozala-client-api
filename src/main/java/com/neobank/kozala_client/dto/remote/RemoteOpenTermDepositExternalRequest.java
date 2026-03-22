package com.neobank.kozala_client.dto.remote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Corps JSON pour POST /api/client/accounts/open-term-deposit sur l’API distante.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoteOpenTermDepositExternalRequest {

    private Long clientId;
    private String termDepositProductCode;
    private Long periodId;
    private BigDecimal openingAmount;
    private String currency;
    private Long sourceAccountId;
    private String accountLabel;
}
