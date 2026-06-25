package com.ragplatform.application.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ConversationResponse {
    private UUID id;
    private String title;
    private List<MessageResponse> messages;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
