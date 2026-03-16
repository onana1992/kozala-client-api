package com.neobank.kozala_client.service;

import com.neobank.kozala_client.config.AwsProperties;
import com.neobank.kozala_client.config.AwsS3Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class AwsS3DocumentStorageService {

    private final AwsProperties awsProperties;
    private final AwsS3Properties s3Properties;
    private S3Client s3Client;

    public AwsS3DocumentStorageService(AwsProperties awsProperties, AwsS3Properties s3Properties) {
        this.awsProperties = awsProperties;
        this.s3Properties = s3Properties;
    }

    @PostConstruct
    public void init() {
        if (!isConfigured()) {
            log.debug("AWS S3 non configuré (bucket manquant)");
            return;
        }
        var builder = S3Client.builder().region(Region.of(awsProperties.getRegion()));
        if (awsProperties.getAccessKeyId() != null && !awsProperties.getAccessKeyId().isBlank()
                && awsProperties.getSecretAccessKey() != null && !awsProperties.getSecretAccessKey().isBlank()) {
            builder = builder.credentialsProvider(
                    () -> software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                            awsProperties.getAccessKeyId(), awsProperties.getSecretAccessKey()));
        }
        this.s3Client = builder.build();
        log.info("AWS S3 client initialisé pour le bucket {}", s3Properties.getBucketIdentity());
    }

    @PreDestroy
    public void destroy() {
        if (s3Client != null) {
            s3Client.close();
        }
    }

    public boolean isConfigured() {
        return s3Properties.getBucketIdentity() != null && !s3Properties.getBucketIdentity().isBlank();
    }

    /**
     * Upload un document vers S3. Clé : clients/{id}/documents/{type}/{uuid}-{side}.ext
     */
    public String uploadDocument(Long clientId, String documentType, String side, MultipartFile file) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("AWS S3 non configuré (aws.s3.bucket-identity)");
        }
        String ext = getFileExtension(file.getContentType());
        String key = "clients/" + clientId + "/documents/" + documentType + "/" + UUID.randomUUID() + "-" + side + ext;
        String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Properties.getBucketIdentity())
                .key(key)
                .contentType(contentType)
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        log.info("Document uploadé S3 pour clientId={} key={}", clientId, key);
        return key;
    }

    /**
     * Upload une photo de profil vers S3. Clé : clients/{clientId}/profile/{filename}.
     */
    public String uploadProfilePhoto(Long clientId, String filename, MultipartFile file) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("AWS S3 non configuré (aws.s3.bucket-identity)");
        }
        String key = "clients/" + clientId + "/profile/" + filename;
        String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Properties.getBucketIdentity())
                .key(key)
                .contentType(contentType)
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        log.info("Photo de profil uploadée S3 pour clientId={} key={}", clientId, key);
        return key;
    }

    /**
     * Télécharge les octets d'un objet S3 à partir de sa clé.
     */
    public Optional<byte[]> downloadDocument(String storageKey) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(s3Properties.getBucketIdentity())
                    .key(storageKey)
                    .build();
            byte[] bytes = s3Client.getObject(request).readAllBytes();
            return Optional.of(bytes);
        } catch (S3Exception e) {
            if (e.awsErrorDetails() != null && "NoSuchKey".equals(e.awsErrorDetails().errorCode())) {
                log.warn("S3 GetObject NoSuchKey: bucket={} key={}", s3Properties.getBucketIdentity(), storageKey);
                return Optional.empty();
            }
            log.warn("S3 GetObject erreur: bucket={} key={} errorCode={} message={}", s3Properties.getBucketIdentity(), storageKey,
                e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "?", e.getMessage());
            return Optional.empty();
        } catch (IOException e) {
            log.warn("Erreur lecture S3 {}", storageKey, e);
            return Optional.empty();
        }
    }

    /**
     * Supprime un objet S3 à partir de sa clé.
     */
    public void deleteObject(String key) {
        if (!isConfigured()) {
            log.warn("S3 non configuré, suppression ignorée: key={}", key);
            return;
        }
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucketIdentity())
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
            log.info("Objet S3 supprimé: key={}", key);
        } catch (S3Exception e) {
            log.warn("Erreur suppression S3 key={} errorCode={} message={}", key,
                    e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "?", e.getMessage());
        }
    }

    private static String getFileExtension(String contentType) {
        if (contentType == null) return ".jpg";
        if (contentType.toLowerCase().contains("png")) return ".png";
        if (contentType.toLowerCase().contains("webp")) return ".webp";
        return ".jpg";
    }
}
