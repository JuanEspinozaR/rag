package com.ragplatform.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "Application health and info")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Application health check")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "rag-platform-backend",
                "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @GetMapping("/info")
    @Operation(summary = "Application info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
                "name", "RAG Platform",
                "version", "1.0.0",
                "description", "Self-hosted Retrieval-Augmented Generation platform"
        ));
    }
}
