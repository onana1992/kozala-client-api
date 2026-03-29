package com.neobank.kozala_client.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonTransferResponse {

    private Long id;
    private String transferNumber;
    private String status;
    private BigDecimal amount;
    private String currency;
}
