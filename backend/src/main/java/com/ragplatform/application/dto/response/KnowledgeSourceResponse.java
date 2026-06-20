package com.ragplatform.application.dto.response;

import com.ragplatform.domain.enums.SourceType;
import com.ragplatform.domain.enums.SyncStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class KnowledgeSourceResponse {
    private UUID id;
    private String name;
    private String description;
    private SourceType type;
    private String baseUrl;
    private Map<String, Object> config;
    private SyncStatus syncStatus;
    private OffsetDateTime lastSyncedAt;
    private Integer documentCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
