package com.neobank.kozala_client.service;

import com.neobank.kozala_client.config.RemoteApiConfig;
import com.neobank.kozala_client.config.RemoteApiProperties;
import com.neobank.kozala_client.dto.remote.RemoteTransactionDto;
import com.neobank.kozala_client.dto.remote.RemoteTransactionPageDto;
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

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class RemoteClientTransactionsService {

    private static final String TRANSACTIONS_PATH = "/api/client/transactions";
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
                .build();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
