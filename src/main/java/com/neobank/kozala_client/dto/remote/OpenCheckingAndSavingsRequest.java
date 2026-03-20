package com.neobank.kozala_client.dto.remote;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenCheckingAndSavingsRequest {

    private long clientId;

    @JsonProperty("currentAccountProductCode")
    private String currentAccountProductCode;

    @JsonProperty("savingsAccountProductCode")
    private String savingsAccountProductCode;
}
