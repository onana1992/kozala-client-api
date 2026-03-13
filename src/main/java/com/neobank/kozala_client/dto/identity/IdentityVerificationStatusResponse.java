package com.neobank.kozala_client.dto.identity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdentityVerificationStatusResponse {

    private boolean documentsUploaded;
    private boolean selfieUploaded;
    private boolean identityCompleted;
    private String status; // "pending", "approved", "rejected"
}
