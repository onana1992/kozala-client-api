package com.neobank.kozala_client.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientPaymentMethodInfoDto {

    private Long id;
    private String code;
    private String name;
    private Boolean isActive;
    private String createdAt;
    private String updatedAt;
}
