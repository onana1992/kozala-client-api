package com.neobank.kozala_client.service;

import com.neobank.kozala_client.config.RemoteApiConfig;
import com.neobank.kozala_client.config.RemoteApiProperties;
import com.neobank.kozala_client.dto.auth.OpenSavingsRequest;
import com.neobank.kozala_client.dto.remote.RemoteBankAccountDto;
import com.neobank.kozala_client.dto.remote.RemoteOpenSavingsExternalRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
@Slf4j
@RequiredArgsConstructor
public class RemoteOpenSavingsService {

    private static final String OPEN_SAVINGS_PATH = "/api/client/accounts/open-savings";
    private static final String DEFAULT_CURRENCY = "XAF";
    private static final String MISSING_BEARER =
            "Configuration manquante : définissez app.remote-api.bearer-token pour appeler l’API bancaire.";

    @Qualifier(RemoteApiConfig.REMOTE_API_REST_CLIENT)
    private final RestClient remoteApiRestClient;
    private final RemoteApiProperties remoteApiProperties;

    /**
     * Ouverture compte épargne sur l’API distante (corps : clientId, savingsAccountProductCode, …).
     * Authentification : uniquement {@code app.remote-api.bearer-token} (en-tête par défaut du RestClient).
     */
    public RemoteBankAccountDto open(OpenSavingsRequest request, long clientId) {
        if (!StringUtils.hasText(remoteApiProperties.getBearerToken())) {
            throw new RemoteOpenSavingsException(MISSING_BEARER, null);
        }
        RemoteOpenSavingsExternalRequest body = toExternalRequest(request, clientId);
        try {
            RemoteBankAccountDto r = remoteApiRestClient.post()
                    .uri(OPEN_SAVINGS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(RemoteBankAccountDto.class);
            return r != null ? r : new RemoteBankAccountDto();
        } catch (RestClientResponseException e) {
            log.warn("remote POST {} → HTTP {} body={}",
                    OPEN_SAVINGS_PATH, e.getStatusCode(), truncate(e.getResponseBodyAsString(), 400));
            throw new RemoteOpenSavingsException(
                    RemoteRestClientErrorSupport.extractRemoteErrorMessage(e),
                    e);
        } catch (Exception e) {
            log.warn("remote POST {} erreur: {}", OPEN_SAVINGS_PATH, e.getMessage());
            throw new RemoteOpenSavingsException(
                    "L’ouverture du compte épargne a échoué. Réessayez plus tard.",
                    e);
        }
    }

    private static RemoteOpenSavingsExternalRequest toExternalRequest(OpenSavingsRequest request, long clientId) {
        String currency = StringUtils.hasText(request.getCurrency())
                ? request.getCurrency().trim()
                : DEFAULT_CURRENCY;
        return RemoteOpenSavingsExternalRequest.builder()
                .clientId(clientId)
                .savingsAccountProductCode(request.getProductCode().trim())
                .openingAmount(request.getInitialAmount())
                .currency(currency)
                .sourceAccountId(request.getDebitAccountId())
                .accountLabel(request.getAccountLabel().trim())
                .build();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
