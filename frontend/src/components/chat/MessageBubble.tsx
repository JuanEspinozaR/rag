"use client";

import { Bot, User } from "lucide-react";
import { StreamingRenderer } from "./StreamingRenderer";
import { formatRelativeTime } from "@/lib/utils";
import type { StreamingMessage } from "@/types";

interface MessageBubbleProps {
  message: StreamingMessage;
  streamingContent?: string;
}

export function MessageBubble({ message, streamingContent }: MessageBubbleProps) {
  const isUser = message.role === "user";
  const isCurrentlyStreaming = message.isStreaming && streamingContent !== undefined;
  const displayContent = isCurrentlyStreaming ? streamingContent : message.content;

  return (
    <div className={`flex gap-3 ${isUser ? "flex-row-reverse" : "flex-row"} animate-fade-in`}>
      {/* Avatar */}
      <div
        className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full border ${
          isUser
            ? "bg-primary text-primary-foreground border-primary"
            : "bg-muted border-border"
        }`}
      >
        {isUser ? (
          <User className="h-4 w-4" />
        ) : (
          <Bot className="h-4 w-4 text-muted-foreground" />
        )}
      </div>

      {/* Content */}
      <div className={`flex flex-col gap-1 max-w-[80%] ${isUser ? "items-end" : "items-start"}`}>
        <div
          className={`rounded-2xl px-4 py-3 text-sm ${
            isUser
              ? "bg-primary text-primary-foreground rounded-tr-sm"
              : "bg-muted text-foreground rounded-tl-sm border"
          }`}
        >
          {isUser ? (
            <p className="whitespace-pre-wrap">{displayContent}</p>
          ) : (
            <StreamingRenderer
              content={displayContent || ""}
              isStreaming={isCurrentlyStreaming}
            />
          )}
        </div>
        {message.createdAt && (
          <span className="text-[10px] text-muted-foreground px-1">
            {formatRelativeTime(message.createdAt)}
          </span>
        )}
      </div>
    </div>
  );
}
