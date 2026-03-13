package com.neobank.kozala_client.dto.identity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadSelfieResponse {

    private Long documentId;
    private boolean livenessPassed;
    private boolean faceMatchPassed;
    private boolean identityVerified;
}
