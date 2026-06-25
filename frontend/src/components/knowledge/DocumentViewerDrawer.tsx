"use client";

import { useState } from "react";
import { X, FileText, ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Skeleton } from "@/components/ui/skeleton";
import { useDocuments } from "@/hooks/useKnowledgeSources";
import { formatRelativeTime, truncate } from "@/lib/utils";
import type { KnowledgeSource } from "@/types";

interface DocumentViewerDrawerProps {
  source: KnowledgeSource | null;
  onClose: () => void;
}

export function DocumentViewerDrawer({ source, onClose }: DocumentViewerDrawerProps) {
  const [page, setPage] = useState(0);
  const { data, isLoading } = useDocuments(source?.id ?? null, page, 10);

  if (!source) return null;

  return (
    <div className="fixed inset-0 z-50 flex">
      {/* Backdrop */}
      <div
        className="flex-1 bg-black/20 backdrop-blur-sm"
        onClick={onClose}
      />

      {/* Drawer */}
      <div className="w-full max-w-xl bg-background border-l shadow-2xl flex flex-col animate-slide-in">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b">
          <div>
            <h2 className="font-semibold">{source.name}</h2>
            <p className="text-xs text-muted-foreground mt-0.5">
              {data?.totalElements ?? 0} documents
            </p>
          </div>
          <Button variant="ghost" size="icon" onClick={onClose}>
            <X className="h-4 w-4" />
          </Button>
        </div>

        {/* Document list */}
        <ScrollArea className="flex-1">
          <div className="p-4 space-y-3">
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <div key={i} className="space-y-2 rounded-lg border p-4">
                  <Skeleton className="h-4 w-3/4" />
                  <Skeleton className="h-3 w-full" />
                  <Skeleton className="h-3 w-2/3" />
                </div>
              ))
            ) : data?.content.length === 0 ? (
              <div className="text-center py-12 space-y-2">
                <FileText className="h-10 w-10 mx-auto text-muted-foreground/40" />
                <p className="text-sm text-muted-foreground">
                  No documents yet. Sync this source to fetch content.
                </p>
              </div>
            ) : (
              data?.content.map((doc) => (
                <div
                  key={doc.id}
                  className="rounded-lg border bg-card p-4 space-y-2 hover:border-primary/30 transition-colors"
                >
                  <h3 className="font-medium text-sm">
                    {doc.title ?? "Untitled Document"}
                  </h3>
                  {doc.content && (
                    <p className="text-xs text-muted-foreground leading-relaxed">
                      {truncate(doc.content, 200)}
                    </p>
                  )}
                  <div className="flex items-center gap-3 text-xs text-muted-foreground">
                    <span>{doc.chunkCount} chunks</span>
                    {doc.syncedAt && (
                      <span>Synced {formatRelativeTime(doc.syncedAt)}</span>
                    )}
                    {doc.externalId && (
                      <a
                        href={doc.externalId}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="hover:text-primary transition-colors"
                      >
                        Source ↗
                      </a>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        </ScrollArea>

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between px-6 py-3 border-t">
            <Button
              variant="outline"
              size="sm"
              disabled={data.first}
              onClick={() => setPage((p) => p - 1)}
            >
              <ChevronLeft className="h-4 w-4" />
              Prev
            </Button>
            <span className="text-xs text-muted-foreground">
              Page {page + 1} of {data.totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={data.last}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
