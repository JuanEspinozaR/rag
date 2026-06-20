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
import com.ragplatform.infrastructure.rag.DocumentSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class KnowledgeSourceService implements KnowledgeSourceUseCase {

    private final KnowledgeSourceRepository knowledgeSourceRepository;
    private final DocumentRepository documentRepository;
    private final DocumentSyncService documentSyncService;

    @Override
    public KnowledgeSourceResponse create(KnowledgeSourceRequest request) {
        KnowledgeSource source = KnowledgeSource.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .baseUrl(request.getBaseUrl())
                .config(request.getConfig())
                .build();
        return toResponse(knowledgeSourceRepository.save(source));
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
    @Async("syncExecutor")
    public void sync(UUID id) {
        KnowledgeSource source = findOrThrow(id);
        log.info("Starting sync for knowledge source: {} ({})", source.getName(), id);
        documentSyncService.syncKnowledgeSource(source);
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
