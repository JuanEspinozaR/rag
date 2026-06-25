"use client";

import { useEffect } from "react";
import { useParams } from "next/navigation";
import { ChatWindow } from "@/components/chat/ChatWindow";
import { useChatStore } from "@/store/chat.store";
import { useConversation } from "@/hooks/useChat";

export default function ConversationPage() {
  const params = useParams();
  const id = params.id as string;

  const { data: conversation, isLoading } = useConversation(id);
  const { setMessages, setConversationId } = useChatStore();

  useEffect(() => {
    if (conversation) {
      setConversationId(conversation.id);
      setMessages(
        conversation.messages.map((msg) => ({
          role: msg.role.toLowerCase() as "user" | "assistant",
          content: msg.content,
          createdAt: msg.createdAt,
        }))
      );
    }
  }, [conversation, setConversationId, setMessages]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-sm text-muted-foreground animate-pulse">
          Loading conversation...
        </div>
      </div>
    );
  }

  return <ChatWindow conversationId={id} />;
}
