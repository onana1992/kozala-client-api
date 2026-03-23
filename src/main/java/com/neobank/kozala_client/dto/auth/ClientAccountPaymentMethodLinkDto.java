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
public class ClientAccountPaymentMethodLinkDto {

    private Long id;
    private ClientPaymentMethodInfoDto paymentMethod;
    private Boolean allowedDeposit;
    private Boolean allowedWithdrawal;
    private Boolean allowedLoanRepayment;
    private Integer displayOrder;
    private String createdAt;
    private String updatedAt;
}
