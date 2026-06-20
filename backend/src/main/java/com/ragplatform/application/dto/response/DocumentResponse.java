package com.ragplatform.application.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class DocumentResponse {
    private UUID id;
    private UUID knowledgeSourceId;
    private String externalId;
    private String title;
    private String content;
    private String checksum;
    private Map<String, Object> metadata;
    private Integer chunkCount;
    private OffsetDateTime syncedAt;
    private OffsetDateTime createdAt;
}
