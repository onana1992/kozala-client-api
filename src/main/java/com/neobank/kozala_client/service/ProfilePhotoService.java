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

    private final ClientRepository clientRepository;
    private final ProfilePhotoProperties properties;

    /**
     * Enregistre la photo de profil du client : sauvegarde le fichier sur disque,
     * met à jour le client, supprime l'ancienne photo si elle existait.
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
        Path uploadDir = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
        String extension = contentType.contains("png") ? "png" : contentType.contains("webp") ? "webp" : "jpg";
        String filename = UUID.randomUUID() + "." + extension;
        Path targetFile = uploadDir.resolve(filename).normalize();
        if (!targetFile.startsWith(uploadDir)) {
            throw new SecurityException("Nom de fichier invalide");
        }
        file.transferTo(targetFile.toFile());

        String oldPath = client.getProfilePhotoPath();
        client.setProfilePhotoPath(filename);
        clientRepository.save(client);

        if (oldPath != null && !oldPath.isEmpty()) {
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
     * Retourne le chemin absolu du fichier photo du client si il correspond au filename demandé.
     */
    public Optional<Path> getPhotoPath(Client client, String filename) {
        if (filename == null || !filename.equals(client.getProfilePhotoPath())) {
            return Optional.empty();
        }
        Path uploadDir = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        Path file = uploadDir.resolve(filename).normalize();
        if (!file.startsWith(uploadDir) || !Files.isRegularFile(file)) {
            return Optional.empty();
        }
        return Optional.of(file);
    }
}
