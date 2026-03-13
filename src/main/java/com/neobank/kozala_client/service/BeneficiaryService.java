package com.neobank.kozala_client.service;

import com.neobank.kozala_client.dto.beneficiary.BeneficiaryResponse;
import com.neobank.kozala_client.dto.beneficiary.RegisteredClientItem;
import com.neobank.kozala_client.entity.Beneficiary;
import com.neobank.kozala_client.entity.Client;
import com.neobank.kozala_client.repository.BeneficiaryRepository;
import com.neobank.kozala_client.repository.ClientLookupProjection;
import com.neobank.kozala_client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    /** Nombre max de numéros acceptés du client (limite anti-abus, filtrage en mémoire). */
    private static final int MAX_LOOKUP_PHONES = 15_000;
    /** Taille de page pour le lookup (évite de charger toute la table en mémoire). */
    private static final int LOOKUP_PAGE_SIZE = 2_000;

    private final BeneficiaryRepository beneficiaryRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> listByOwner(Client owner) {
        if (owner == null) throw new IllegalArgumentException("Non authentifié");
        return beneficiaryRepository.findByOwnerClientIdOrderByCreatedAtDesc(owner.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BeneficiaryResponse add(Client owner, String phone, String fullName) {
        if (owner == null) throw new IllegalArgumentException("Non authentifié");
        String normalized = OtpService.normalizePhone(phone);
        if (normalized.length() < 9) {
            throw new IllegalArgumentException("Numéro de téléphone invalide");
        }
        if (beneficiaryRepository.existsByOwnerClientIdAndPhone(owner.getId(), normalized)) {
            throw new IllegalArgumentException("Ce bénéficiaire est déjà dans votre liste");
        }
        // Ne pas s'ajouter soi-même
        if (normalized.equals(owner.getPhone())) {
            throw new IllegalArgumentException("Vous ne pouvez pas vous ajouter comme bénéficiaire");
        }
        Client beneficiaryClient = clientRepository.findByPhone(normalized).orElse(null);
        Beneficiary b = Beneficiary.builder()
                .ownerClientId(owner.getId())
                .beneficiaryClientId(beneficiaryClient != null ? beneficiaryClient.getId() : null)
                .phone(normalized)
                .fullName(fullName != null ? fullName.trim() : "")
                .build();
        b = beneficiaryRepository.save(b);
        return toResponse(b);
    }

    @Transactional
    public void delete(Client owner, Long beneficiaryId) {
        if (owner == null) throw new IllegalArgumentException("Non authentifié");
        beneficiaryRepository.deleteByIdAndOwnerClientId(beneficiaryId, owner.getId());
    }

    /**
     * Retourne les clients inscrits dont le téléphone est dans la liste envoyée par le client,
     * exclus du propriétaire et déjà bénéficiaires.
     * Approche : pagination (pages de clients id/phone/displayName), filtrage en mémoire par page.
     */
    @Transactional(readOnly = true, timeout = 60)
    public List<RegisteredClientItem> lookupRegisteredNotYetBeneficiaries(Client owner, List<String> normalizedPhones) {
        if (owner == null || normalizedPhones == null || normalizedPhones.isEmpty()) {
            return List.of();
        }
        Set<String> clientPhones = normalizedPhones.stream()
                .map(OtpService::normalizePhone)
                .filter(p -> p != null && p.length() >= 9)
                .limit(MAX_LOOKUP_PHONES)
                .collect(Collectors.toSet());
        if (clientPhones.isEmpty()) {
            return List.of();
        }

        Set<String> alreadyBeneficiaryPhones = beneficiaryRepository.findByOwnerClientIdOrderByCreatedAtDesc(owner.getId()).stream()
                .map(Beneficiary::getPhone)
                .collect(Collectors.toSet());
        String ownerPhone = OtpService.normalizePhone(owner.getPhone());
        Long ownerId = owner.getId();

        List<RegisteredClientItem> result = new ArrayList<>();
        int page = 0;
        boolean hasMore;
        do {
            var pageable = PageRequest.of(page, LOOKUP_PAGE_SIZE);
            var clientPage = clientRepository.findAllForLookup(pageable);
            List<ClientLookupProjection> content = clientPage.getContent();
            hasMore = clientPage.hasNext();

            content.stream()
                    .filter(c -> clientPhones.contains(OtpService.normalizePhone(c.getPhone())))
                    .filter(c -> !c.getId().equals(ownerId) && !ownerPhone.equals(OtpService.normalizePhone(c.getPhone())))
                    .filter(c -> !alreadyBeneficiaryPhones.contains(OtpService.normalizePhone(c.getPhone())))
                    .map(c -> RegisteredClientItem.builder()
                            .id(c.getId())
                            .phone(c.getPhone())
                            .displayName(c.getDisplayName() != null ? c.getDisplayName() : c.getPhone())
                            .build())
                    .forEach(result::add);
            page++;
        } while (hasMore);

        return result;
    }

    private BeneficiaryResponse toResponse(Beneficiary b) {
        return BeneficiaryResponse.builder()
                .id(b.getId())
                .phone(b.getPhone())
                .fullName(b.getFullName())
                .beneficiaryClientId(b.getBeneficiaryClientId())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
