package com.ragplatform.domain.repository;

import com.ragplatform.domain.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Page<Document> findByKnowledgeSourceIdOrderByCreatedAtDesc(UUID knowledgeSourceId, Pageable pageable);

    Optional<Document> findByKnowledgeSourceIdAndChecksum(UUID knowledgeSourceId, String checksum);

    Optional<Document> findByKnowledgeSourceIdAndExternalId(UUID knowledgeSourceId, String externalId);

    @Modifying
    @Query("DELETE FROM Document d WHERE d.knowledgeSource.id = :knowledgeSourceId")
    void deleteByKnowledgeSourceId(@Param("knowledgeSourceId") UUID knowledgeSourceId);

    long countByKnowledgeSourceId(UUID knowledgeSourceId);
}
