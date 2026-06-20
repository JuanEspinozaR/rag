"use client";

import { useEffect, useRef } from "react";
import { Bot, Loader2 } from "lucide-react";
import { MessageBubble } from "./MessageBubble";
import { PromptInput } from "./PromptInput";
import { ScrollArea } from "@/components/ui/scroll-area";
import { useChatStore } from "@/store/chat.store";
import { useStreamingChat } from "@/hooks/useChat";

interface ChatWindowProps {
  conversationId?: string;
}

export function ChatWindow({ conversationId }: ChatWindowProps) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const {
    messages,
    streamingContent,
    isStreaming,
    setConversationId,
    setMessages,
  } = useChatStore();

  const { sendMessage, stopStreaming } = useStreamingChat();

  useEffect(() => {
    setConversationId(conversationId ?? null);
    if (!conversationId) {
      setMessages([]);
    }
  }, [conversationId, setConversationId, setMessages]);

  // Auto-scroll to bottom
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, streamingContent]);

  const hasMessages = messages.length > 0;

  return (
    <div className="flex flex-col h-full">
      {/* Messages area */}
      <div
        ref={scrollRef}
        className="flex-1 overflow-y-auto"
      >
        {!hasMessages && !isStreaming ? (
          <EmptyState />
        ) : (
          <div className="p-6 space-y-6 max-w-3xl mx-auto">
            {messages.map((message, i) => {
              const isLastAssistant =
                i === messages.length - 1 && message.role === "assistant" && isStreaming;

              return (
                <MessageBubble
                  key={i}
                  message={message}
                  streamingContent={isLastAssistant ? streamingContent : undefined}
                />
              );
            })}

            {/* Streaming placeholder when no messages yet or last was user */}
            {isStreaming &&
              (messages.length === 0 ||
                messages[messages.length - 1]?.role === "user") && (
                <div className="flex gap-3 animate-fade-in">
                  <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border bg-muted">
                    {streamingContent ? (
                      <Bot className="h-4 w-4 text-muted-foreground" />
                    ) : (
                      <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
                    )}
                  </div>
                  <div className="flex flex-col gap-1 max-w-[80%] items-start">
                    <div className="rounded-2xl rounded-tl-sm px-4 py-3 text-sm bg-muted border">
                      {streamingContent ? (
                        <div className="prose-chat streaming-cursor">
                          {streamingContent}
                        </div>
                      ) : (
                        <div className="flex gap-1 items-center text-muted-foreground">
                          <span className="text-xs">Thinking</span>
                          <span className="animate-blink">...</span>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              )}
          </div>
        )}
      </div>

      {/* Input area */}
      <div className="border-t bg-background/95 backdrop-blur p-4">
        <div className="max-w-3xl mx-auto">
          <PromptInput
            onSend={sendMessage}
            onStop={stopStreaming}
            isStreaming={isStreaming}
          />
          <p className="text-[10px] text-center text-muted-foreground mt-2">
            Responses are grounded in your synced knowledge sources via RAG
          </p>
        </div>
      </div>
    </div>
  );
}

function EmptyState() {
  const suggestions = [
    "What topics are covered in my knowledge base?",
    "Summarize the main concepts from the documentation",
    "How do I get started with this platform?",
    "What are the key features available?",
  ];

  const { setMessages } = useChatStore();
  const { sendMessage } = useStreamingChat();

  const handleSuggestion = (text: string) => {
    sendMessage(text);
  };

  return (
    <div className="flex flex-col items-center justify-center h-full p-6 space-y-8">
      <div className="text-center space-y-3">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-primary/10 mx-auto">
          <Bot className="h-8 w-8 text-primary" />
        </div>
        <h2 className="text-xl font-semibold">How can I help you?</h2>
        <p className="text-sm text-muted-foreground max-w-sm">
          Ask questions about your synced knowledge sources. I&apos;ll retrieve
          relevant context and generate grounded answers.
        </p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 w-full max-w-xl">
        {suggestions.map((suggestion) => (
          <button
            key={suggestion}
            onClick={() => handleSuggestion(suggestion)}
            className="text-left text-sm rounded-xl border bg-card p-3 hover:border-primary/50 hover:bg-accent transition-all"
          >
            {suggestion}
          </button>
        ))}
      </div>
    </div>
  );
}
