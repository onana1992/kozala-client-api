package com.neobank.kozala_client.service;

import com.neobank.kozala_client.config.RemoteApiConfig;
import com.neobank.kozala_client.config.RemoteApiProperties;
import com.neobank.kozala_client.dto.auth.OpenTermDepositRequest;
import com.neobank.kozala_client.dto.remote.RemoteOpenSavingsResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
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

    @Qualifier(RemoteApiConfig.REMOTE_API_REST_CLIENT)
    private final RestClient remoteApiRestClient;
    private final RemoteApiProperties remoteApiProperties;

    public RemoteOpenSavingsResponseDto open(OpenTermDepositRequest body, String userAccessToken) {
        if (StringUtils.hasText(remoteApiProperties.getBearerToken())) {
            try {
                RemoteOpenSavingsResponseDto r = remoteApiRestClient.post()
                        .uri(OPEN_TERM_DEPOSIT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(RemoteOpenSavingsResponseDto.class);
                if (r != null) {
                    return r;
                }
            } catch (RestClientResponseException e) {
                log.warn("remote POST {} (service) → HTTP {} body={}",
                        OPEN_TERM_DEPOSIT_PATH, e.getStatusCode(), truncate(e.getResponseBodyAsString(), 400));
            } catch (Exception e) {
                log.warn("remote POST {} (service) erreur: {}", OPEN_TERM_DEPOSIT_PATH, e.getMessage());
            }
        }

        if (!StringUtils.hasText(userAccessToken)) {
            throw new RemoteOpenTermDepositException(
                    "Ouverture impossible : identifiant de session manquant pour l’API distante.",
                    null);
        }

        try {
            RemoteOpenSavingsResponseDto r = remoteApiRestClient.post()
                    .uri(OPEN_TERM_DEPOSIT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken.trim())
                    .body(body)
                    .retrieve()
                    .body(RemoteOpenSavingsResponseDto.class);
            return r != null ? r : new RemoteOpenSavingsResponseDto();
        } catch (RestClientResponseException e) {
            log.warn("remote POST {} (user JWT) → HTTP {} body={}",
                    OPEN_TERM_DEPOSIT_PATH, e.getStatusCode(), truncate(e.getResponseBodyAsString(), 400));
            throw new RemoteOpenTermDepositException(
                    "L’ouverture du dépôt à terme a échoué côté serveur bancaire. Réessayez plus tard.",
                    e);
        } catch (Exception e) {
            log.warn("remote POST {} (user JWT) erreur: {}", OPEN_TERM_DEPOSIT_PATH, e.getMessage());
            throw new RemoteOpenTermDepositException(
                    "L’ouverture du dépôt à terme a échoué. Réessayez plus tard.",
                    e);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
