package com.ragplatform.api.controller;

import com.ragplatform.api.response.ApiResponse;
import com.ragplatform.api.response.PageResponse;
import com.ragplatform.application.dto.request.KnowledgeSourceRequest;
import com.ragplatform.application.dto.response.DocumentResponse;
import com.ragplatform.application.dto.response.KnowledgeSourceResponse;
import com.ragplatform.application.port.KnowledgeSourceUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/knowledge-sources")
@RequiredArgsConstructor
@Tag(name = "Knowledge Sources", description = "Manage RAG knowledge sources and document sync")
public class KnowledgeSourceController {

    private final KnowledgeSourceUseCase knowledgeSourceUseCase;

    @PostMapping
    @Operation(summary = "Create a new knowledge source")
    public ResponseEntity<ApiResponse<KnowledgeSourceResponse>> create(
            @Valid @RequestBody KnowledgeSourceRequest request) {
        log.info("Creating knowledge source: {}", request.getName());
        KnowledgeSourceResponse response = knowledgeSourceUseCase.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "List all knowledge sources")
    public ResponseEntity<ApiResponse<PageResponse<KnowledgeSourceResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        var result = knowledgeSourceUseCase.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a knowledge source by ID")
    public ResponseEntity<ApiResponse<KnowledgeSourceResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(knowledgeSourceUseCase.findById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a knowledge source")
    public ResponseEntity<ApiResponse<KnowledgeSourceResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody KnowledgeSourceRequest request) {
        log.info("Updating knowledge source: {}", id);
        return ResponseEntity.ok(ApiResponse.ok(knowledgeSourceUseCase.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a knowledge source")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        log.info("Deleting knowledge source: {}", id);
        knowledgeSourceUseCase.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Knowledge source deleted successfully"));
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "Trigger synchronization of a knowledge source")
    public ResponseEntity<ApiResponse<Void>> sync(@PathVariable UUID id) {
        log.info("Triggering sync for knowledge source: {}", id);
        knowledgeSourceUseCase.sync(id);
        return ResponseEntity.accepted()
                .body(ApiResponse.ok(null, "Sync started in background"));
    }

    @PostMapping("/sync-all")
    @Operation(summary = "Trigger synchronization for all PENDING or FAILED knowledge sources")
    public ResponseEntity<ApiResponse<Void>> syncAll() {
        log.info("Triggering sync-all");
        knowledgeSourceUseCase.syncAll();
        return ResponseEntity.accepted()
                .body(ApiResponse.ok(null, "Sync-all started in background"));
    }

    @GetMapping("/{id}/documents")
    @Operation(summary = "List documents for a knowledge source")
    public ResponseEntity<ApiResponse<PageResponse<DocumentResponse>>> findDocuments(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        var result = knowledgeSourceUseCase.findDocuments(id, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }
}
