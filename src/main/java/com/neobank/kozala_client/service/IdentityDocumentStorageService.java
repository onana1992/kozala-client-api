package com.neobank.kozala_client.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdentityDocumentStorageService {

    private final AwsS3DocumentStorageService awsS3DocumentStorageService;

    /**
     * Indique si le stockage S3 est configuré (documents et photos de profil).
     */
    public boolean isUsingAws() {
        return awsS3DocumentStorageService != null && awsS3DocumentStorageService.isConfigured();
    }

    /**
     * Upload un document d'identité (recto, verso ou selfie) vers AWS S3.
     */
    public String uploadDocument(Long clientId, String documentType, String side, MultipartFile file) throws IOException {
        if (!isUsingAws()) {
            throw new IllegalStateException("Stockage S3 non configuré (aws.s3.bucket-identity)");
        }
        return awsS3DocumentStorageService.uploadDocument(clientId, documentType, side, file);
    }

    /**
     * Upload une photo de profil vers S3 (clients/{id}/profile/{filename}).
     */
    public String uploadProfilePhoto(Long clientId, String filename, MultipartFile file) throws IOException {
        if (!isUsingAws()) {
            throw new IllegalStateException("Stockage S3 non configuré (aws.s3.bucket-identity)");
        }
        return awsS3DocumentStorageService.uploadProfilePhoto(clientId, filename, file);
    }

    /**
     * Télécharge les octets d'un document à partir de sa clé S3.
     */
    public Optional<byte[]> downloadDocument(String storageKey) {
        if (!isUsingAws()) {
            log.warn("downloadDocument: S3 non configuré (aws.s3.bucket-identity manquant?), clé ignorée: {}", storageKey);
            return Optional.empty();
        }
        return awsS3DocumentStorageService.downloadDocument(storageKey);
    }

    /**
     * Supprime un document du stockage S3 à partir de sa clé.
     */
    public void deleteDocument(String storageKey) {
        if (!isUsingAws()) {
            log.warn("deleteDocument: S3 non configuré, clé ignorée: {}", storageKey);
            return;
        }
        awsS3DocumentStorageService.deleteObject(storageKey);
    }
}
