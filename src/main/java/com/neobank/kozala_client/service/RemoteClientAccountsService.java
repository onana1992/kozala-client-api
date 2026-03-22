package com.neobank.kozala_client.service;

import com.neobank.kozala_client.config.RemoteApiConfig;
import com.neobank.kozala_client.config.RemoteApiProperties;
import com.neobank.kozala_client.dto.auth.ClientAccountDto;
import com.neobank.kozala_client.dto.remote.RemoteBankAccountDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RemoteClientAccountsService {

    private static final String ACCOUNTS_PATH = "/api/client/accounts";

    @Qualifier(RemoteApiConfig.REMOTE_API_REST_CLIENT)
    private final RestClient remoteApiRestClient;
    private final RemoteApiProperties remoteApiProperties;

    /**
     * Récupère les comptes sur l’API distante pour un client résolu en base ({@code clients.id}).
     * Authentification : uniquement {@code app.remote-api.bearer-token} + param / en-tête client selon la config.
     */
    public List<ClientAccountDto> fetchAccounts(long clientIdFromDb) {
        List<ClientAccountDto> list = tryFetchWithServiceTokenAndClientId(clientIdFromDb);
        log.info("remote GET {} — bearer service + clientId={} (id BD) → {} compte(s)",
                ACCOUNTS_PATH, clientIdFromDb, list.size());
        return list;
    }

    private List<ClientAccountDto> tryFetchWithServiceTokenAndClientId(long clientId) {
        if (!StringUtils.hasText(remoteApiProperties.getBearerToken())) {
            log.warn("remote GET {} ignoré : app.remote-api.bearer-token non défini", ACCOUNTS_PATH);
            return Collections.emptyList();
        }
        String param = remoteApiProperties.getAccountsClientIdQueryParam();
        UriComponentsBuilder ub = UriComponentsBuilder.fromPath(ACCOUNTS_PATH);
        if (StringUtils.hasText(param)) {
            ub.queryParam(param.trim(), clientId);
        }
        String uri = ub.build().toUriString();
        try {
            var spec = remoteApiRestClient.get().uri(uri);
            String idHeader = remoteApiProperties.getAccountsClientIdHeader();
            if (StringUtils.hasText(idHeader)) {
                spec = spec.header(idHeader.trim(), String.valueOf(clientId));
            }
            List<RemoteBankAccountDto> raw = spec
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<RemoteBankAccountDto>>() {});
            return mapList(raw);
        } catch (RestClientResponseException e) {
            log.warn("remote GET {} (clientId={}) → HTTP {} body={}",
                    ACCOUNTS_PATH, clientId, e.getStatusCode(), truncate(e.getResponseBodyAsString(), 500));
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("remote GET {} (clientId={}) erreur: {}", ACCOUNTS_PATH, clientId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private static List<ClientAccountDto> mapList(List<RemoteBankAccountDto> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        return raw.stream().map(RemoteClientAccountsService::map).toList();
    }

    private static ClientAccountDto map(RemoteBankAccountDto a) {
        var p = a.getProduct();
        var effective = a.getEffectiveAvailableBalance() != null
                ? a.getEffectiveAvailableBalance()
                : a.getAvailableBalance();
        return ClientAccountDto.builder()
                .id(a.getId())
                .accountNumber(a.getAccountNumber())
                .productCode(p != null ? p.getCode() : null)
                .productName(p != null ? p.getName() : null)
                .productDescription(p != null ? p.getDescription() : null)
                .productCategory(p != null ? p.getCategory() : null)
                .status(a.getStatus())
                .openedAt(a.getOpenedAt())
                .currency(a.getCurrency())
                .balance(a.getBalance())
                .effectiveAvailableBalance(effective)
                .build();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
