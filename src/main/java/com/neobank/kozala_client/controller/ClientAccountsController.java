package com.neobank.kozala_client.controller;

import com.neobank.kozala_client.dto.ApiResponse;
import com.neobank.kozala_client.dto.auth.OpenSavingsRequest;
import com.neobank.kozala_client.dto.auth.OpenTermDepositRequest;
import com.neobank.kozala_client.dto.remote.RemoteBankAccountDto;
import com.neobank.kozala_client.dto.remote.RemoteDepositProductCatalogItemDto;
import com.neobank.kozala_client.entity.Client;
import com.neobank.kozala_client.service.RemoteDepositProductCatalogException;
import com.neobank.kozala_client.service.RemoteDepositProductCatalogService;
import com.neobank.kozala_client.service.RemoteOpenSavingsException;
import com.neobank.kozala_client.service.RemoteOpenSavingsService;
import com.neobank.kozala_client.service.RemoteOpenTermDepositException;
import com.neobank.kozala_client.service.RemoteOpenTermDepositService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/client/accounts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(
        name = "Comptes client",
        description = "Proxy vers l’API distante : GET /api/client/accounts/deposit-product-catalog, "
                + "POST /api/client/accounts/open-savings, POST /api/client/accounts/open-term-deposit.")
public class ClientAccountsController {

    private final RemoteDepositProductCatalogService depositProductCatalogService;
    private final RemoteOpenSavingsService remoteOpenSavingsService;
    private final RemoteOpenTermDepositService remoteOpenTermDepositService;

    @GetMapping("/deposit-product-catalog")
    @Operation(
            summary = "Catalogue des produits de dépôt",
            description = "GET sans paramètre. Données issues de l’API distante, appelée avec le jeton service "
                    + "(app.remote-api.bearer-token)."
    )
    public ResponseEntity<ApiResponse<List<RemoteDepositProductCatalogItemDto>>> getDepositProductCatalog(
            @AuthenticationPrincipal Client client) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }
        try {
            List<RemoteDepositProductCatalogItemDto> data = depositProductCatalogService.fetchCatalog();
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (RemoteDepositProductCatalogException e) {
            return ResponseEntity.status(502).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/open-savings")
    @Operation(
            summary = "Demande d’ouverture compte épargne",
            description = "Proxy POST vers l’API distante (jeton service). clientId = id client en base."
    )
    public ResponseEntity<ApiResponse<RemoteBankAccountDto>> openSavings(
            @Valid @RequestBody OpenSavingsRequest request,
            @AuthenticationPrincipal Client client) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }
        try {
            RemoteBankAccountDto data = remoteOpenSavingsService.open(request, client.getId());
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (RemoteOpenSavingsException e) {
            return ResponseEntity.status(502).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/open-term-deposit")
    @Operation(
            summary = "Demande d’ouverture dépôt à terme",
            description = "Proxy POST vers l’API distante (jeton service). clientId = id client en base."
    )
    public ResponseEntity<ApiResponse<RemoteBankAccountDto>> openTermDeposit(
            @Valid @RequestBody OpenTermDepositRequest request,
            @AuthenticationPrincipal Client client) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }
        try {
            RemoteBankAccountDto data = remoteOpenTermDepositService.open(request, client.getId());
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (RemoteOpenTermDepositException e) {
            return ResponseEntity.status(502).body(ApiResponse.error(e.getMessage()));
        }
    }
}
