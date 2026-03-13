package com.neobank.kozala_client.dto.identity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadDocumentResponse {

    private List<DocumentItem> documents;
    private boolean fakeCheckPassed;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentItem {
        private Long documentId;
        private String side; // "recto", "verso"
        private String storageKey;
    }
}
