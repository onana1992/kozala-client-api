package com.neobank.kozala_client.service;

import com.neobank.kozala_client.config.RemoteApiConfig;
import com.neobank.kozala_client.config.RemoteApiProperties;
import com.neobank.kozala_client.dto.remote.RemoteDepositProductCatalogItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RemoteDepositProductCatalogService {

    private static final String CATALOG_PATH = "/api/client/accounts/deposit-product-catalog";
    private static final String MISSING_BEARER =
            "Configuration manquante : définissez app.remote-api.bearer-token pour le catalogue produits.";

    @Qualifier(RemoteApiConfig.REMOTE_API_REST_CLIENT)
    private final RestClient remoteApiRestClient;
    private final RemoteApiProperties remoteApiProperties;

    /**
     * Catalogue produits de dépôt sur l’API distante (GET authentifié avec {@code app.remote-api.bearer-token}).
     */
    public List<RemoteDepositProductCatalogItemDto> fetchCatalog() {
        if (!StringUtils.hasText(remoteApiProperties.getBearerToken())) {
            throw new RemoteDepositProductCatalogException(MISSING_BEARER, null);
        }
        try {
            List<RemoteDepositProductCatalogItemDto> list = remoteApiRestClient.get()
                    .uri(CATALOG_PATH)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<RemoteDepositProductCatalogItemDto>>() {});
            return list != null ? list : Collections.emptyList();
        } catch (RestClientResponseException e) {
            log.warn("remote GET {} → HTTP {} body={}",
                    CATALOG_PATH, e.getStatusCode(), truncate(e.getResponseBodyAsString(), 400));
            throw new RemoteDepositProductCatalogException(
                    RemoteRestClientErrorSupport.extractRemoteErrorMessage(e),
                    e);
        } catch (Exception e) {
            log.warn("remote GET {} erreur: {}", CATALOG_PATH, e.getMessage());
            throw new RemoteDepositProductCatalogException(
                    "Impossible de charger le catalogue des produits de dépôt pour le moment.",
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
