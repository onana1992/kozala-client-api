package com.neobank.kozala_client.controller;

import com.neobank.kozala_client.dto.ApiResponse;
import com.neobank.kozala_client.entity.Client;
import com.neobank.kozala_client.service.ProfilePhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profil", description = "Photo de profil (upload et consultation)")
public class ProfileController {

    private final ProfilePhotoService profilePhotoService;

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Mettre à jour la photo de profil", description = "Envoi d'une image (JPEG, PNG ou WebP, max 5 Mo). Authentification requise.")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProfilePhoto(
            @AuthenticationPrincipal Client client,
            @RequestParam("file") MultipartFile file) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }
        try {
            String filename = profilePhotoService.saveProfilePhoto(client, file);
            String profilePhotoUrl = "/api/profile/photos/" + filename;
            return ResponseEntity.ok(ApiResponse.success("Photo enregistrée", Map.of("profilePhotoUrl", profilePhotoUrl)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Erreur lors de l'enregistrement"));
        }
    }

    @GetMapping("/photos/{filename}")
    @Operation(summary = "Récupérer la photo de profil", description = "Retourne le fichier image du client connecté. Authentification requise.")
    public ResponseEntity<byte[]> getProfilePhoto(
            @AuthenticationPrincipal Client client,
            @PathVariable String filename) {
        if (client == null) {
            return ResponseEntity.status(401).build();
        }
        return profilePhotoService.getPhotoPath(client, filename)
                .map(path -> {
                    try {
                        byte[] bytes = Files.readAllBytes(path);
                        String contentType = Files.probeContentType(path);
                        if (contentType == null) contentType = "image/jpeg";
                        return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(contentType))
                                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                                .body(bytes);
                    } catch (IOException e) {
                        return ResponseEntity.notFound().<byte[]>build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
