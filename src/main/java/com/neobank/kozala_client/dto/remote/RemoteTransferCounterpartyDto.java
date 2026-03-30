package com.neobank.kozala_client.dto.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Contrepartie virement renvoyée par le core sur le détail transaction.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteTransferCounterpartyDto {

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

    /** Chemin fichier côté core (décliné en URL par le client-api). */
    private String profilePhotoPath;
}
