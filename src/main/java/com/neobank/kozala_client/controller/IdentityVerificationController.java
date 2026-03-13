package com.neobank.kozala_client.controller;

import com.neobank.kozala_client.dto.ApiResponse;
import com.neobank.kozala_client.dto.identity.IdentityVerificationStatusResponse;
import com.neobank.kozala_client.dto.identity.UploadDocumentResponse;
import com.neobank.kozala_client.dto.identity.UploadSelfieResponse;
import com.neobank.kozala_client.entity.Client;
import com.neobank.kozala_client.service.IdentityVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/identity-verification")
@RequiredArgsConstructor
@Tag(name = "Vérification d'identité", description = "Upload document, selfie, statut (Azure Blob, Custom Vision, Face API)")
public class IdentityVerificationController {

    private final IdentityVerificationService identityVerificationService;

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload document d'identité", description = "Recto obligatoire, verso obligatoire si CNI. Détection faux document (Custom Vision).")
    public ResponseEntity<ApiResponse<UploadDocumentResponse>> uploadDocuments(
            @AuthenticationPrincipal Client client,
            @RequestParam("docType") String docType,
            @RequestParam("recto") MultipartFile recto,
            @RequestParam(value = "verso", required = false) MultipartFile verso) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }
        if (recto == null || recto.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Le recto du document est requis."));
        }
        try {
            UploadDocumentResponse response = identityVerificationService.uploadDocuments(client, docType, recto, verso);
            return ResponseEntity.ok(ApiResponse.success("Document(s) enregistré(s)", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Erreur lors de l'enregistrement du fichier."));
        }
    }

    @PostMapping(value = "/selfie", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload selfie", description = "Comparaison visage document / selfie (Azure Face API). Liveness à brancher si besoin.")
    public ResponseEntity<ApiResponse<UploadSelfieResponse>> uploadSelfie(
            @AuthenticationPrincipal Client client,
            @RequestParam("file") MultipartFile file) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("La selfie est requise."));
        }
        try {
            UploadSelfieResponse response = identityVerificationService.uploadSelfie(client, file);
            return ResponseEntity.ok(ApiResponse.success("Vérification réussie", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Erreur lors de l'enregistrement de la selfie."));
        }
    }

    @GetMapping("/status")
    @Operation(summary = "Statut de la vérification identité", description = "Documents envoyés, selfie, identité complétée.")
    public ResponseEntity<ApiResponse<IdentityVerificationStatusResponse>> getStatus(@AuthenticationPrincipal Client client) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }
        IdentityVerificationStatusResponse status = identityVerificationService.getStatus(client);
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
