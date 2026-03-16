package com.neobank.kozala_client.repository;

import com.neobank.kozala_client.entity.Document;
import com.neobank.kozala_client.entity.DocumentStatus;
import com.neobank.kozala_client.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByClientId(Long clientId);

    List<Document> findByClientIdAndType(Long clientId, DocumentType type);

    List<Document> findByClientIdAndTypeOrderByUploadedAtAsc(Long clientId, DocumentType type);

    List<Document> findByClientIdAndTypeOrderByUploadedAtDesc(Long clientId, DocumentType type);

    List<Document> findByClientIdAndStatus(Long clientId, DocumentStatus status);

    Optional<Document> findByClientIdAndTypeAndStatus(Long clientId, DocumentType type, DocumentStatus status);
}
