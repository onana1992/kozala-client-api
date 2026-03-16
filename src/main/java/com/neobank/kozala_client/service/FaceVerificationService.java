package com.neobank.kozala_client.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FaceVerificationService {

    private static final String PROVIDER_NONE = "none";

    @Value("${app.face-verification.provider:aws}")
    private String faceVerificationProvider;

    private final AwsRekognitionFaceVerificationService awsRekognitionFaceVerificationService;

    public FaceVerificationService(AwsRekognitionFaceVerificationService awsRekognitionFaceVerificationService) {
        this.awsRekognitionFaceVerificationService = awsRekognitionFaceVerificationService;
    }

    /**
     * Compare le visage du document (image du recto) au visage de la selfie (AWS Rekognition CompareFaces).
     * Si app.face-verification.provider=none, retourne DISABLED (selfie acceptée sans vérification).
     */
    public FaceVerificationResult verifyDocumentAndSelfie(byte[] documentImageBytes, byte[] selfieImageBytes) {
        if (PROVIDER_NONE.equalsIgnoreCase(faceVerificationProvider)) {
            return FaceVerificationResult.DISABLED;
        }
        if (awsRekognitionFaceVerificationService == null) {
            log.debug("Rekognition non disponible, skip vérification");
            return FaceVerificationResult.DISABLED;
        }
        return awsRekognitionFaceVerificationService.compareFaces(documentImageBytes, selfieImageBytes);
    }
}
