package com.neobank.kozala_client.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Détection de faux documents. Actuellement désactivée (aucun service externe).
 * Toujours retourne false : les documents ne sont pas rejetés pour cause de faux.
 * Une future intégration pourrait utiliser Rekognition Custom Labels ou un autre service.
 */
@Service
@Slf4j
public class FakeDocumentDetectionService {

    /**
     * Vérifie si l'image est détectée comme faux document.
     *
     * @param imageBytes contenu binaire de l'image
     * @return true si le document est considéré comme faux (à rejeter). Actuellement toujours false.
     */
    public boolean isFakeDocument(byte[] imageBytes) {
        return false;
    }
}
