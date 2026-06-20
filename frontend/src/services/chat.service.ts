import { get, getPage, del } from "@/lib/api";
import { streamChat as streamChatFetch } from "@/lib/api";
import type { Conversation, ChatRequest, PageResponse } from "@/types";

const BASE = "/chat";

export const chatService = {
  listConversations: (
    page = 0,
    size = 20
  ): Promise<PageResponse<Conversation>> =>
    getPage<Conversation>(`${BASE}/conversations`, page, size),

  getConversation: (id: string): Promise<Conversation> =>
    get<Conversation>(`${BASE}/conversations/${id}`),

  deleteConversation: (id: string): Promise<void> =>
    del(`${BASE}/conversations/${id}`),

  streamChat: (
    request: ChatRequest,
    onToken: (token: string) => void,
    onDone: () => void,
    onError: (err: Error) => void,
    signal?: AbortSignal
  ) => {
    streamChatFetch(request, onToken, onDone, onError, signal);
  },
};
