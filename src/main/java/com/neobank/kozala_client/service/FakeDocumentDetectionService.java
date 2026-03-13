package com.neobank.kozala_client.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neobank.kozala_client.config.AzureCustomVisionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class FakeDocumentDetectionService {

    private static final double FAKE_THRESHOLD = 0.7;

    private final AzureCustomVisionProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Vérifie si l'image est détectée comme faux document.
     *
     * @param imageBytes contenu binaire de l'image
     * @return true si le document est considéré comme faux (à rejeter)
     */
    public boolean isFakeDocument(byte[] imageBytes) {
        if (properties.getEndpoint() == null || properties.getEndpoint().isBlank()
                || properties.getPredictionKey() == null || properties.getPredictionKey().isBlank()) {
            log.debug("Custom Vision non configuré, skip détection faux document");
            return false;
        }
        String url = properties.getEndpoint().replaceAll("/$", "")
                + "/customvision/v3.0/Prediction/" + properties.getProjectId()
                + "/classify/iterations/" + properties.getPublishedName() + "/image";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Prediction-Key", properties.getPredictionKey());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        HttpEntity<byte[]> request = new HttpEntity<>(imageBytes, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode predictions = root.path("predictions");
                for (JsonNode p : predictions) {
                    String tag = p.path("tagName").asText("");
                    double probability = p.path("probability").asDouble(0);
                    if ("fake".equalsIgnoreCase(tag) && probability >= FAKE_THRESHOLD) {
                        log.info("Document détecté comme faux (probabilité {})", probability);
                        return true;
                    }
                }
                return false;
            }
        } catch (Exception e) {
            log.warn("Erreur Custom Vision, on considère le document comme non faux", e);
        }
        return false;
    }
}
