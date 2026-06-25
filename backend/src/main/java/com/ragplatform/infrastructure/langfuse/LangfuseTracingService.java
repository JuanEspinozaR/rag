package com.ragplatform.infrastructure.langfuse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LangfuseTracingService {

    private final LangfuseClient langfuseClient;

    @Async
    public void traceStartAsync(String traceId, String userMessage, List<String> retrievedChunks, String conversationId) {
        try {
            Map<String, Object> traceBody = new HashMap<>();
            traceBody.put("id", traceId);
            traceBody.put("name", "rag-chat");
            traceBody.put("input", userMessage);
            traceBody.put("metadata", Map.of(
                    "conversationId", conversationId,
                    "retrievedChunkCount", retrievedChunks.size()
            ));
            traceBody.put("tags", List.of("rag", "chat"));
            traceBody.put("timestamp", Instant.now().toString());

            Map<String, Object> spanBody = new HashMap<>();
            spanBody.put("id", traceId + "-retrieval");
            spanBody.put("traceId", traceId);
            spanBody.put("name", "vector-retrieval");
            spanBody.put("startTime", Instant.now().toString());
            spanBody.put("input", Map.of("query", userMessage));
            spanBody.put("output", Map.of("chunks", retrievedChunks));
            spanBody.put("metadata", Map.of("chunkCount", retrievedChunks.size()));

            langfuseClient.ingest(List.of(
                    Map.of("type", "trace-create", "body", traceBody),
                    Map.of("type", "span-create", "body", spanBody)
            ));
        } catch (Exception e) {
            log.warn("Failed to trace start: {}", e.getMessage());
        }
    }

    @Async
    public void traceCompleteAsync(String traceId, String response) {
        try {
            Map<String, Object> generationBody = new HashMap<>();
            generationBody.put("id", traceId + "-generation");
            generationBody.put("traceId", traceId);
            generationBody.put("name", "openai-chat");
            generationBody.put("output", response);
            generationBody.put("completionStartTime", Instant.now().toString());
            generationBody.put("endTime", Instant.now().toString());

            langfuseClient.ingest(List.of(
                    Map.of("type", "generation-create", "body", generationBody)
            ));
        } catch (Exception e) {
            log.warn("Failed to trace completion: {}", e.getMessage());
        }
    }

    @Async
    public void traceErrorAsync(String traceId, String errorMessage) {
        try {
            Map<String, Object> traceBody = new HashMap<>();
            traceBody.put("id", traceId);
            traceBody.put("level", "ERROR");
            traceBody.put("statusMessage", errorMessage);

            langfuseClient.ingest(List.of(
                    Map.of("type", "trace-create", "body", traceBody)
            ));
        } catch (Exception e) {
            log.warn("Failed to trace error: {}", e.getMessage());
        }
    }
}
