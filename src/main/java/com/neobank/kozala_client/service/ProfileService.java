package com.neobank.kozala_client.service;

import com.neobank.kozala_client.dto.ClientResponse;
import com.neobank.kozala_client.dto.profile.ProfileUpdateRequest;
import com.neobank.kozala_client.dto.profile.VerificationStatusResponse;
import com.neobank.kozala_client.entity.Address;
import com.neobank.kozala_client.entity.AddressType;
import com.neobank.kozala_client.entity.Client;
import com.neobank.kozala_client.repository.AddressRepository;
import com.neobank.kozala_client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProfileService {

    private final ClientRepository clientRepository;
    private final AddressRepository addressRepository;
    private final OtpService otpService;
    private final ClientService clientService;

    @Transactional(readOnly = true)
    public ClientResponse getMe(Client client) {
        if (client == null) throw new IllegalArgumentException("Non authentifié");
        Client c = clientRepository.findById(client.getId())
                .orElseThrow(() -> new RuntimeException("Client introuvable"));
        var primary = addressRepository.findByClientIdAndPrimaryAddressTrue(c.getId());
        return clientService.toResponseWithAddress(c, primary.orElse(null));
    }

    /** Statut des étapes de vérification du compte (rafraîchi à chaque affichage de la page). */
    @Transactional(readOnly = true)
    public VerificationStatusResponse getVerificationStatus(Client client) {
        if (client == null) throw new IllegalArgumentException("Non authentifié");
        Client c = clientRepository.findById(client.getId())
                .orElseThrow(() -> new RuntimeException("Client introuvable"));
        boolean emailVerified = c.getEmail() != null
                && !c.getEmail().isBlank()
                && !c.getEmail().toLowerCase().startsWith("signup-");
        boolean personalFilled = c.getFirstName() != null && !c.getFirstName().isBlank()
                && c.getLastName() != null && !c.getLastName().isBlank()
                && c.getGender() != null && !c.getGender().isBlank()
                && c.getBirthDate() != null
                && c.getMaritalStatus() != null && !c.getMaritalStatus().isBlank();
        var primaryAddr = addressRepository.findByClientIdAndPrimaryAddressTrue(c.getId());
        boolean addressFilled = primaryAddr.map(a ->
                a.getCountry() != null && !a.getCountry().isBlank()
                        && a.getCity() != null && !a.getCity().isBlank()
                        && a.getLine1() != null && !a.getLine1().isBlank()
        ).orElse(false);
        boolean profileCompleted = personalFilled && addressFilled;
        boolean identityCompleted = c.getDocuments() != null && !c.getDocuments().isEmpty();
        return VerificationStatusResponse.builder()
                .emailVerified(emailVerified)
                .profileCompleted(profileCompleted)
                .identityCompleted(identityCompleted)
                .build();
    }

    @Transactional
    public ClientResponse updateMe(Client client, ProfileUpdateRequest request) {
        if (client == null) throw new IllegalArgumentException("Non authentifié");
        Client c = clientRepository.findById(client.getId())
                .orElseThrow(() -> new RuntimeException("Client introuvable"));
        c.setFirstName(request.getFirstName().trim());
        c.setLastName(request.getLastName().trim());
        c.setDisplayName((request.getFirstName().trim() + " " + request.getLastName().trim()).trim());
        if (c.getDisplayName().isEmpty()) c.setDisplayName("Client");
        if (request.getGender() != null) c.setGender(request.getGender().trim().isEmpty() ? null : request.getGender().trim());
        c.setBirthDate(request.getBirthDate());
        if (request.getMaritalStatus() != null) c.setMaritalStatus(request.getMaritalStatus().trim().isEmpty() ? null : request.getMaritalStatus().trim());
        clientRepository.save(c);

        boolean hasAddressData = (request.getCountry() != null && !request.getCountry().isBlank())
                || (request.getCity() != null && !request.getCity().isBlank())
                || (request.getFullAddress() != null && !request.getFullAddress().isBlank());
        if (hasAddressData) {
            Address addr = addressRepository.findByClientIdAndPrimaryAddressTrue(c.getId())
                    .orElse(Address.builder()
                            .client(c)
                            .type(AddressType.RESIDENTIAL)
                            .primaryAddress(true)
                            .line1("")
                            .city("")
                            .country("")
                            .build());
            addr.setLine1(request.getFullAddress() != null ? request.getFullAddress().trim() : "");
            addr.setCity(request.getCity() != null ? request.getCity().trim() : "");
            addr.setState(request.getRegion() != null ? request.getRegion().trim() : null);
            addr.setCountry(request.getCountry() != null ? request.getCountry().trim() : "");
            addressRepository.save(addr);
        }

        log.info("Profile updated for clientId={}", c.getId());
        var primary = addressRepository.findByClientIdAndPrimaryAddressTrue(c.getId());
        return clientService.toResponseWithAddress(c, primary.orElse(null));
    }

    /** Envoie un code OTP à l'email (stocké en Redis). À brancher : envoi email réel. */
    public void sendEmailCode(Client client, String email) {
        if (client == null) throw new IllegalArgumentException("Non authentifié");
        String normalized = OtpService.normalizeEmail(email);
        if (normalized.isEmpty()) throw new IllegalArgumentException("Email invalide");
        otpService.generateAndStoreEmail(normalized);
        // TODO: envoyer email (SendGrid, etc.). En dev le code est loggé par OtpService.
    }

    /** Vérifie le code et met à jour l'email du client. */
    @Transactional
    public void verifyEmailCode(Client client, String email, String code) {
        if (client == null) throw new IllegalArgumentException("Non authentifié");
        String normalized = OtpService.normalizeEmail(email);
        if (!otpService.validateEmail(normalized, code)) {
            throw new IllegalArgumentException("Code invalide ou expiré");
        }
        if (clientRepository.existsByEmail(normalized) && !normalized.equals(client.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé par un autre compte");
        }
        Client c = clientRepository.findById(client.getId())
                .orElseThrow(() -> new RuntimeException("Client introuvable"));
        c.setEmail(normalized);
        clientRepository.save(c);
        log.info("Email verified for clientId={} email={}", c.getId(), OtpService.maskEmail(email));
    }
}
