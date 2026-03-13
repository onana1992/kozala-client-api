package com.neobank.kozala_client.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.neobank.kozala_client.config.AzureFaceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FaceVerificationService {

    private static final double FACE_MATCH_CONFIDENCE_THRESHOLD = 0.8;

    private final AzureFaceProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Détecte le visage sur une image et retourne son faceId (Azure Face API).
     */
    public Optional<UUID> detectFaceId(byte[] imageBytes) {
        if (!isConfigured()) {
            log.debug("Face API non configuré");
            return Optional.empty();
        }
        String url = properties.getEndpoint().replaceAll("/$", "") + "/face/v1.0/detect?returnFaceId=true&returnFaceLandmarks=false";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Ocp-Apim-Subscription-Key", properties.getSubscriptionKey());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        HttpEntity<byte[]> request = new HttpEntity<>(imageBytes, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ArrayNode array = (ArrayNode) objectMapper.readTree(response.getBody());
                if (array.size() > 0) {
                    String faceId = array.get(0).path("faceId").asText();
                    return Optional.of(UUID.fromString(faceId));
                }
            }
        } catch (Exception e) {
            log.warn("Erreur Face API detect", e);
        }
        return Optional.empty();
    }

    /**
     * Compare deux faceId (même personne ou non).
     */
    public boolean verifyFaceMatch(UUID faceId1, UUID faceId2) {
        if (!isConfigured()) {
            log.debug("Face API non configuré, skip verify");
            return false;
        }
        String url = properties.getEndpoint().replaceAll("/$", "") + "/face/v1.0/verify";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Ocp-Apim-Subscription-Key", properties.getSubscriptionKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        ObjectNode body = objectMapper.createObjectNode();
        body.put("faceId1", faceId1.toString());
        body.put("faceId2", faceId2.toString());
        HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                boolean isIdentical = root.path("isIdentical").asBoolean(false);
                double confidence = root.path("confidence").asDouble(0);
                return isIdentical && confidence >= FACE_MATCH_CONFIDENCE_THRESHOLD;
            }
        } catch (Exception e) {
            log.warn("Erreur Face API verify", e);
        }
        return false;
    }

    private boolean isConfigured() {
        return properties.getEndpoint() != null && !properties.getEndpoint().isBlank()
                && properties.getSubscriptionKey() != null && !properties.getSubscriptionKey().isBlank();
    }
}
