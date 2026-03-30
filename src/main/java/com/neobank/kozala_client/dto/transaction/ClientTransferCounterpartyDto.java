package com.neobank.kozala_client.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientTransferCounterpartyDto {

    private String partyRole;
    private Long counterpartyClientId;
    private String displayName;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private Long counterpartyAccountId;
    private String accountNumber;
    private String accountLabel;
    private String productName;

    /** URL relative vers {@code GET /api/profile/photos?key=…} (kozala-client-api). */
    private String profilePhotoUrl;
}
