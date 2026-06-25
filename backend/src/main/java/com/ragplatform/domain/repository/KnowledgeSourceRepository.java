package com.ragplatform.domain.repository;

import com.ragplatform.domain.enums.SyncStatus;
import com.ragplatform.domain.model.KnowledgeSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface KnowledgeSourceRepository extends JpaRepository<KnowledgeSource, UUID> {

    Page<KnowledgeSource> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Modifying
    @Query("UPDATE KnowledgeSource ks SET ks.syncStatus = :status WHERE ks.id = :id")
    void updateSyncStatus(@Param("id") UUID id, @Param("status") SyncStatus status);

    @Modifying
    @Query("UPDATE KnowledgeSource ks SET ks.documentCount = :count WHERE ks.id = :id")
    void updateDocumentCount(@Param("id") UUID id, @Param("count") int count);

    @Modifying
    @Query("UPDATE KnowledgeSource ks SET ks.lastSyncedAt = :now WHERE ks.id = :id")
    void updateLastSyncedAt(@Param("id") UUID id, @Param("now") OffsetDateTime now);
}
