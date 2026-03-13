package com.neobank.kozala_client.controller;

import com.neobank.kozala_client.dto.ApiResponse;
import com.neobank.kozala_client.dto.beneficiary.AddBeneficiaryRequest;
import com.neobank.kozala_client.dto.beneficiary.BeneficiaryResponse;
import com.neobank.kozala_client.dto.beneficiary.LookupRegisteredRequest;
import com.neobank.kozala_client.dto.beneficiary.RegisteredClientItem;
import com.neobank.kozala_client.entity.Client;
import com.neobank.kozala_client.service.BeneficiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
@Tag(name = "Bénéficiaires", description = "Liste, ajout, suppression et lookup contacts inscrits")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @GetMapping
    @Operation(summary = "Liste des bénéficiaires", description = "Retourne la liste des bénéficiaires du client connecté.")
    public ResponseEntity<ApiResponse<List<BeneficiaryResponse>>> list(@AuthenticationPrincipal Client client) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }
        List<BeneficiaryResponse> list = beneficiaryService.listByOwner(client);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping
    @Operation(summary = "Ajouter un bénéficiaire", description = "Ajoute un bénéficiaire par téléphone et nom. Le téléphone est normalisé (237 / +237 / 9 chiffres).")
    public ResponseEntity<ApiResponse<BeneficiaryResponse>> add(
            @AuthenticationPrincipal Client client,
            @Valid @RequestBody AddBeneficiaryRequest request) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }
        try {
            BeneficiaryResponse created = beneficiaryService.add(client, request.getPhone(), request.getFullName());
            return ResponseEntity.ok(ApiResponse.success("Bénéficiaire ajouté", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un bénéficiaire")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Client client,
            @PathVariable Long id) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }
        beneficiaryService.delete(client, id);
        return ResponseEntity.ok(ApiResponse.success("Bénéficiaire supprimé", null));
    }

    @PostMapping("/lookup-registered")
    @Operation(summary = "Rechercher les contacts inscrits", description = "Pour la page répertoire : renvoie les clients inscrits dont le téléphone est dans la liste, et qui ne sont pas déjà bénéficiaires.")
    public ResponseEntity<ApiResponse<List<RegisteredClientItem>>> lookupRegistered(
            @AuthenticationPrincipal Client client,
            @Valid @RequestBody LookupRegisteredRequest request) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }
        try {
            List<RegisteredClientItem> items = beneficiaryService.lookupRegisteredNotYetBeneficiaries(client, request.getPhones());
            return ResponseEntity.ok(ApiResponse.success(items));
        } catch (Exception e) {
            log.error("Lookup registered failed for clientId={}", client != null ? client.getId() : null, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Recherche temporairement indisponible. Réessayez plus tard."));
        }
    }
}
