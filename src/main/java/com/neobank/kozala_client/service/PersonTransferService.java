package com.neobank.kozala_client.service;

import com.neobank.kozala_client.dto.remote.RemoteOpsTransferRequest;
import com.neobank.kozala_client.dto.remote.RemoteOpsTransferResponseDto;
import com.neobank.kozala_client.dto.transfer.CreatePersonTransferRequest;
import com.neobank.kozala_client.dto.transfer.PersonTransferResponse;
import com.neobank.kozala_client.entity.Beneficiary;
import com.neobank.kozala_client.entity.Client;
import com.neobank.kozala_client.repository.BeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PersonTransferService {

    private static final String DEFAULT_CURRENCY = "XAF";

    private final BeneficiaryRepository beneficiaryRepository;
    private final RemoteOpsTransferService remoteOpsTransferService;

    @Transactional(readOnly = true)
    public PersonTransferResponse createPersonTransfer(Client client, CreatePersonTransferRequest request) {
        if (client == null) {
            throw new IllegalArgumentException("Non authentifié");
        }
        Beneficiary beneficiary = beneficiaryRepository
                .findByIdAndOwnerClientId(request.getBeneficiaryId(), client.getId())
                .orElseThrow(() -> new IllegalArgumentException("Bénéficiaire introuvable"));

        if (beneficiary.getBeneficiaryClientId() == null) {
            throw new IllegalArgumentException(
                    "Ce bénéficiaire n'est pas un client inscrit : le virement vers son compte courant n'est pas disponible.");
        }

        String currency = StringUtils.hasText(request.getCurrency())
                ? request.getCurrency().trim()
                : DEFAULT_CURRENCY;

        RemoteOpsTransferRequest.RemoteOpsTransferRequestBuilder rb = RemoteOpsTransferRequest.builder()
                .fromAccountId(request.getFromAccountId())
                .toClientId(beneficiary.getBeneficiaryClientId())
                .amount(request.getAmount())
                .currency(currency);

        if (StringUtils.hasText(request.getDescription())) {
            rb.description(request.getDescription().trim());
        }

        RemoteOpsTransferResponseDto remote = remoteOpsTransferService.create(rb.build());
        return map(remote, request.getAmount(), currency);
    }

    private static PersonTransferResponse map(
            RemoteOpsTransferResponseDto remote,
            BigDecimal fallbackAmount,
            String fallbackCurrency) {
        if (remote == null) {
            return PersonTransferResponse.builder().build();
        }
        BigDecimal amt = remote.getAmount() != null ? remote.getAmount() : fallbackAmount;
        String cur = StringUtils.hasText(remote.getCurrency()) ? remote.getCurrency() : fallbackCurrency;
        return PersonTransferResponse.builder()
                .id(remote.getId())
                .transferNumber(remote.getTransferNumber())
                .status(remote.getStatus())
                .amount(amt)
                .currency(cur)
                .build();
    }
}
