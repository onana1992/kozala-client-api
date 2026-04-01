package com.neobank.kozala_client.controller;

import com.neobank.kozala_client.dto.ApiResponse;
import com.neobank.kozala_client.dto.transfer.CreateOwnTransferRequest;
import com.neobank.kozala_client.dto.transfer.CreatePersonTransferRequest;
import com.neobank.kozala_client.dto.transfer.PersonTransferResponse;
import com.neobank.kozala_client.entity.Client;
import com.neobank.kozala_client.service.OwnTransferService;
import com.neobank.kozala_client.service.PersonTransferService;
import com.neobank.kozala_client.service.RemoteClientAccountsException;
import com.neobank.kozala_client.service.RemotePersonTransferException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client/transfers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(
        name = "Transferts client",
        description = "Proxy vers le core : /person (P2P), /own (entre comptes du client) ; jeton service.")
public class ClientTransfersController {

    private final PersonTransferService personTransferService;
    private final OwnTransferService ownTransferService;

    @PostMapping("/person")
    @Operation(
            summary = "Transfert vers une personne (bénéficiaire)",
            description = "Envoie POST /api/client/transfers/person au core (fromAccountId, toClientId du bénéficiaire inscrit, montant, devise). "
                    + "Bénéficiaire non inscrit : refus (400).")
    public ResponseEntity<ApiResponse<PersonTransferResponse>> createPersonTransfer(
            @Valid @RequestBody CreatePersonTransferRequest request,
            @AuthenticationPrincipal Client client) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifie"));
        }
        try {
            PersonTransferResponse data = personTransferService.createPersonTransfer(client, request);
            return ResponseEntity.ok(ApiResponse.success("Transfert envoye", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (RemotePersonTransferException e) {
            return ResponseEntity.status(502).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/own")
    @Operation(
            summary = "Virement entre vos comptes",
            description = "Vérifie que les deux comptes appartiennent au client (GET comptes distant), puis POST /api/client/transfers/own sur le core.")
    public ResponseEntity<ApiResponse<PersonTransferResponse>> createOwnTransfer(
            @Valid @RequestBody CreateOwnTransferRequest request,
            @AuthenticationPrincipal Client client) {
        if (client == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }
        try {
            PersonTransferResponse data = ownTransferService.createOwnTransfer(client, request);
            return ResponseEntity.ok(ApiResponse.success("Transfert effectué", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (RemotePersonTransferException e) {
            return ResponseEntity.status(502).body(ApiResponse.error(e.getMessage()));
        } catch (RemoteClientAccountsException e) {
            return ResponseEntity.status(502).body(ApiResponse.error(e.getMessage()));
        }
    }
}
