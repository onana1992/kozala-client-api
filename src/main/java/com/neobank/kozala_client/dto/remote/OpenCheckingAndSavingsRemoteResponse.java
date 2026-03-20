package com.neobank.kozala_client.dto.remote;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Corps JSON renvoyé par POST /api/client/accounts/open-checking-and-savings.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenCheckingAndSavingsRemoteResponse {

    @JsonAlias("checkingAccount")
    private RemoteBankAccountDto currentAccount;

    private RemoteBankAccountDto savingsAccount;
}
