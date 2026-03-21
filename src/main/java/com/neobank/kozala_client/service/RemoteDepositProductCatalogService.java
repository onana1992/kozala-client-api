package com.neobank.kozala_client.service;

import com.neobank.kozala_client.config.RemoteApiConfig;
import com.neobank.kozala_client.config.RemoteApiProperties;
import com.neobank.kozala_client.dto.remote.RemoteDepositProductCatalogItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
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

    @Qualifier(RemoteApiConfig.REMOTE_API_REST_CLIENT)
    private final RestClient remoteApiRestClient;
    private final RemoteApiProperties remoteApiProperties;

    /**
     * Catalogue produits de dépôt sur l’API distante.
     * <ol>
     *   <li>Si {@code app.remote-api.bearer-token} est défini : GET avec ce jeton service.</li>
     *   <li>Sinon ou en cas d’échec : GET avec le JWT utilisateur (Authorization Bearer).</li>
     * </ol>
     */
    public List<RemoteDepositProductCatalogItemDto> fetchCatalog(String userAccessToken) {
        if (StringUtils.hasText(remoteApiProperties.getBearerToken())) {
            try {
                List<RemoteDepositProductCatalogItemDto> list = remoteApiRestClient.get()
                        .uri(CATALOG_PATH)
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<RemoteDepositProductCatalogItemDto>>() {});
                if (list != null) {
                    return list;
                }
            } catch (RestClientResponseException e) {
                log.warn("remote GET {} (service) → HTTP {} body={}",
                        CATALOG_PATH, e.getStatusCode(), truncate(e.getResponseBodyAsString(), 400));
            } catch (Exception e) {
                log.warn("remote GET {} (service) erreur: {}", CATALOG_PATH, e.getMessage());
            }
        }

        if (!StringUtils.hasText(userAccessToken)) {
            log.debug("deposit-product-catalog: pas de jeton utilisateur pour repli");
            return Collections.emptyList();
        }

        try {
            List<RemoteDepositProductCatalogItemDto> list = remoteApiRestClient.get()
                    .uri(CATALOG_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken.trim())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<RemoteDepositProductCatalogItemDto>>() {});
            return list != null ? list : Collections.emptyList();
        } catch (RestClientResponseException e) {
            log.warn("remote GET {} (user JWT) → HTTP {} body={}",
                    CATALOG_PATH, e.getStatusCode(), truncate(e.getResponseBodyAsString(), 400));
            throw new RemoteDepositProductCatalogException(
                    "Impossible de charger le catalogue des produits de dépôt pour le moment.",
                    e);
        } catch (Exception e) {
            log.warn("remote GET {} (user JWT) erreur: {}", CATALOG_PATH, e.getMessage());
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
