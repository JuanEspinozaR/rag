"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { chatService } from "@/services/chat.service";
import { useChatStore } from "@/store/chat.store";
import { toast } from "sonner";
import { useCallback, useRef } from "react";
import type { ChatRequest } from "@/types";

export const CONVERSATIONS_KEY = ["conversations"] as const;

export function useConversations(page = 0, size = 20) {
  return useQuery({
    queryKey: [...CONVERSATIONS_KEY, page, size],
    queryFn: () => chatService.listConversations(page, size),
  });
}

export function useConversation(id: string | null) {
  return useQuery({
    queryKey: [...CONVERSATIONS_KEY, id],
    queryFn: () => chatService.getConversation(id!),
    enabled: !!id,
  });
}

export function useDeleteConversation() {
  const queryClient = useQueryClient();
  const { reset } = useChatStore();

  return useMutation({
    mutationFn: (id: string) => chatService.deleteConversation(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CONVERSATIONS_KEY });
      reset();
      toast.success("Conversation deleted");
    },
    onError: (err: Error) => {
      toast.error(err.message);
    },
  });
}

export function useStreamingChat() {
  const {
    conversationId,
    setConversationId,
    addMessage,
    appendStreamingToken,
    commitStreamedMessage,
    setIsStreaming,
    isStreaming,
  } = useChatStore();

  const queryClient = useQueryClient();
  const abortRef = useRef<AbortController | null>(null);

  const sendMessage = useCallback(
    (message: string) => {
      if (isStreaming || !message.trim()) return;

      // Add user message optimistically
      addMessage({
        role: "user",
        content: message,
        createdAt: new Date().toISOString(),
      });

      setIsStreaming(true);
      abortRef.current = new AbortController();

      const request: ChatRequest = {
        conversationId: conversationId || undefined,
        message,
      };

      chatService.streamChat(
        request,
        (token) => {
          appendStreamingToken(token);
        },
        () => {
          commitStreamedMessage();
          queryClient.invalidateQueries({ queryKey: CONVERSATIONS_KEY });
        },
        (err) => {
          setIsStreaming(false);
          toast.error(`Chat error: ${err.message}`);
        },
        abortRef.current.signal,
        (convId) => {
          // Persist the server-assigned conversation ID so follow-up messages
          // continue the same conversation instead of creating a new one.
          setConversationId(convId);
        }
      );
    },
    [
      isStreaming,
      conversationId,
      addMessage,
      setIsStreaming,
      appendStreamingToken,
      commitStreamedMessage,
      queryClient,
    ]
  );

  const stopStreaming = useCallback(() => {
    abortRef.current?.abort();
    commitStreamedMessage();
  }, [commitStreamedMessage]);

  return { sendMessage, stopStreaming, isStreaming };
}
