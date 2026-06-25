package com.ragplatform.application.service;

import com.ragplatform.application.dto.request.ChatRequest;
import com.ragplatform.application.dto.response.ConversationResponse;
import com.ragplatform.application.dto.response.MessageResponse;
import com.ragplatform.application.port.ChatUseCase;
import com.ragplatform.api.exception.ResourceNotFoundException;
import com.ragplatform.config.AppProperties;
import com.ragplatform.domain.enums.MessageRole;
import com.ragplatform.domain.model.Conversation;
import com.ragplatform.domain.model.Message;
import com.ragplatform.domain.repository.ConversationRepository;
import com.ragplatform.domain.repository.MessageRepository;
import com.ragplatform.infrastructure.langfuse.LangfuseTracingService;
import com.ragplatform.infrastructure.rag.RagPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// __CONV_ID__ prefix is parsed by the frontend to track the active conversation.
// It is emitted as the very first SSE token before any AI text.

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatService implements ChatUseCase {

    private final ChatClient chatClient;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final RagPipelineService ragPipelineService;
    private final LangfuseTracingService langfuseTracingService;
    private final AppProperties appProperties;

    @Override
    public Flux<String> streamChat(ChatRequest request) {
        Conversation conversation = resolveOrCreateConversation(request);
        UUID conversationId = conversation.getId();

        List<Message> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        List<String> retrievedChunks = ragPipelineService.retrieveRelevantChunks(
                request.getMessage(),
                appProperties.getRag().getTopK()
        );

        String systemPrompt = buildSystemPrompt(retrievedChunks);
        List<org.springframework.ai.chat.messages.Message> messages = buildMessageHistory(history, systemPrompt);
        messages.add(new UserMessage(request.getMessage()));

        String traceId = UUID.randomUUID().toString();
        langfuseTracingService.traceStartAsync(traceId, request.getMessage(), retrievedChunks, conversationId.toString());

        Message userMsg = Message.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(request.getMessage())
                .build();
        messageRepository.save(userMsg);

        StringBuilder responseBuilder = new StringBuilder();

        Flux<String> tokenStream = chatClient.prompt()
                .messages(messages)
                .stream()
                .content()
                .doOnNext(responseBuilder::append)
                .doOnComplete(() -> {
                    String fullResponse = responseBuilder.toString();
                    Message assistantMsg = Message.builder()
                            .conversation(conversation)
                            .role(MessageRole.ASSISTANT)
                            .content(fullResponse)
                            .model(appProperties.getRag().toString())
                            .build();
                    messageRepository.save(assistantMsg);

                    updateConversationTitle(conversation, request.getMessage());
                    langfuseTracingService.traceCompleteAsync(traceId, fullResponse);
                    log.debug("Chat completed for conversation: {}", conversationId);
                })
                .doOnError(e -> {
                    log.error("Error in chat streaming for conversation: {}", conversationId, e);
                    langfuseTracingService.traceErrorAsync(traceId, e.getMessage());
                });

        // Prepend conversation ID marker so the frontend can persist the ID
        // and continue the same conversation on follow-up messages.
        return Flux.concat(
                Flux.just("__CONV_ID__:" + conversationId),
                tokenStream
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse findConversation(UUID id) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", id));
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(id);
        return toResponse(conversation, messages);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> findAllConversations(Pageable pageable) {
        return conversationRepository.findAllByOrderByUpdatedAtDesc(pageable)
                .map(conv -> toResponse(conv, List.of()));
    }

    @Override
    public void deleteConversation(UUID id) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", id));
        conversationRepository.delete(conversation);
    }

    private Conversation resolveOrCreateConversation(ChatRequest request) {
        if (request.getConversationId() != null) {
            return conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation", request.getConversationId()));
        }
        return conversationRepository.save(Conversation.builder().build());
    }

    private String buildSystemPrompt(List<String> chunks) {
        if (chunks.isEmpty()) {
            return "You are a helpful AI assistant. Answer questions clearly and concisely.";
        }

        String context = chunks.stream()
                .limit(appProperties.getRag().getTopK())
                .collect(Collectors.joining("\n\n---\n\n"));

        return """
                You are a helpful AI assistant with access to a knowledge base.
                Use the following retrieved context to answer the user's question.
                If the answer is not in the context, say so clearly and provide your best general answer.
                Always cite the context when using it.
                
                CONTEXT:
                %s
                """.formatted(context);
    }

    private List<org.springframework.ai.chat.messages.Message> buildMessageHistory(
            List<Message> history, String systemPrompt) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        // Include last N messages for context window management
        int startIdx = Math.max(0, history.size() - 10);
        for (Message msg : history.subList(startIdx, history.size())) {
            if (msg.getRole() == MessageRole.USER) {
                messages.add(new UserMessage(msg.getContent()));
            } else if (msg.getRole() == MessageRole.ASSISTANT) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }
        return messages;
    }

    private void updateConversationTitle(Conversation conversation, String firstMessage) {
        if ("New Conversation".equals(conversation.getTitle()) && !firstMessage.isBlank()) {
            String title = firstMessage.length() > 60
                    ? firstMessage.substring(0, 57) + "..."
                    : firstMessage;
            conversation.setTitle(title);
            conversation.setUpdatedAt(OffsetDateTime.now());
            conversationRepository.save(conversation);
        }
    }

    private ConversationResponse toResponse(Conversation conv, List<Message> messages) {
        return ConversationResponse.builder()
                .id(conv.getId())
                .title(conv.getTitle())
                .messages(messages.stream().map(this::toMessageResponse).collect(Collectors.toList()))
                .createdAt(conv.getCreatedAt())
                .updatedAt(conv.getUpdatedAt())
                .build();
    }

    private MessageResponse toMessageResponse(Message msg) {
        return MessageResponse.builder()
                .id(msg.getId())
                .conversationId(msg.getConversation().getId())
                .role(msg.getRole())
                .content(msg.getContent())
                .model(msg.getModel())
                .promptTokens(msg.getPromptTokens())
                .completionTokens(msg.getCompletionTokens())
                .totalTokens(msg.getTotalTokens())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
