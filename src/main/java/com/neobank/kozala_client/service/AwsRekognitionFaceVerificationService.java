package com.neobank.kozala_client.service;

import com.neobank.kozala_client.config.AwsProperties;
import com.neobank.kozala_client.config.AwsRekognitionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
@Slf4j
public class AwsRekognitionFaceVerificationService {

    private final AwsProperties awsProperties;
    private final AwsRekognitionProperties rekognitionProperties;
    private RekognitionClient rekognitionClient;

    public AwsRekognitionFaceVerificationService(AwsProperties awsProperties, AwsRekognitionProperties rekognitionProperties) {
        this.awsProperties = awsProperties;
        this.rekognitionProperties = rekognitionProperties;
    }

    @PostConstruct
    public void init() {
        var builder = RekognitionClient.builder().region(Region.of(awsProperties.getRegion()));
        if (awsProperties.getAccessKeyId() != null && !awsProperties.getAccessKeyId().isBlank()
                && awsProperties.getSecretAccessKey() != null && !awsProperties.getSecretAccessKey().isBlank()) {
            builder = builder.credentialsProvider(
                    () -> software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                            awsProperties.getAccessKeyId(), awsProperties.getSecretAccessKey()));
        }
        this.rekognitionClient = builder.build();
        log.info("AWS Rekognition client initialisé (région {})", awsProperties.getRegion());
    }

    @PreDestroy
    public void destroy() {
        if (rekognitionClient != null) {
            rekognitionClient.close();
        }
    }

    /**
     * Compare le visage du document (source) au visage de la selfie (target).
     * Retourne le résultat détaillé pour les messages d'erreur côté métier.
     */
    public FaceVerificationResult compareFaces(byte[] sourceImageBytes, byte[] targetImageBytes) {
        Image sourceImage = Image.builder().bytes(SdkBytes.fromByteArray(sourceImageBytes)).build();
        Image targetImage = Image.builder().bytes(SdkBytes.fromByteArray(targetImageBytes)).build();
        // Rekognition attend et retourne une similarité en 0-100 (pourcentage). Config 0.8 = 80%.
        float thresholdPercent = rekognitionProperties.getFaceMatchThreshold() * 100f;

        try {
            // Seuil 0 dans la requête pour que Rekognition retourne toutes les paires avec leur similarité (permettre de la logger même si < seuil métier).
            CompareFacesRequest request = CompareFacesRequest.builder()
                    .sourceImage(sourceImage)
                    .targetImage(targetImage)
                    .similarityThreshold(0f)
                    .build();
            CompareFacesResponse response = rekognitionClient.compareFaces(request);

            int matchCount = response.faceMatches() != null ? response.faceMatches().size() : 0;
            int unmatchedSourceCount = response.unmatchedFaces() != null ? response.unmatchedFaces().size() : 0;
            log.info("Rekognition CompareFaces – réponse: faceMatches={}, unmatchedFaces={}, sourceImageOrientationCorrection={}, targetImageOrientationCorrection={}",
                    matchCount, unmatchedSourceCount,
                    response.sourceImageOrientationCorrection(), response.targetImageOrientationCorrection());

            if (response.faceMatches() != null && !response.faceMatches().isEmpty()) {
                double maxSimilarity = 0;
                for (CompareFacesMatch m : response.faceMatches()) {
                    double sim = m.similarity().doubleValue();
                    if (sim > maxSimilarity) maxSimilarity = sim;
                    log.info("Rekognition CompareFaces – similarité={}%", m.similarity());
                }
                boolean match = maxSimilarity > thresholdPercent;
                log.info("Rekognition CompareFaces – similarité max: {}% (seuil>{}%), résultat={}", maxSimilarity, thresholdPercent, match ? "MATCH" : "NO_MATCH");
                if (match) {
                    return FaceVerificationResult.MATCH;
                }
                return FaceVerificationResult.NO_MATCH;
            }

            // Aucune paire retournée : pas de visage comparable ou pas de visage dans une des images
            log.info("Rekognition CompareFaces – aucune paire (similarité non disponible), appel DetectFaces pour distinguer absence de visage / pas la même personne");
            boolean faceInSource = hasFaceInImage(sourceImageBytes);
            boolean faceInTarget = hasFaceInImage(targetImageBytes);
            log.info("Rekognition DetectFaces – source (document): visage détecté={}, target (selfie): visage détecté={}", faceInSource, faceInTarget);
            if (!faceInSource) {
                return FaceVerificationResult.NO_FACE_IN_DOCUMENT;
            }
            if (!faceInTarget) {
                return FaceVerificationResult.NO_FACE_IN_SELFIE;
            }
            return FaceVerificationResult.NO_MATCH;

        } catch (Exception e) {
            log.warn("Erreur Rekognition CompareFaces: {}", e.getMessage());
            return FaceVerificationResult.NO_MATCH;
        }
    }

    private boolean hasFaceInImage(byte[] imageBytes) {
        try {
            DetectFacesRequest request = DetectFacesRequest.builder()
                    .image(Image.builder().bytes(SdkBytes.fromByteArray(imageBytes)).build())
                    .build();
            DetectFacesResponse response = rekognitionClient.detectFaces(request);
            int faceCount = response.faceDetails() != null ? response.faceDetails().size() : 0;
            if (faceCount > 0) {
                log.info("Rekognition DetectFaces – réponse: {} visage(s) détecté(s)", faceCount);
            }
            return faceCount > 0;
        } catch (Exception e) {
            log.debug("DetectFaces échec: {}", e.getMessage());
            return false;
        }
    }
}
