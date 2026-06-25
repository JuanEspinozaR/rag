package com.ragplatform.application.service;

import com.ragplatform.application.dto.request.KnowledgeSourceRequest;
import com.ragplatform.application.dto.response.DocumentResponse;
import com.ragplatform.application.dto.response.KnowledgeSourceResponse;
import com.ragplatform.application.port.KnowledgeSourceUseCase;
import com.ragplatform.api.exception.ResourceNotFoundException;
import com.ragplatform.domain.model.Document;
import com.ragplatform.domain.model.KnowledgeSource;
import com.ragplatform.domain.repository.DocumentRepository;
import com.ragplatform.domain.repository.KnowledgeSourceRepository;
import com.ragplatform.domain.enums.SyncStatus;
import com.ragplatform.infrastructure.rag.DocumentSyncService;
import com.ragplatform.infrastructure.rag.KnowledgeSourceSyncTrigger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class KnowledgeSourceService implements KnowledgeSourceUseCase {

    private final KnowledgeSourceRepository knowledgeSourceRepository;
    private final DocumentRepository documentRepository;
    private final DocumentSyncService documentSyncService;
    private final KnowledgeSourceSyncTrigger syncTrigger;

    @Override
    public KnowledgeSourceResponse create(KnowledgeSourceRequest request) {
        KnowledgeSource source = KnowledgeSource.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .baseUrl(request.getBaseUrl())
                .config(request.getConfig())
                .build();
        KnowledgeSource saved = knowledgeSourceRepository.save(source);

        // Trigger sync in background after this transaction commits so the entity
        // is visible to the async thread when it reads from the DB.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                syncTrigger.trigger(saved);
            }
        });

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeSourceResponse findById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KnowledgeSourceResponse> findAll(Pageable pageable) {
        return knowledgeSourceRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    @Override
    public KnowledgeSourceResponse update(UUID id, KnowledgeSourceRequest request) {
        KnowledgeSource source = findOrThrow(id);
        source.setName(request.getName());
        source.setDescription(request.getDescription());
        source.setType(request.getType());
        source.setBaseUrl(request.getBaseUrl());
        source.setConfig(request.getConfig());
        return toResponse(knowledgeSourceRepository.save(source));
    }

    @Override
    public void delete(UUID id) {
        KnowledgeSource source = findOrThrow(id);
        documentSyncService.deleteVectorsByKnowledgeSource(id);
        knowledgeSourceRepository.delete(source);
    }

    @Override
    public void sync(UUID id) {
        KnowledgeSource source = findOrThrow(id);
        log.info("Queuing async sync for knowledge source: {} ({})", source.getName(), id);
        syncTrigger.trigger(source);
    }

    @Override
    public void syncAll() {
        List<KnowledgeSource> pending = knowledgeSourceRepository.findAll().stream()
                .filter(s -> s.getSyncStatus() == SyncStatus.PENDING
                        || s.getSyncStatus() == SyncStatus.FAILED)
                .toList();
        log.info("sync-all: triggering async sync for {} sources", pending.size());
        pending.forEach(syncTrigger::trigger);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> findDocuments(UUID knowledgeSourceId, Pageable pageable) {
        findOrThrow(knowledgeSourceId);
        return documentRepository
                .findByKnowledgeSourceIdOrderByCreatedAtDesc(knowledgeSourceId, pageable)
                .map(this::toDocumentResponse);
    }

    private KnowledgeSource findOrThrow(UUID id) {
        return knowledgeSourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeSource", id));
    }

    private KnowledgeSourceResponse toResponse(KnowledgeSource source) {
        return KnowledgeSourceResponse.builder()
                .id(source.getId())
                .name(source.getName())
                .description(source.getDescription())
                .type(source.getType())
                .baseUrl(source.getBaseUrl())
                .config(source.getConfig())
                .syncStatus(source.getSyncStatus())
                .lastSyncedAt(source.getLastSyncedAt())
                .documentCount(source.getDocumentCount())
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .build();
    }

    private DocumentResponse toDocumentResponse(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .knowledgeSourceId(doc.getKnowledgeSource().getId())
                .externalId(doc.getExternalId())
                .title(doc.getTitle())
                .content(doc.getContent() != null
                        ? doc.getContent().substring(0, Math.min(doc.getContent().length(), 500))
                        : null)
                .checksum(doc.getChecksum())
                .metadata(doc.getMetadata())
                .chunkCount(doc.getChunkCount())
                .syncedAt(doc.getSyncedAt())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
