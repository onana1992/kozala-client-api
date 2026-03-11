package com.neobank.kozala_client.service;

import com.neobank.kozala_client.dto.ClientRequest;
import com.neobank.kozala_client.dto.ClientResponse;
import com.neobank.kozala_client.entity.Address;
import com.neobank.kozala_client.entity.Client;
import com.neobank.kozala_client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<ClientResponse> findAll() {
        return clientRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClientResponse findById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'id: " + id));
        return toResponse(client);
    }

    @Transactional
    public ClientResponse create(ClientRequest request) {
        if (clientRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un client avec cet email existe déjà");
        }
        if (clientRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Un client avec ce téléphone existe déjà");
        }
        String passwordHash = (request.getPassword() != null && !request.getPassword().isBlank())
                ? passwordEncoder.encode(request.getPassword())
                : null;
        Client client = Client.builder()
                .type(request.getType())
                .displayName(request.getDisplayName())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordHash)
                .build();
        client = clientRepository.save(client);
        return toResponse(client);
    }

    @Transactional
    public ClientResponse update(Long id, ClientRequest request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'id: " + id));
        client.setType(request.getType());
        client.setDisplayName(request.getDisplayName());
        client.setFirstName(request.getFirstName());
        client.setLastName(request.getLastName());
        client.setEmail(request.getEmail());
        client.setPhone(request.getPhone());
        client = clientRepository.save(client);
        return toResponse(client);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new RuntimeException("Client non trouvé avec l'id: " + id);
        }
        clientRepository.deleteById(id);
    }

    private ClientResponse toResponse(Client client) {
        return toResponseWithAddress(client, null);
    }

    /** Réponse client avec champs profil + adresse principale (optionnelle). */
    public ClientResponse toResponseWithAddress(Client client, Address primaryAddress) {
        ClientResponse r = ClientResponse.builder()
                .id(client.getId())
                .type(client.getType())
                .displayName(client.getDisplayName())
                .firstName(client.getFirstName())
                .lastName(client.getLastName())
                .gender(client.getGender())
                .birthDate(client.getBirthDate())
                .maritalStatus(client.getMaritalStatus())
                .email(client.getEmail())
                .phone(client.getPhone())
                .status(client.getStatus())
                .riskScore(client.getRiskScore())
                .pepFlag(client.getPepFlag())
                .rejectionReason(client.getRejectionReason())
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .build();
        if (primaryAddress != null) {
            r.setCountry(primaryAddress.getCountry());
            r.setRegion(primaryAddress.getState());
            r.setCity(primaryAddress.getCity());
            r.setFullAddress(primaryAddress.getLine1());
        }
        return r;
    }
}
