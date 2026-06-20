"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Plus, Trash2, MessageSquare, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Skeleton } from "@/components/ui/skeleton";
import { useConversations, useDeleteConversation } from "@/hooks/useChat";
import { useChatStore } from "@/store/chat.store";
import { formatRelativeTime, truncate, cn } from "@/lib/utils";

export function ChatSidebar() {
  const pathname = usePathname();
  const { data, isLoading } = useConversations();
  const deleteMutation = useDeleteConversation();
  const { reset } = useChatStore();

  const conversations = data?.content ?? [];

  return (
    <div className="flex h-full w-64 flex-col border-r bg-sidebar">
      {/* Header */}
      <div className="flex items-center justify-between px-3 py-3 border-b border-sidebar-border">
        <span className="text-xs font-semibold text-sidebar-foreground/60 uppercase tracking-wider">
          Conversations
        </span>
        <Button
          size="sm"
          variant="ghost"
          className="h-7 w-7 p-0"
          onClick={reset}
          title="New conversation"
          asChild
        >
          <Link href="/dashboard/chat">
            <Plus className="h-4 w-4" />
          </Link>
        </Button>
      </div>

      {/* Conversation list */}
      <ScrollArea className="flex-1">
        <div className="p-2 space-y-1">
          {isLoading ? (
            Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="flex items-center gap-2 rounded-lg p-2">
                <Skeleton className="h-4 w-4 rounded" />
                <Skeleton className="h-4 flex-1" />
              </div>
            ))
          ) : conversations.length === 0 ? (
            <div className="py-8 text-center">
              <MessageSquare className="h-8 w-8 mx-auto text-sidebar-foreground/30 mb-2" />
              <p className="text-xs text-sidebar-foreground/50">
                No conversations yet
              </p>
            </div>
          ) : (
            conversations.map((conv) => {
              const isActive =
                pathname === `/dashboard/chat/${conv.id}`;
              return (
                <div key={conv.id} className="group relative flex items-center">
                  <Link
                    href={`/dashboard/chat/${conv.id}`}
                    className={cn(
                      "flex flex-1 items-center gap-2 rounded-lg px-3 py-2 text-xs transition-colors min-w-0",
                      isActive
                        ? "bg-sidebar-primary text-sidebar-primary-foreground"
                        : "text-sidebar-foreground/70 hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
                    )}
                  >
                    <MessageSquare className="h-3.5 w-3.5 shrink-0" />
                    <div className="flex-1 min-w-0">
                      <p className="truncate font-medium">
                        {truncate(conv.title, 28)}
                      </p>
                      <p className="text-[10px] opacity-60">
                        {formatRelativeTime(conv.updatedAt)}
                      </p>
                    </div>
                  </Link>

                  <button
                    onClick={(e) => {
                      e.preventDefault();
                      deleteMutation.mutate(conv.id);
                    }}
                    disabled={deleteMutation.isPending}
                    className="absolute right-2 opacity-0 group-hover:opacity-100 p-1 rounded hover:bg-destructive/20 hover:text-destructive transition-all"
                    title="Delete conversation"
                  >
                    {deleteMutation.isPending && deleteMutation.variables === conv.id ? (
                      <Loader2 className="h-3 w-3 animate-spin" />
                    ) : (
                      <Trash2 className="h-3 w-3" />
                    )}
                  </button>
                </div>
              );
            })
          )}
        </div>
      </ScrollArea>
    </div>
  );
}
