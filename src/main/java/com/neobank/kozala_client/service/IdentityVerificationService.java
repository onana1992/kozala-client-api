package com.neobank.kozala_client.service;

import com.neobank.kozala_client.dto.identity.IdentityVerificationStatusResponse;
import com.neobank.kozala_client.dto.identity.UploadDocumentResponse;
import com.neobank.kozala_client.dto.identity.UploadSelfieResponse;
import com.neobank.kozala_client.entity.Client;
import com.neobank.kozala_client.entity.Document;
import com.neobank.kozala_client.entity.DocumentStatus;
import com.neobank.kozala_client.entity.DocumentType;
import com.neobank.kozala_client.entity.ReviewStatus;
import com.neobank.kozala_client.repository.ClientRepository;
import com.neobank.kozala_client.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdentityVerificationService {

    private final DocumentRepository documentRepository;
    private final ClientRepository clientRepository;
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
     * Upload selfie : stockage S3 + comparaison visage document/selfie (AWS Rekognition).
     */
    @Transactional
    public UploadSelfieResponse uploadSelfie(Client client, MultipartFile selfieFile) throws IOException {
        if (client == null) throw new IllegalArgumentException("Non authentifié");

        String selfieKey = storageService.uploadDocument(client.getId(), "selfie", "selfie", selfieFile);
        Document selfieDoc = saveDocument(client, DocumentType.SELFIE, selfieKey, selfieFile.getOriginalFilename(), selfieFile.getContentType());

        byte[] selfieBytes = selfieFile.getBytes();

        // Prendre le recto le plus récent (clé contenant "-recto") pour la comparaison visage
        List<Document> idDocs = documentRepository.findByClientIdAndTypeOrderByUploadedAtDesc(client.getId(), DocumentType.ID_CARD);
        if (idDocs.isEmpty()) {
            idDocs = documentRepository.findByClientIdAndTypeOrderByUploadedAtDesc(client.getId(), DocumentType.PASSPORT);
        }
        if (idDocs.isEmpty()) {
            throw new IllegalStateException("Veuillez d'abord envoyer votre document d'identité.");
        }
        Document rectoDoc = idDocs.stream()
                .filter(d -> d.getStorageKey() != null && d.getStorageKey().contains("-recto"))
                .findFirst()
                .orElse(idDocs.get(0));
        String storageKey = rectoDoc.getStorageKey();
        Optional<byte[]> docImage = storageService.downloadDocument(storageKey);
        if (docImage.isEmpty()) {
            log.warn("Impossible de télécharger le document pour la comparaison. clientId={} storageKey={} (vérifier que l'objet existe dans S3 et que les credentials ont s3:GetObject)", client.getId(), storageKey);
            throw new IllegalStateException(
                "Impossible de récupérer le document pour la comparaison. Vérifiez que le document a bien été envoyé avec cette application (stockage S3). Si vous venez de passer sur AWS, renvoyez d'abord votre document d'identité puis la selfie.");
        }

        FaceVerificationResult result = faceVerificationService.verifyDocumentAndSelfie(docImage.get(), selfieBytes);

        switch (result) {
            case NO_FACE_IN_SELFIE -> {
                deleteAllDocumentsForClient(client.getId());
                ensureClientIdentityReviewPending(client.getId());
                throw new IllegalArgumentException("Aucun visage détecté sur la selfie. Veuillez reprendre la photo en gardant votre visage bien visible.");
            }
            case NO_FACE_IN_DOCUMENT -> {
                deleteAllDocumentsForClient(client.getId());
                ensureClientIdentityReviewPending(client.getId());
                throw new IllegalArgumentException("Aucun visage détecté sur le document. Veuillez envoyer une photo claire de votre document.");
            }
            case NO_MATCH -> {
                deleteAllDocumentsForClient(client.getId());
                ensureClientIdentityReviewPending(client.getId());
                throw new IllegalArgumentException("La selfie ne correspond pas au visage du document. Veuillez reprendre une selfie claire.");
            }
            case DISABLED, MATCH -> {
                // DISABLED : accepter sans vérification ; MATCH : correspondance validée → identité en attente de revue (client.status = accepté reste manuel par le reviewer)
                selfieDoc.setStatus(DocumentStatus.APPROVED);
                documentRepository.save(selfieDoc);
                clientRepository.findById(client.getId()).ifPresent(cl -> {
                    cl.setIdentityReviewStatus(ReviewStatus.PENDING_REVIEW);
                    cl.updateStatusFromReviewStatuses();
                    clientRepository.save(cl);
                });
            }
        }

        log.info("Vérification identité réussie pour clientId={} (result={})", client.getId(), result);

        return UploadSelfieResponse.builder()
                .documentId(selfieDoc.getId())
                .livenessPassed(true)
                .faceMatchPassed(result == FaceVerificationResult.MATCH || result == FaceVerificationResult.DISABLED)
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

    /**
     * Supprime tous les documents du client : S3 puis base. Utilisé quand la vérification (selfie) échoue.
     * REQUIRES_NEW pour que les suppressions soient commitées même si l'appelant lance une exception.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteAllDocumentsForClient(Long clientId) {
        List<Document> documents = documentRepository.findByClientId(clientId);
        for (Document d : documents) {
            if (d.getStorageKey() != null && !d.getStorageKey().isBlank()) {
                storageService.deleteDocument(d.getStorageKey());
            }
        }
        documentRepository.deleteAll(documents);
        log.info("Tous les documents du client {} ont été supprimés (S3 + base)", clientId);
    }

    /** Remet le statut de revue identité à PENDING après un échec (documents supprimés). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureClientIdentityReviewPending(Long clientId) {
        clientRepository.findById(clientId).ifPresent(cl -> {
            cl.setIdentityReviewStatus(ReviewStatus.PENDING);
            cl.updateStatusFromReviewStatuses();
            clientRepository.save(cl);
            log.info("Client {} identity_review_status remis à PENDING après échec vérification", clientId);
        });
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
