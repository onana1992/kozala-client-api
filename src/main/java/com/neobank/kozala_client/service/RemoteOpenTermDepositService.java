package com.neobank.kozala_client.service;

import com.neobank.kozala_client.config.RemoteApiConfig;
import com.neobank.kozala_client.config.RemoteApiProperties;
import com.neobank.kozala_client.dto.auth.OpenTermDepositRequest;
import com.neobank.kozala_client.dto.remote.RemoteBankAccountDto;
import com.neobank.kozala_client.dto.remote.RemoteOpenTermDepositExternalRequest;
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
public class RemoteOpenTermDepositService {

    private static final String OPEN_TERM_DEPOSIT_PATH = "/api/client/accounts/open-term-deposit";
    private static final String DEFAULT_CURRENCY = "XAF";
    private static final String MISSING_BEARER =
            "Configuration manquante : définissez app.remote-api.bearer-token pour appeler l’API bancaire.";

    @Qualifier(RemoteApiConfig.REMOTE_API_REST_CLIENT)
    private final RestClient remoteApiRestClient;
    private final RemoteApiProperties remoteApiProperties;

    /**
     * Ouverture dépôt à terme sur l’API distante (corps : clientId, termDepositProductCode, periodId, …).
     * Authentification : uniquement {@code app.remote-api.bearer-token}.
     */
    public RemoteBankAccountDto open(OpenTermDepositRequest request, long clientId) {
        if (!StringUtils.hasText(remoteApiProperties.getBearerToken())) {
            throw new RemoteOpenTermDepositException(MISSING_BEARER, null);
        }
        RemoteOpenTermDepositExternalRequest body = toExternalRequest(request, clientId);
        try {
            RemoteBankAccountDto r = remoteApiRestClient.post()
                    .uri(OPEN_TERM_DEPOSIT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(RemoteBankAccountDto.class);
            return r != null ? r : new RemoteBankAccountDto();
        } catch (RestClientResponseException e) {
            log.warn("remote POST {} → HTTP {} body={}",
                    OPEN_TERM_DEPOSIT_PATH, e.getStatusCode(), truncate(e.getResponseBodyAsString(), 400));
            throw new RemoteOpenTermDepositException(
                    RemoteRestClientErrorSupport.extractRemoteErrorMessage(e),
                    e);
        } catch (Exception e) {
            log.warn("remote POST {} erreur: {}", OPEN_TERM_DEPOSIT_PATH, e.getMessage());
            throw new RemoteOpenTermDepositException(
                    "L’ouverture du dépôt à terme a échoué. Réessayez plus tard.",
                    e);
        }
    }

    private static RemoteOpenTermDepositExternalRequest toExternalRequest(OpenTermDepositRequest request, long clientId) {
        String currency = StringUtils.hasText(request.getCurrency())
                ? request.getCurrency().trim()
                : DEFAULT_CURRENCY;
        return RemoteOpenTermDepositExternalRequest.builder()
                .clientId(clientId)
                .termDepositProductCode(request.getProductCode().trim())
                .periodId(request.getTermPeriodId())
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
