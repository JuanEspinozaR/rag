package com.ragplatform.infrastructure.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Core RAG retrieval pipeline.
 * Embeds the query and performs similarity search in pgvector.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagPipelineService {

    private final VectorStore vectorStore;

    public List<String> retrieveRelevantChunks(String query, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(0.5)
                    .build();

            List<org.springframework.ai.document.Document> results = vectorStore.similaritySearch(searchRequest);

            log.debug("Retrieved {} chunks for query: {}", results.size(), query.substring(0, Math.min(query.length(), 50)));

            return results.stream()
                    .map(doc -> {
                        String title = (String) doc.getMetadata().getOrDefault("title", "");
                        String content = doc.getText();
                        return title.isBlank() ? content : "## " + title + "\n" + content;
                    })
                    .toList();

        } catch (Exception e) {
            log.error("Vector search failed: {}", e.getMessage(), e);
            return List.of();
        }
    }
}
