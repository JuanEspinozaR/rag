package com.ragplatform.infrastructure.rag;

import com.ragplatform.config.AppProperties;
import com.ragplatform.domain.enums.SyncStatus;
import com.ragplatform.domain.model.Document;
import com.ragplatform.domain.model.DocumentChunk;
import com.ragplatform.domain.model.KnowledgeSource;
import com.ragplatform.domain.repository.DocumentChunkRepository;
import com.ragplatform.domain.repository.DocumentRepository;
import com.ragplatform.domain.repository.KnowledgeSourceRepository;
import com.ragplatform.infrastructure.connector.ConnectorFactory;
import com.ragplatform.infrastructure.connector.DataSourceConnector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentSyncService {

    private final ConnectorFactory connectorFactory;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final KnowledgeSourceRepository knowledgeSourceRepository;
    private final VectorStore vectorStore;
    private final AppProperties appProperties;

    @Transactional
    public void syncKnowledgeSource(KnowledgeSource source) {
        knowledgeSourceRepository.updateSyncStatus(source.getId(), SyncStatus.SYNCING);

        try {
            DataSourceConnector connector = connectorFactory.getConnector(source.getType());
            List<DataSourceConnector.FetchedDocument> fetchedDocs = connector.fetch(
                    source.getBaseUrl(), source.getConfig());

            log.info("Fetched {} documents from source: {}", fetchedDocs.size(), source.getName());

            int processed = 0;
            for (DataSourceConnector.FetchedDocument fetchedDoc : fetchedDocs) {
                processDocument(source, fetchedDoc);
                processed++;
            }

            knowledgeSourceRepository.updateSyncStatus(source.getId(), SyncStatus.COMPLETED);
            knowledgeSourceRepository.updateDocumentCount(source.getId(), processed);

            // Update lastSyncedAt
            KnowledgeSource updatedSource = knowledgeSourceRepository.findById(source.getId()).orElseThrow();
            updatedSource.setLastSyncedAt(OffsetDateTime.now());
            knowledgeSourceRepository.save(updatedSource);

            log.info("Sync completed for source: {} - {} documents processed", source.getName(), processed);

        } catch (Exception e) {
            log.error("Sync failed for source: {}", source.getName(), e);
            knowledgeSourceRepository.updateSyncStatus(source.getId(), SyncStatus.FAILED);
            throw e;
        }
    }

    private void processDocument(KnowledgeSource source, DataSourceConnector.FetchedDocument fetchedDoc) {
        String checksum = computeChecksum(fetchedDoc.content());

        // Skip if document content hasn't changed
        var existing = documentRepository.findByKnowledgeSourceIdAndChecksum(source.getId(), checksum);
        if (existing.isPresent()) {
            log.debug("Skipping unchanged document: {}", fetchedDoc.externalId());
            return;
        }

        // Delete old version if it exists
        documentRepository.findByKnowledgeSourceIdAndExternalId(source.getId(), fetchedDoc.externalId())
                .ifPresent(old -> {
                    documentChunkRepository.deleteByDocumentId(old.getId());
                    documentRepository.delete(old);
                });

        // Save new document
        Document document = Document.builder()
                .knowledgeSource(source)
                .externalId(fetchedDoc.externalId())
                .title(fetchedDoc.title())
                .content(fetchedDoc.content())
                .checksum(checksum)
                .metadata(fetchedDoc.metadata())
                .syncedAt(OffsetDateTime.now())
                .build();
        document = documentRepository.save(document);

        // Chunk and embed
        List<String> chunks = chunkText(fetchedDoc.content());
        List<org.springframework.ai.document.Document> vectorDocs = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunkContent = chunks.get(i);

            DocumentChunk chunk = DocumentChunk.builder()
                    .document(document)
                    .chunkIndex(i)
                    .content(chunkContent)
                    .tokenCount(estimateTokenCount(chunkContent))
                    .build();
            documentChunkRepository.save(chunk);

            // Add to vector store
            org.springframework.ai.document.Document vectorDoc =
                    new org.springframework.ai.document.Document(chunkContent);
            vectorDoc.getMetadata().put("knowledge_source_id", source.getId().toString());
            vectorDoc.getMetadata().put("document_id", document.getId().toString());
            vectorDoc.getMetadata().put("chunk_index", String.valueOf(i));
            vectorDoc.getMetadata().put("title", fetchedDoc.title());
            vectorDoc.getMetadata().put("source_url", fetchedDoc.externalId());
            vectorDocs.add(vectorDoc);
        }

        vectorStore.add(vectorDocs);

        document.setChunkCount(chunks.size());
        documentRepository.save(document);

        log.debug("Processed document: {} ({} chunks)", fetchedDoc.title(), chunks.size());
    }

    public void deleteVectorsByKnowledgeSource(UUID knowledgeSourceId) {
        try {
            vectorStore.delete(List.of(knowledgeSourceId.toString()));
        } catch (Exception e) {
            log.warn("Could not delete vectors for knowledge source: {}", knowledgeSourceId, e);
        }
    }

    private List<String> chunkText(String text) {
        if (text == null || text.isBlank()) return List.of();

        int chunkSize = appProperties.getChunking().getChunkSize();
        int overlap = appProperties.getChunking().getChunkOverlap();

        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");

        int i = 0;
        while (i < words.length) {
            int end = Math.min(i + chunkSize, words.length);
            String chunk = String.join(" ", java.util.Arrays.copyOfRange(words, i, end));
            chunks.add(chunk.trim());
            i += (chunkSize - overlap);
        }

        return chunks;
    }

    private int estimateTokenCount(String text) {
        // Rough estimation: ~4 characters per token
        return text.length() / 4;
    }

    private String computeChecksum(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes());
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (Exception e) {
            return String.valueOf(content.hashCode());
        }
    }
}
