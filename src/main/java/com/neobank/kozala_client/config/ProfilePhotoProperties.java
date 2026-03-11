package com.neobank.kozala_client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.profile-photo")
public class ProfilePhotoProperties {

    /** Répertoire de stockage des photos (créé si nécessaire). */
    private String uploadDir = "uploads/profile-photos";
    /** Taille max en octets (défaut 5 Mo). */
    private long maxSizeBytes = 5 * 1024 * 1024;
    /** Types MIME autorisés. */
    private List<String> allowedContentTypes = List.of("image/jpeg", "image/png", "image/webp");
}
