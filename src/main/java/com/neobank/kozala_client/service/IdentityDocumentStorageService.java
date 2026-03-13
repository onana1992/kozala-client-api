package com.neobank.kozala_client.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.neobank.kozala_client.config.AzureStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdentityDocumentStorageService {

    private final AzureStorageProperties properties;

    /**
     * Upload un document d'identité (recto, verso ou selfie) vers Azure Blob Storage.
     *
     * @param clientId   ID du client
     * @param documentType type (ex. "id_card", "passport", "selfie")
     * @param side       "recto", "verso" ou "selfie"
     * @param file       fichier image
     * @return clé de stockage (chemin dans le conteneur) pour enregistrement en base
     */
    public String uploadDocument(Long clientId, String documentType, String side, MultipartFile file) throws IOException {
        if (properties.getConnectionString() == null || properties.getConnectionString().isBlank()) {
            throw new IllegalStateException("Azure Storage non configuré (azure.storage.connection-string)");
        }
        String ext = getFileExtension(file.getContentType());
        String blobName = "clients/" + clientId + "/documents/" + documentType + "/" + UUID.randomUUID() + "-" + side + ext;
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(properties.getConnectionString())
                .buildClient();
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(properties.getContainerIdentity());
        if (!containerClient.exists()) {
            containerClient.create();
        }
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        blobClient.upload(file.getInputStream(), file.getSize(), true);
        log.info("Document uploadé pour clientId={} blob={}", clientId, blobName);
        return blobName;
    }

    /**
     * Télécharge les octets d'un blob à partir de sa clé (pour Custom Vision / Face API).
     */
    public Optional<byte[]> downloadDocument(String storageKey) {
        if (properties.getConnectionString() == null || properties.getConnectionString().isBlank()) {
            return Optional.empty();
        }
        try {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(properties.getConnectionString())
                    .buildClient();
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(properties.getContainerIdentity());
            BlobClient blobClient = containerClient.getBlobClient(storageKey);
            if (!blobClient.exists()) {
                return Optional.empty();
            }
            return Optional.of(blobClient.downloadContent().toBytes());
        } catch (Exception e) {
            log.warn("Erreur téléchargement blob {}", storageKey, e);
            return Optional.empty();
        }
    }

    private static String getFileExtension(String contentType) {
        if (contentType == null) return ".jpg";
        if (contentType.toLowerCase().contains("png")) return ".png";
        if (contentType.toLowerCase().contains("webp")) return ".webp";
        return ".jpg";
    }
}
