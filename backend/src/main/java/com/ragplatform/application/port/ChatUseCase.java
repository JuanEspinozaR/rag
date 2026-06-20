package com.ragplatform.application.port;

import com.ragplatform.application.dto.request.ChatRequest;
import com.ragplatform.application.dto.response.ConversationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface ChatUseCase {

    Flux<String> streamChat(ChatRequest request);

    ConversationResponse findConversation(UUID id);

    Page<ConversationResponse> findAllConversations(Pageable pageable);

    void deleteConversation(UUID id);
}
