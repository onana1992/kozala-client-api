package com.neobank.kozala_client.service;

import com.neobank.kozala_client.config.RemoteApiConfig;
import com.neobank.kozala_client.config.RemoteApiProperties;
import com.neobank.kozala_client.dto.auth.ClientAccountDto;
import com.neobank.kozala_client.dto.auth.ClientAccountPaymentMethodLinkDto;
import com.neobank.kozala_client.dto.auth.ClientPaymentMethodInfoDto;
import com.neobank.kozala_client.dto.remote.RemoteAccountPaymentMethodLinkDto;
import com.neobank.kozala_client.dto.remote.RemoteBankAccountDto;
import com.neobank.kozala_client.dto.remote.RemotePaymentMethodRefDto;
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
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class RemoteClientAccountsService {

    private static final String ACCOUNTS_PATH = "/api/client/accounts";
    private static final String MISSING_BEARER =
            "Configuration manquante : définissez app.remote-api.bearer-token pour lister les comptes.";

    @Qualifier(RemoteApiConfig.REMOTE_API_REST_CLIENT)
    private final RestClient remoteApiRestClient;
    private final RemoteApiProperties remoteApiProperties;

    /**
     * Récupère les comptes sur l’API distante pour un {@code clientId} (id client en base).
     * Authentification sortante : {@code app.remote-api.bearer-token} + query / en-tête client selon la config.
     *
     * @throws RemoteClientAccountsException si bearer absent ou erreur HTTP / réseau
     */
    public List<ClientAccountDto> fetchAccounts(long clientId) {
        if (!StringUtils.hasText(remoteApiProperties.getBearerToken())) {
            log.warn("remote GET {} : app.remote-api.bearer-token non défini", ACCOUNTS_PATH);
            throw new RemoteClientAccountsException(MISSING_BEARER, null);
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
            List<ClientAccountDto> list = mapList(raw);
            log.info("remote GET {} — bearer service + clientId={} → {} compte(s)",
                    ACCOUNTS_PATH, clientId, list.size());
            return list;
        } catch (RestClientResponseException e) {
            log.warn("remote GET {} (clientId={}) → HTTP {} body={}",
                    ACCOUNTS_PATH, clientId, e.getStatusCode(), truncate(e.getResponseBodyAsString(), 500));
            throw new RemoteClientAccountsException(
                    RemoteRestClientErrorSupport.extractRemoteErrorMessage(e),
                    e);
        } catch (Exception e) {
            log.warn("remote GET {} (clientId={}) erreur: {}", ACCOUNTS_PATH, clientId, e.getMessage());
            throw new RemoteClientAccountsException(
                    "Impossible de charger les comptes pour le moment.",
                    e);
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
                .paymentMethods(mapPaymentMethods(a.getPaymentMethods()))
                .build();
    }

    private static List<ClientAccountPaymentMethodLinkDto> mapPaymentMethods(
            List<RemoteAccountPaymentMethodLinkDto> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return raw.stream()
                .filter(Objects::nonNull)
                .map(RemoteClientAccountsService::mapPaymentLink)
                .toList();
    }

    private static ClientAccountPaymentMethodLinkDto mapPaymentLink(RemoteAccountPaymentMethodLinkDto link) {
        return ClientAccountPaymentMethodLinkDto.builder()
                .id(link.getId())
                .paymentMethod(mapPaymentRef(link.getPaymentMethod()))
                .allowedDeposit(link.getAllowedDeposit())
                .allowedWithdrawal(link.getAllowedWithdrawal())
                .allowedLoanRepayment(link.getAllowedLoanRepayment())
                .displayOrder(link.getDisplayOrder())
                .createdAt(link.getCreatedAt())
                .updatedAt(link.getUpdatedAt())
                .build();
    }

    private static ClientPaymentMethodInfoDto mapPaymentRef(RemotePaymentMethodRefDto ref) {
        if (ref == null) {
            return null;
        }
        return ClientPaymentMethodInfoDto.builder()
                .id(ref.getId())
                .code(ref.getCode())
                .name(ref.getName())
                .isActive(ref.getIsActive())
                .createdAt(ref.getCreatedAt())
                .updatedAt(ref.getUpdatedAt())
                .build();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
