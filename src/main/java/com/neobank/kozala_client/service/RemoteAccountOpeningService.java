package com.neobank.kozala_client.service;

import com.neobank.kozala_client.config.RemoteApiConfig;
import com.neobank.kozala_client.config.RemoteApiProperties;
import com.neobank.kozala_client.dto.auth.OpenedAccountsDto;
import com.neobank.kozala_client.dto.remote.OpenCheckingAndSavingsRemoteResponse;
import com.neobank.kozala_client.dto.remote.OpenCheckingAndSavingsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
@Slf4j
@RequiredArgsConstructor
public class RemoteAccountOpeningService {

    private static final String OPEN_ACCOUNTS_PATH = "/api/client/accounts/open-checking-and-savings";

    @Qualifier(RemoteApiConfig.REMOTE_API_REST_CLIENT)
    private final RestClient remoteApiRestClient;
    private final RemoteApiProperties remoteApiProperties;

    public OpenedAccountsDto openCheckingAndSavings(long clientId) {
        String currentCode = remoteApiProperties.getCurrentAccountProductCode();
        String savingsCode = remoteApiProperties.getSavingsAccountProductCode();
        if (!StringUtils.hasText(currentCode) || !StringUtils.hasText(savingsCode)) {
            throw new IllegalStateException(
                    "Codes produits comptes non configurés (app.remote-api.current-account-product-code / savings-account-product-code)");
        }

        OpenCheckingAndSavingsRequest body = OpenCheckingAndSavingsRequest.builder()
                .clientId(clientId)
                .currentAccountProductCode(currentCode.trim())
                .savingsAccountProductCode(savingsCode.trim())
                .build();

        try {
            OpenCheckingAndSavingsRemoteResponse remote = remoteApiRestClient.post()
                    .uri(OPEN_ACCOUNTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(OpenCheckingAndSavingsRemoteResponse.class);

            return map(remote);
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 401 && !StringUtils.hasText(remoteApiProperties.getBearerToken())) {
                log.warn("open-checking-and-savings 401: app.remote-api.bearer-token is empty — run with profile "
                        + "\"local\" (application-local.properties) or set REMOTE_API_BEARER_TOKEN");
            }
            log.warn("open-checking-and-savings failed clientId={} status={} body={}",
                    clientId, status, e.getResponseBodyAsString());
            throw new RemoteAccountOpeningException(
                    "Impossible d’ouvrir vos comptes pour le moment. Réessayez dans quelques instants.",
                    e);
        } catch (Exception e) {
            log.error("open-checking-and-savings error clientId={}", clientId, e);
            throw new RemoteAccountOpeningException(
                    "Impossible d’ouvrir vos comptes pour le moment. Réessayez dans quelques instants.",
                    e);
        }
    }

    private static OpenedAccountsDto map(OpenCheckingAndSavingsRemoteResponse r) {
        if (r == null) {
            return OpenedAccountsDto.builder().build();
        }
        var current = r.getCurrentAccount();
        var savings = r.getSavingsAccount();
        return OpenedAccountsDto.builder()
                .checkingAccountId(current != null ? current.getId() : null)
                .checkingAccountNumber(current != null ? current.getAccountNumber() : null)
                .savingsAccountId(savings != null ? savings.getId() : null)
                .savingsAccountNumber(savings != null ? savings.getAccountNumber() : null)
                .build();
    }
}
