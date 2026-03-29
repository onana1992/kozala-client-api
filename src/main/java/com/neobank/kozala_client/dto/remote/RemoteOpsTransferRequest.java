package com.neobank.kozala_client.dto.remote;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Corps JSON pour {@code POST /api/client/transfers/person} sur le core : le compte crédité est résolu
 * côté core via {@code app.client-transfer.default-credit-product-code}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RemoteOpsTransferRequest {

    private Long fromAccountId;
    /** Client destinataire inscrit (id en base core) : le core choisit son compte produit courant standard. */
    private Long toClientId;
    private BigDecimal amount;
    private String currency;
    private String description;
}
