import { create } from "zustand";
import type { StreamingMessage } from "@/types";

interface ChatState {
  conversationId: string | null;
  messages: StreamingMessage[];
  isStreaming: boolean;
  streamingContent: string;

  setConversationId: (id: string | null) => void;
  setMessages: (messages: StreamingMessage[]) => void;
  addMessage: (message: StreamingMessage) => void;
  appendStreamingToken: (token: string) => void;
  commitStreamedMessage: () => void;
  setIsStreaming: (streaming: boolean) => void;
  reset: () => void;
}

export const useChatStore = create<ChatState>((set, get) => ({
  conversationId: null,
  messages: [],
  isStreaming: false,
  streamingContent: "",

  setConversationId: (id) => set({ conversationId: id }),

  setMessages: (messages) => set({ messages }),

  addMessage: (message) =>
    set((state) => ({ messages: [...state.messages, message] })),

  appendStreamingToken: (token) =>
    set((state) => ({ streamingContent: state.streamingContent + token })),

  commitStreamedMessage: () => {
    const { streamingContent } = get();
    if (!streamingContent) return;
    set((state) => ({
      messages: [
        ...state.messages,
        {
          role: "assistant" as const,
          content: streamingContent,
          isStreaming: false,
          createdAt: new Date().toISOString(),
        },
      ],
      streamingContent: "",
      isStreaming: false,
    }));
  },

  setIsStreaming: (streaming) => set({ isStreaming: streaming }),

  reset: () =>
    set({
      conversationId: null,
      messages: [],
      isStreaming: false,
      streamingContent: "",
    }),
}));
