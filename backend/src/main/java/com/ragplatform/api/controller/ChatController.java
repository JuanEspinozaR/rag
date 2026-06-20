package com.ragplatform.api.controller;

import com.ragplatform.api.response.ApiResponse;
import com.ragplatform.api.response.PageResponse;
import com.ragplatform.application.dto.request.ChatRequest;
import com.ragplatform.application.dto.response.ConversationResponse;
import com.ragplatform.application.port.ChatUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "RAG-powered streaming chat")
public class ChatController {

    private final ChatUseCase chatUseCase;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream a chat response using RAG")
    public Flux<String> streamChat(@Valid @RequestBody ChatRequest request) {
        log.info("Chat request: conversationId={}, message length={}",
                request.getConversationId(), request.getMessage().length());
        return chatUseCase.streamChat(request);
    }

    @GetMapping("/conversations")
    @Operation(summary = "List all conversations")
    public ResponseEntity<ApiResponse<PageResponse<ConversationResponse>>> listConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        var result = chatUseCase.findAllConversations(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @GetMapping("/conversations/{id}")
    @Operation(summary = "Get a conversation with its messages")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(chatUseCase.findConversation(id)));
    }

    @DeleteMapping("/conversations/{id}")
    @Operation(summary = "Delete a conversation")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(@PathVariable UUID id) {
        log.info("Deleting conversation: {}", id);
        chatUseCase.deleteConversation(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Conversation deleted successfully"));
    }
}
