package com.neobank.kozala_client.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;

/** Extraction des messages d’erreur JSON renvoyés par l’API distante. */
final class RemoteRestClientErrorSupport {

    private static final ObjectMapper JSON = new ObjectMapper();

    private RemoteRestClientErrorSupport() {}

    static String extractRemoteErrorMessage(RestClientResponseException e) {
        String fromBody = tryParseMessageField(e.getResponseBodyAsString());
        if (StringUtils.hasText(fromBody)) {
            return fromBody.trim();
        }
        return fallbackMessage(e.getStatusCode());
    }

    private static String tryParseMessageField(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            JsonNode n = JSON.readTree(body);
            if (n != null && n.hasNonNull("message")) {
                String m = n.get("message").asText();
                return StringUtils.hasText(m) ? m : null;
            }
        } catch (Exception ignored) {
            // corps non JSON
        }
        return null;
    }

    private static String fallbackMessage(HttpStatusCode status) {
        int code = status.value();
        return switch (code) {
            case 400 -> "Requête refusée par le serveur bancaire.";
            case 401, 403 -> "Authentification refusée auprès du serveur bancaire.";
            case 404 -> "Ressource introuvable côté serveur bancaire.";
            case 409 -> "Conflit signalé par le serveur bancaire.";
            default -> "Erreur du serveur bancaire (HTTP " + code + ").";
        };
    }
}
