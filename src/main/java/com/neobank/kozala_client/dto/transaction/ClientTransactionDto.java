package com.neobank.kozala_client.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientTransactionDto {

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
}
