package com.neobank.kozala_client.controller;

import com.neobank.kozala_client.dto.ApiResponse;
import com.neobank.kozala_client.dto.ClientResponse;
import com.neobank.kozala_client.dto.profile.ProfileUpdateRequest;
import com.neobank.kozala_client.dto.profile.VerificationStatusResponse;
import com.neobank.kozala_client.dto.profile.SendEmailCodeRequest;
import com.neobank.kozala_client.dto.profile.VerifyEmailCodeRequest;
import com.neobank.kozala_client.entity.Client;
import com.neobank.kozala_client.service.ProfilePhotoService;
import com.neobank.kozala_client.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profil", description = "Photo, email, détail du profil")
public class ProfileController {

    private final ProfilePhotoService profilePhotoService;
    private final ProfileService profileService;

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Mettre à jour la photo de profil", description = "Envoi d'une image (JPEG, PNG ou WebP, max 5 Mo). Authentification requise.")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProfilePhoto(
            @AuthenticationPrincipal Client client,
            @RequestParam("file") MultipartFile file) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }
        try {
            String filenameOrKey = profilePhotoService.saveProfilePhoto(client, file);
            String profilePhotoUrl = "/api/profile/photos?key=" + URLEncoder.encode(filenameOrKey, StandardCharsets.UTF_8);
            return ResponseEntity.ok(ApiResponse.success("Photo enregistrée", Map.of("profilePhotoUrl", profilePhotoUrl)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Erreur lors de l'enregistrement"));
        }
    }

    @GetMapping(value = "/photos", params = "key")
    @SecurityRequirements
    @Operation(summary = "Récupérer la photo de profil", description = "Accès public. key = nom fichier ou clé S3 (ex. clients/1/profile/uuid.jpg), passé en query pour éviter 400 avec %2F dans le path.")
    public ResponseEntity<byte[]> getProfilePhotoByKey(@RequestParam("key") String key) {
        return profilePhotoService.getPhotoBytesByPath(key)
                .map(p -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(p.contentType()))
                        .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                        .body(p.bytes()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/verification-status")
    @Operation(summary = "Statut de vérification du compte", description = "Retourne l'état de chaque étape (email, profil, identité) pour afficher les badges.")
    public ResponseEntity<ApiResponse<VerificationStatusResponse>> getVerificationStatus(@AuthenticationPrincipal Client client) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }
        return ResponseEntity.ok(ApiResponse.success(profileService.getVerificationStatus(client)));
    }

    @GetMapping("/me")
    @Operation(summary = "Profil du client connecté", description = "Retourne les infos du client authentifié.")
    public ResponseEntity<ApiResponse<ClientResponse>> getMe(@AuthenticationPrincipal Client client) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }
        return ResponseEntity.ok(ApiResponse.success(profileService.getMe(client)));
    }

    @PutMapping("/me")
    @Operation(summary = "Mettre à jour le profil", description = "Met à jour prénom, nom et nom d'affichage du client connecté.")
    public ResponseEntity<ApiResponse<ClientResponse>> updateMe(
            @AuthenticationPrincipal Client client,
            @Valid @RequestBody ProfileUpdateRequest request) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.success("Profil mis à jour", profileService.updateMe(client, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/send-email-code")
    @Operation(summary = "Envoyer le code de vérification email", description = "Génère un code 6 chiffres et l'envoie par email (en dev : loggé).")
    public ResponseEntity<ApiResponse<Void>> sendEmailCode(
            @AuthenticationPrincipal Client client,
            @Valid @RequestBody SendEmailCodeRequest request) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }
        try {
            profileService.sendEmailCode(client, request.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Code envoyé", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/verify-email-code")
    @Operation(summary = "Vérifier le code email", description = "Valide le code et associe l'email au compte.")
    public ResponseEntity<ApiResponse<Void>> verifyEmailCode(
            @AuthenticationPrincipal Client client,
            @Valid @RequestBody VerifyEmailCodeRequest request) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }
        try {
            profileService.verifyEmailCode(client, request.getEmail(), request.getCode());
            return ResponseEntity.ok(ApiResponse.success("Email vérifié", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
