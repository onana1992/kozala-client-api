package com.neobank.kozala_client.dto.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Corps d’une transaction telle que sérialisée par le core ({@code GET /api/client/transactions}).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteTransactionDto {

    private Long id;
    private String transactionNumber;
    private String type;
    private String status;
    private BigDecimal amount;
    private String currency;
    private Long accountId;
    private String referenceType;
    private Long referenceId;
    private String description;
    private String metadata;
    private LocalDate valueDate;
    private Instant transactionDate;
    private Long createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    /** Présent sur GET détail transaction pour les virements (TRANSFER). */
    private RemoteTransferCounterpartyDto transferCounterparty;
}
