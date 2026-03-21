package com.neobank.kozala_client.controller;

import com.neobank.kozala_client.dto.ApiResponse;
import com.neobank.kozala_client.dto.remote.RemoteDepositProductCatalogItemDto;
import com.neobank.kozala_client.entity.Client;
import com.neobank.kozala_client.service.RemoteDepositProductCatalogException;
import com.neobank.kozala_client.service.RemoteDepositProductCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/client/accounts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Comptes client", description = "Proxy catalogue produits de dépôt vers l’API distante")
public class ClientAccountsController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final RemoteDepositProductCatalogService depositProductCatalogService;

    @GetMapping("/deposit-product-catalog")
    @Operation(
            summary = "Catalogue des produits de dépôt",
            description = "GET sans paramètre. Renvoie la liste telle que l’API distante (frais, plafonds, taux, périodes, pénalités)."
    )
    public ResponseEntity<ApiResponse<List<RemoteDepositProductCatalogItemDto>>> getDepositProductCatalog(
            HttpServletRequest request,
            @AuthenticationPrincipal Client client) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }
        String accessToken = extractBearerAccessToken(request);
        try {
            List<RemoteDepositProductCatalogItemDto> data = depositProductCatalogService.fetchCatalog(accessToken);
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (RemoteDepositProductCatalogException e) {
            return ResponseEntity.status(502).body(ApiResponse.error(e.getMessage()));
        }
    }

    private static String extractBearerAccessToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
