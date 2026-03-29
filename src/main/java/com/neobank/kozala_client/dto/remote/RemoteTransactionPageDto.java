package com.neobank.kozala_client.dto.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * Page Spring Data renvoyée par le core pour {@code GET /api/client/transactions}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteTransactionPageDto {

    private List<RemoteTransactionDto> content = Collections.emptyList();
    private long totalElements;
    private int totalPages;
    private int size;
    /** Index de page (0-based), champ JSON {@code number}. */
    private int number;
    private boolean last;
    private boolean first;
}
