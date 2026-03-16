package com.neobank.kozala_client.service;

import com.neobank.kozala_client.config.ProfilePhotoProperties;
import com.neobank.kozala_client.entity.Client;
import com.neobank.kozala_client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProfilePhotoService {

    private static final String S3_KEY_PREFIX = "clients/";

    private final ClientRepository clientRepository;
    private final ProfilePhotoProperties properties;
    private final IdentityDocumentStorageService identityDocumentStorageService;

    /**
     * Enregistre la photo de profil : sur S3 si app.identity-storage.provider=aws, sinon sur disque local.
     */
    @Transactional
    public String saveProfilePhoto(Client client, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier requis");
        }
        long size = file.getSize();
        if (size > properties.getMaxSizeBytes()) {
            throw new IllegalArgumentException("Fichier trop volumineux (max " + (properties.getMaxSizeBytes() / 1024 / 1024) + " Mo)");
        }
        String contentType = Optional.ofNullable(file.getContentType()).orElse("").toLowerCase();
        if (!properties.getAllowedContentTypes().stream().anyMatch(contentType::startsWith)) {
            throw new IllegalArgumentException("Type de fichier non autorisé. Utilisez JPEG, PNG ou WebP.");
        }
        String extension = contentType.contains("png") ? "png" : contentType.contains("webp") ? "webp" : "jpg";
        String filename = UUID.randomUUID() + "." + extension;

        if (identityDocumentStorageService.isUsingAws()) {
            String key = identityDocumentStorageService.uploadProfilePhoto(client.getId(), filename, file);
            String oldPath = client.getProfilePhotoPath();
            client.setProfilePhotoPath(key);
            clientRepository.save(client);
            log.info("Photo de profil enregistrée S3 pour clientId={} key={}", client.getId(), key);
            return key;
        }

        Path uploadDir = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
        Path targetFile = uploadDir.resolve(filename).normalize();
        if (!targetFile.startsWith(uploadDir)) {
            throw new SecurityException("Nom de fichier invalide");
        }
        file.transferTo(targetFile.toFile());

        String oldPath = client.getProfilePhotoPath();
        client.setProfilePhotoPath(filename);
        clientRepository.save(client);

        if (oldPath != null && !oldPath.isEmpty() && !oldPath.startsWith(S3_KEY_PREFIX)) {
            Path oldFile = uploadDir.resolve(oldPath).normalize();
            if (oldFile.startsWith(uploadDir) && Files.exists(oldFile)) {
                try {
                    Files.delete(oldFile);
                } catch (IOException e) {
                    log.warn("Impossible de supprimer l'ancienne photo: {}", oldPath, e);
                }
            }
        }
        log.info("Photo de profil enregistrée pour clientId={} filename={}", client.getId(), filename);
        return filename;
    }

    /**
     * Retourne les octets et le content-type de la photo de profil (S3 ou disque local).
     * Le paramètre filenameOrKey est soit un simple filename (local) soit la clé S3 (clients/...).
     */
    public Optional<PhotoBytes> getPhotoBytes(Client client, String filenameOrKey) {
        if (filenameOrKey == null || client.getProfilePhotoPath() == null) {
            return Optional.empty();
        }
        if (!filenameOrKey.equals(client.getProfilePhotoPath())) {
            return Optional.empty();
        }
        String path = client.getProfilePhotoPath();
        if (path.startsWith(S3_KEY_PREFIX)) {
            return identityDocumentStorageService.downloadDocument(path)
                    .map(bytes -> new PhotoBytes(bytes, contentTypeFromPath(path)));
        }
        Path uploadDir = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        Path file = uploadDir.resolve(path).normalize();
        if (!file.startsWith(uploadDir) || !Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            byte[] bytes = Files.readAllBytes(file);
            String contentType = Files.probeContentType(file);
            if (contentType == null) contentType = "image/jpeg";
            return Optional.of(new PhotoBytes(bytes, contentType));
        } catch (IOException e) {
            log.warn("Erreur lecture photo profil {}", path, e);
            return Optional.empty();
        }
    }

    private static String contentTypeFromPath(String path) {
        if (path == null) return "image/jpeg";
        if (path.toLowerCase().endsWith(".png")) return "image/png";
        if (path.toLowerCase().endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    /**
     * Retourne le chemin absolu du fichier photo (uniquement en mode local).
     */
    public Optional<Path> getPhotoPath(Client client, String filename) {
        if (filename == null || !filename.equals(client.getProfilePhotoPath())) {
            return Optional.empty();
        }
        if (client.getProfilePhotoPath().startsWith(S3_KEY_PREFIX)) {
            return Optional.empty();
        }
        Path uploadDir = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        Path file = uploadDir.resolve(filename).normalize();
        if (!file.startsWith(uploadDir) || !Files.isRegularFile(file)) {
            return Optional.empty();
        }
        return Optional.of(file);
    }

    public record PhotoBytes(byte[] bytes, String contentType) {}
}
