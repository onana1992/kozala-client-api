package com.neobank.kozala_client.controller;

import com.neobank.kozala_client.dto.ApiResponse;
import com.neobank.kozala_client.dto.transaction.ClientTransactionsPageDto;
import com.neobank.kozala_client.entity.Client;
import com.neobank.kozala_client.service.RemoteClientTransactionsException;
import com.neobank.kozala_client.service.RemoteClientTransactionsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/client/transactions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(
        name = "Transactions client",
        description = "Proxy vers le core : GET /api/client/transactions (consultation, jeton service).")
public class ClientTransactionsController {

    private final RemoteClientTransactionsService remoteClientTransactionsService;

    @GetMapping
    @Operation(
            summary = "Historique des transactions (proxy API distante)",
            description = "Query obligatoire clientId (id client en base), alignée sur le JWT. "
                    + "Paramètres optionnels : accountId, fromDate, toDate, type, status, page, size, sort (asc|desc).")
    public ResponseEntity<ApiResponse<ClientTransactionsPageDto>> listTransactions(
            @RequestParam("clientId") long clientId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "desc") String sort,
            @AuthenticationPrincipal Client client) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }
        if (clientId != client.getId()) {
            return ResponseEntity.status(403).body(ApiResponse.error("clientId incompatible avec le compte authentifié"));
        }
        if (size < 1 || size > 100) {
            return ResponseEntity.badRequest().body(ApiResponse.error("size doit être entre 1 et 100"));
        }
        if (page < 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("page doit être >= 0"));
        }
        try {
            ClientTransactionsPageDto data = remoteClientTransactionsService.fetchPage(
                    clientId, accountId, fromDate, toDate, type, status, page, size, sort);
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (RemoteClientTransactionsException e) {
            return ResponseEntity.status(502).body(ApiResponse.error(e.getMessage()));
        }
    }
}
