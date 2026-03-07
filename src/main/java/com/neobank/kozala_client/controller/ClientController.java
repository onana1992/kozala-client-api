package com.neobank.kozala_client.controller;

import com.neobank.kozala_client.dto.ApiResponse;
import com.neobank.kozala_client.dto.ClientRequest;
import com.neobank.kozala_client.dto.ClientResponse;
import com.neobank.kozala_client.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClientResponse>>> getAllClients() {
        List<ClientResponse> clients = clientService.findAll();
        return ResponseEntity.ok(ApiResponse.success(clients));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientResponse>> getClientById(@PathVariable Long id) {
        ClientResponse client = clientService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(client));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClientResponse>> createClient(@Valid @RequestBody ClientRequest request) {
        ClientResponse client = clientService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Client créé", client));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientResponse>> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody ClientRequest request) {
        ClientResponse client = clientService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Client mis à jour", client));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteClient(@PathVariable Long id) {
        clientService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Client supprimé", null));
    }
}
