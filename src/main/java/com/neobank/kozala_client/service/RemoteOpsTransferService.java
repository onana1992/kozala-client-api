package com.neobank.kozala_client.service;

import com.neobank.kozala_client.config.RemoteApiConfig;
import com.neobank.kozala_client.config.RemoteApiProperties;
import com.neobank.kozala_client.dto.remote.RemoteOpsTransferRequest;
import com.neobank.kozala_client.dto.remote.RemoteOpsTransferResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RemoteOpsTransferService {

    private static final String CLIENT_PERSON_TRANSFER_PATH = "/api/client/transfers/person";
    private static final String MISSING_BEARER =
            "Configuration manquante : définissez app.remote-api.bearer-token pour effectuer un transfert.";

    @Qualifier(RemoteApiConfig.REMOTE_API_REST_CLIENT)
    private final RestClient remoteApiRestClient;
    private final RemoteApiProperties remoteApiProperties;

    /**
     * Crée un virement P2P côté core ({@code POST /api/client/transfers/person}), avec le jeton service.
     * En-tête {@code Idempotency-Key} requis par le core : généré à chaque appel.
     */
    public RemoteOpsTransferResponseDto create(RemoteOpsTransferRequest body) {
        if (!StringUtils.hasText(remoteApiProperties.getBearerToken())) {
            throw new RemotePersonTransferException(MISSING_BEARER, null);
        }
        try {
            RemoteOpsTransferResponseDto r = remoteApiRestClient.post()
                    .uri(CLIENT_PERSON_TRANSFER_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(body)
                    .retrieve()
                    .body(RemoteOpsTransferResponseDto.class);
            return r != null ? r : RemoteOpsTransferResponseDto.builder().build();
        } catch (RestClientResponseException e) {
            log.warn("remote POST {} → HTTP {} body={}",
                    CLIENT_PERSON_TRANSFER_PATH, e.getStatusCode(), truncate(e.getResponseBodyAsString(), 400));
            throw new RemotePersonTransferException(
                    RemoteRestClientErrorSupport.extractRemoteErrorMessage(e),
                    e);
        } catch (Exception e) {
            log.warn("remote POST {} erreur: {}", CLIENT_PERSON_TRANSFER_PATH, e.getMessage());
            throw new RemotePersonTransferException(
                    "Le transfert n'a pas pu être effectué. Réessayez plus tard.",
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
