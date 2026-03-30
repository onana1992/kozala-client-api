package com.neobank.kozala_client.service;

import com.neobank.kozala_client.config.RemoteApiConfig;
import com.neobank.kozala_client.config.RemoteApiProperties;
import com.neobank.kozala_client.dto.remote.RemoteTransferCounterpartyDto;
import com.neobank.kozala_client.dto.remote.RemoteTransactionDto;
import com.neobank.kozala_client.dto.remote.RemoteTransactionPageDto;
import com.neobank.kozala_client.dto.transaction.ClientTransferCounterpartyDto;
import com.neobank.kozala_client.dto.transaction.ClientTransactionDto;
import com.neobank.kozala_client.dto.transaction.ClientTransactionsPageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class RemoteClientTransactionsService {

    private static final String TRANSACTIONS_PATH = "/api/client/transactions";

    private static String transactionsDetailPath(long transactionId) {
        return TRANSACTIONS_PATH + "/" + transactionId;
    }
    private static final String MISSING_BEARER =
            "Configuration manquante : définissez app.remote-api.bearer-token pour consulter les transactions.";

    @Qualifier(RemoteApiConfig.REMOTE_API_REST_CLIENT)
    private final RestClient remoteApiRestClient;
    private final RemoteApiProperties remoteApiProperties;

    /**
     * Proxy {@code GET /api/client/transactions} vers le core (jeton service + {@code clientId}).
     */
    public ClientTransactionsPageDto fetchPage(
            long clientId,
            Long accountId,
            LocalDate fromDate,
            LocalDate toDate,
            String type,
            String status,
            int page,
            int size,
            String sort) {
        if (!StringUtils.hasText(remoteApiProperties.getBearerToken())) {
            log.warn("remote GET {} : app.remote-api.bearer-token non défini", TRANSACTIONS_PATH);
            throw new RemoteClientTransactionsException(MISSING_BEARER, null);
        }
        UriComponentsBuilder ub = UriComponentsBuilder.fromPath(TRANSACTIONS_PATH)
                .queryParam("clientId", clientId)
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("sort", sort != null && !sort.isBlank() ? sort : "desc");
        if (accountId != null) {
            ub.queryParam("accountId", accountId);
        }
        if (fromDate != null) {
            ub.queryParam("fromDate", fromDate.toString());
        }
        if (toDate != null) {
            ub.queryParam("toDate", toDate.toString());
        }
        if (StringUtils.hasText(type)) {
            ub.queryParam("type", type.trim());
        }
        if (StringUtils.hasText(status)) {
            ub.queryParam("status", status.trim());
        }
        String uri = ub.build().toUriString();
        try {
            RemoteTransactionPageDto raw = remoteApiRestClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<RemoteTransactionPageDto>() {});
            if (raw == null) {
                raw = new RemoteTransactionPageDto();
            }
            ClientTransactionsPageDto out = ClientTransactionsPageDto.builder()
                    .content(mapContent(raw.getContent()))
                    .totalElements(raw.getTotalElements())
                    .totalPages(raw.getTotalPages())
                    .page(raw.getNumber())
                    .size(raw.getSize())
                    .last(raw.isLast())
                    .first(raw.isFirst())
                    .build();
            log.info("remote GET {} — clientId={} page={}/{} → {} ligne(s)",
                    TRANSACTIONS_PATH, clientId, page, raw.getTotalPages(),
                    out.getContent() != null ? out.getContent().size() : 0);
            return out;
        } catch (RestClientResponseException e) {
            log.warn("remote GET {} (clientId={}) → HTTP {} body={}",
                    TRANSACTIONS_PATH, clientId, e.getStatusCode(),
                    truncate(e.getResponseBodyAsString(), 500));
            throw new RemoteClientTransactionsException(
                    RemoteRestClientErrorSupport.extractRemoteErrorMessage(e),
                    e);
        } catch (Exception e) {
            log.warn("remote GET {} (clientId={}) erreur: {}", TRANSACTIONS_PATH, clientId, e.getMessage());
            throw new RemoteClientTransactionsException(
                    "Impossible de charger les transactions pour le moment.",
                    e);
        }
    }

    /**
     * Proxy {@code GET /api/client/transactions/{id}?clientId=} vers le core.
     */
    public ClientTransactionDto fetchOne(long clientId, long transactionId) {
        if (!StringUtils.hasText(remoteApiProperties.getBearerToken())) {
            log.warn("remote GET {} : app.remote-api.bearer-token non défini", TRANSACTIONS_PATH);
            throw new RemoteClientTransactionsException(MISSING_BEARER, null);
        }
        String uri = UriComponentsBuilder.fromPath(transactionsDetailPath(transactionId))
                .queryParam("clientId", clientId)
                .build()
                .toUriString();
        try {
            RemoteTransactionDto raw = remoteApiRestClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(RemoteTransactionDto.class);
            if (raw == null || raw.getId() == null) {
                throw new RemoteClientTransactionsException(
                        "Réponse transaction vide du serveur bancaire.", null);
            }
            log.info("remote GET {} — clientId={} transactionId={}", uri, clientId, transactionId);
            return mapRow(raw);
        } catch (RestClientResponseException e) {
            log.warn("remote GET transaction detail (clientId={} id={}) → HTTP {} body={}",
                    clientId, transactionId, e.getStatusCode(),
                    truncate(e.getResponseBodyAsString(), 500));
            throw new RemoteClientTransactionsException(
                    RemoteRestClientErrorSupport.extractRemoteErrorMessage(e),
                    e);
        } catch (Exception e) {
            log.warn("remote GET transaction detail (clientId={} id={}) erreur: {}",
                    clientId, transactionId, e.getMessage());
            throw new RemoteClientTransactionsException(
                    "Impossible de charger le détail de la transaction pour le moment.",
                    e);
        }
    }

    private static List<ClientTransactionDto> mapContent(List<RemoteTransactionDto> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        return raw.stream().filter(Objects::nonNull).map(RemoteClientTransactionsService::mapRow).toList();
    }

    private static ClientTransactionDto mapRow(RemoteTransactionDto t) {
        return ClientTransactionDto.builder()
                .id(t.getId())
                .transactionNumber(t.getTransactionNumber())
                .type(t.getType())
                .status(t.getStatus())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .accountId(t.getAccountId())
                .referenceType(t.getReferenceType())
                .referenceId(t.getReferenceId())
                .description(t.getDescription())
                .metadata(t.getMetadata())
                .valueDate(t.getValueDate())
                .transactionDate(t.getTransactionDate())
                .createdBy(t.getCreatedBy())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .transferCounterparty(mapCounterparty(t.getTransferCounterparty()))
                .build();
    }

    private static ClientTransferCounterpartyDto mapCounterparty(RemoteTransferCounterpartyDto c) {
        if (c == null) {
            return null;
        }
        return ClientTransferCounterpartyDto.builder()
                .partyRole(c.getPartyRole())
                .counterpartyClientId(c.getCounterpartyClientId())
                .displayName(c.getDisplayName())
                .firstName(c.getFirstName())
                .lastName(c.getLastName())
                .phone(c.getPhone())
                .email(c.getEmail())
                .counterpartyAccountId(c.getCounterpartyAccountId())
                .accountNumber(c.getAccountNumber())
                .accountLabel(c.getAccountLabel())
                .productName(c.getProductName())
                .profilePhotoUrl(buildProfilePhotoUrlFromCorePath(c.getProfilePhotoPath()))
                .build();
    }

    private static String buildProfilePhotoUrlFromCorePath(String profilePhotoPath) {
        if (!StringUtils.hasText(profilePhotoPath)) {
            return null;
        }
        return "/api/profile/photos?key="
                + URLEncoder.encode(profilePhotoPath.trim(), StandardCharsets.UTF_8);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
