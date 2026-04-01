package com.neobank.kozala_client.service;

import com.neobank.kozala_client.dto.auth.ClientAccountDto;
import com.neobank.kozala_client.dto.remote.RemoteOwnTransferRequest;
import com.neobank.kozala_client.dto.remote.RemoteOpsTransferResponseDto;
import com.neobank.kozala_client.dto.transfer.CreateOwnTransferRequest;
import com.neobank.kozala_client.dto.transfer.PersonTransferResponse;
import com.neobank.kozala_client.entity.Client;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnTransferService {

    private static final String DEFAULT_CURRENCY = "XAF";

    private final RemoteClientAccountsService remoteClientAccountsService;
    private final RemoteOpsTransferService remoteOpsTransferService;

    @Transactional(readOnly = true)
    public PersonTransferResponse createOwnTransfer(Client client, CreateOwnTransferRequest request) {
        if (client == null) {
            throw new IllegalArgumentException("Non authentifié");
        }
        if (Objects.equals(request.getFromAccountId(), request.getToAccountId())) {
            throw new IllegalArgumentException("Les comptes à débiter et à créditer doivent être distincts.");
        }

        List<ClientAccountDto> accounts = remoteClientAccountsService.fetchAccounts(client.getId());
        Set<Long> ownedIds = accounts.stream().map(ClientAccountDto::getId).collect(Collectors.toSet());
        if (!ownedIds.contains(request.getFromAccountId()) || !ownedIds.contains(request.getToAccountId())) {
            throw new IllegalArgumentException("Un ou plusieurs comptes ne sont pas autorisés pour votre profil.");
        }

        ClientAccountDto from = findById(accounts, request.getFromAccountId());
        ClientAccountDto to = findById(accounts, request.getToAccountId());
        String fromCur = normalizeCurrency(from);
        String toCur = normalizeCurrency(to);
        if (!fromCur.equalsIgnoreCase(toCur)) {
            throw new IllegalArgumentException("Les deux comptes doivent être dans la même devise.");
        }

        String currency = StringUtils.hasText(request.getCurrency())
                ? request.getCurrency().trim()
                : fromCur;
        if (!currency.equalsIgnoreCase(fromCur)) {
            throw new IllegalArgumentException("La devise du virement ne correspond pas aux comptes choisis.");
        }

        RemoteOwnTransferRequest.RemoteOwnTransferRequestBuilder rb = RemoteOwnTransferRequest.builder()
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .currency(currency);

        if (StringUtils.hasText(request.getDescription())) {
            rb.description(request.getDescription().trim());
        }

        RemoteOpsTransferResponseDto remote = remoteOpsTransferService.createOwn(rb.build());
        return map(remote, request.getAmount(), currency);
    }

    private static ClientAccountDto findById(List<ClientAccountDto> accounts, Long id) {
        return accounts.stream()
                .filter(a -> Objects.equals(a.getId(), id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable."));
    }

    private static String normalizeCurrency(ClientAccountDto a) {
        String c = a.getCurrency();
        return (c != null && !c.isBlank()) ? c.trim() : DEFAULT_CURRENCY;
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
