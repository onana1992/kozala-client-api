package com.neobank.kozala_client.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientTransactionsPageDto {

    private List<ClientTransactionDto> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
    private boolean last;
    private boolean first;
}
