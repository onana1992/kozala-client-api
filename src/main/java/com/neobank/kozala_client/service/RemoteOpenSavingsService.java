package com.neobank.kozala_client.service;

import com.neobank.kozala_client.config.RemoteApiConfig;
import com.neobank.kozala_client.config.RemoteApiProperties;
import com.neobank.kozala_client.dto.auth.OpenSavingsRequest;
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
public class RemoteOpenSavingsService {

    private static final String OPEN_SAVINGS_PATH = "/api/client/accounts/open-savings";

    @Qualifier(RemoteApiConfig.REMOTE_API_REST_CLIENT)
    private final RestClient remoteApiRestClient;
    private final RemoteApiProperties remoteApiProperties;

    /**
     * Demande d’ouverture compte épargne sur l’API distante.
     * Même stratégie jeton que le catalogue : service puis JWT utilisateur.
     */
    public RemoteOpenSavingsResponseDto open(OpenSavingsRequest body, String userAccessToken) {
        if (StringUtils.hasText(remoteApiProperties.getBearerToken())) {
            try {
                RemoteOpenSavingsResponseDto r = remoteApiRestClient.post()
                        .uri(OPEN_SAVINGS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(RemoteOpenSavingsResponseDto.class);
                if (r != null) {
                    return r;
                }
            } catch (RestClientResponseException e) {
                log.warn("remote POST {} (service) → HTTP {} body={}",
                        OPEN_SAVINGS_PATH, e.getStatusCode(), truncate(e.getResponseBodyAsString(), 400));
            } catch (Exception e) {
                log.warn("remote POST {} (service) erreur: {}", OPEN_SAVINGS_PATH, e.getMessage());
            }
        }

        if (!StringUtils.hasText(userAccessToken)) {
            throw new RemoteOpenSavingsException(
                    "Ouverture impossible : identifiant de session manquant pour l’API distante.",
                    null);
        }

        try {
            RemoteOpenSavingsResponseDto r = remoteApiRestClient.post()
                    .uri(OPEN_SAVINGS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken.trim())
                    .body(body)
                    .retrieve()
                    .body(RemoteOpenSavingsResponseDto.class);
            return r != null ? r : new RemoteOpenSavingsResponseDto();
        } catch (RestClientResponseException e) {
            log.warn("remote POST {} (user JWT) → HTTP {} body={}",
                    OPEN_SAVINGS_PATH, e.getStatusCode(), truncate(e.getResponseBodyAsString(), 400));
            throw new RemoteOpenSavingsException(
                    "L’ouverture du compte épargne a échoué côté serveur bancaire. Réessayez plus tard.",
                    e);
        } catch (Exception e) {
            log.warn("remote POST {} (user JWT) erreur: {}", OPEN_SAVINGS_PATH, e.getMessage());
            throw new RemoteOpenSavingsException(
                    "L’ouverture du compte épargne a échoué. Réessayez plus tard.",
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
