package com.neobank.kozala_client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aws.rekognition")
public class AwsRekognitionProperties {

    /**
     * Seuil de similarité (0.0 à 1.0) pour considérer que deux visages correspondent.
     * Rekognition CompareFaces retourne une confidence ; si >= ce seuil, on considère un match.
     */
    private float faceMatchThreshold = 0.8f;
}
