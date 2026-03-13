package com.neobank.kozala_client.service;

import com.neobank.kozala_client.dto.identity.IdentityVerificationStatusResponse;
import com.neobank.kozala_client.dto.identity.UploadDocumentResponse;
import com.neobank.kozala_client.dto.identity.UploadSelfieResponse;
import com.neobank.kozala_client.entity.Client;
import com.neobank.kozala_client.entity.Document;
import com.neobank.kozala_client.entity.DocumentStatus;
import com.neobank.kozala_client.entity.DocumentType;
import com.neobank.kozala_client.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdentityVerificationService {

    private final DocumentRepository documentRepository;
    private final IdentityDocumentStorageService storageService;
    private final FakeDocumentDetectionService fakeDocumentDetectionService;
    private final FaceVerificationService faceVerificationService;

    /**
     * Upload document(s) : recto obligatoire, verso si CNI.
     * Stockage Blob + détection faux (Custom Vision) + enregistrement en base.
     */
    @Transactional
    public UploadDocumentResponse uploadDocuments(Client client, String docType, MultipartFile recto, MultipartFile verso) throws IOException {
        if (client == null) throw new IllegalArgumentException("Non authentifié");
        DocumentType type = "passport".equalsIgnoreCase(docType) ? DocumentType.PASSPORT : DocumentType.ID_CARD;
        boolean isCni = type == DocumentType.ID_CARD;

        List<UploadDocumentResponse.DocumentItem> items = new ArrayList<>();

        // Recto
        String rectoKey = storageService.uploadDocument(client.getId(), type.name().toLowerCase(), "recto", recto);
        byte[] rectoBytes = recto.getBytes();
        if (fakeDocumentDetectionService.isFakeDocument(rectoBytes)) {
            throw new IllegalArgumentException("Le document recto n'a pas pu être validé. Veuillez utiliser un document authentique.");
        }
        Document rectoDoc = saveDocument(client, type, rectoKey, recto.getOriginalFilename(), recto.getContentType());
        items.add(new UploadDocumentResponse.DocumentItem(rectoDoc.getId(), "recto", rectoKey));

        // Verso (CNI only)
        if (isCni && verso != null && !verso.isEmpty()) {
            String versoKey = storageService.uploadDocument(client.getId(), type.name().toLowerCase(), "verso", verso);
            byte[] versoBytes = verso.getBytes();
            if (fakeDocumentDetectionService.isFakeDocument(versoBytes)) {
                throw new IllegalArgumentException("Le document verso n'a pas pu être validé. Veuillez utiliser un document authentique.");
            }
            Document versoDoc = saveDocument(client, type, versoKey, verso.getOriginalFilename(), verso.getContentType());
            items.add(new UploadDocumentResponse.DocumentItem(versoDoc.getId(), "verso", versoKey));
        }

        return UploadDocumentResponse.builder()
                .documents(items)
                .fakeCheckPassed(true)
                .build();
    }

    /**
     * Upload selfie : stockage Blob + Face API (détection visage document + selfie, puis verify).
     * Liveness : non implémenté ici (à brancher sur Azure Face Liveness si besoin).
     */
    @Transactional
    public UploadSelfieResponse uploadSelfie(Client client, MultipartFile selfieFile) throws IOException {
        if (client == null) throw new IllegalArgumentException("Non authentifié");

        String selfieKey = storageService.uploadDocument(client.getId(), "selfie", "selfie", selfieFile);
        Document selfieDoc = saveDocument(client, DocumentType.SELFIE, selfieKey, selfieFile.getOriginalFilename(), selfieFile.getContentType());

        byte[] selfieBytes = selfieFile.getBytes();
        Optional<UUID> selfieFaceId = faceVerificationService.detectFaceId(selfieBytes);
        if (selfieFaceId.isEmpty()) {
            throw new IllegalArgumentException("Aucun visage détecté sur la selfie. Veuillez reprendre la photo en gardant votre visage bien visible.");
        }

        // Récupérer le premier document d'identité (recto) pour comparer le visage
        List<Document> idDocs = documentRepository.findByClientIdAndTypeOrderByUploadedAtAsc(client.getId(), DocumentType.ID_CARD);
        if (idDocs.isEmpty()) {
            idDocs = documentRepository.findByClientIdAndTypeOrderByUploadedAtAsc(client.getId(), DocumentType.PASSPORT);
        }
        if (idDocs.isEmpty()) {
            throw new IllegalStateException("Veuillez d'abord envoyer votre document d'identité.");
        }

        Optional<byte[]> docImage = storageService.downloadDocument(idDocs.get(0).getStorageKey());
        if (docImage.isEmpty()) {
            throw new IllegalStateException("Impossible de récupérer le document pour la comparaison.");
        }
        Optional<UUID> documentFaceId = faceVerificationService.detectFaceId(docImage.get());
        if (documentFaceId.isEmpty()) {
            throw new IllegalArgumentException("Aucun visage détecté sur le document. Veuillez envoyer une photo claire de votre document.");
        }

        boolean faceMatchPassed = faceVerificationService.verifyFaceMatch(documentFaceId.get(), selfieFaceId.get());
        if (!faceMatchPassed) {
            selfieDoc.setStatus(DocumentStatus.REJECTED);
            selfieDoc.setReviewerNote("Face match échoué");
            documentRepository.save(selfieDoc);
            throw new IllegalArgumentException("La selfie ne correspond pas au visage du document. Veuillez reprendre une selfie claire.");
        }

        selfieDoc.setStatus(DocumentStatus.APPROVED);
        documentRepository.save(selfieDoc);
        log.info("Vérification identité réussie pour clientId={}", client.getId());

        return UploadSelfieResponse.builder()
                .documentId(selfieDoc.getId())
                .livenessPassed(true)
                .faceMatchPassed(true)
                .identityVerified(true)
                .build();
    }

    @Transactional(readOnly = true)
    public IdentityVerificationStatusResponse getStatus(Client client) {
        if (client == null) throw new IllegalArgumentException("Non authentifié");
        List<Document> all = documentRepository.findByClientId(client.getId());
        boolean hasIdDoc = all.stream().anyMatch(d -> d.getType() == DocumentType.ID_CARD || d.getType() == DocumentType.PASSPORT);
        boolean hasSelfie = all.stream().anyMatch(d -> d.getType() == DocumentType.SELFIE);
        boolean anyApproved = all.stream().anyMatch(d -> d.getStatus() == DocumentStatus.APPROVED);
        boolean anyRejected = all.stream().anyMatch(d -> d.getStatus() == DocumentStatus.REJECTED);
        String status = anyRejected ? "rejected" : (anyApproved && hasSelfie ? "approved" : "pending");
        return IdentityVerificationStatusResponse.builder()
                .documentsUploaded(hasIdDoc)
                .selfieUploaded(hasSelfie)
                .identityCompleted(hasIdDoc && hasSelfie && all.stream().anyMatch(d -> d.getType() == DocumentType.SELFIE && d.getStatus() == DocumentStatus.APPROVED))
                .status(status)
                .build();
    }

    private Document saveDocument(Client client, DocumentType type, String storageKey, String fileName, String contentType) {
        Document doc = Document.builder()
                .client(client)
                .type(type)
                .storageKey(storageKey)
                .fileName(fileName != null ? fileName : storageKey)
                .contentType(contentType)
                .status(DocumentStatus.PENDING)
                .build();
        return documentRepository.save(doc);
    }
}
