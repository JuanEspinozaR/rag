"use client";

import { useState } from "react";
import { MoreHorizontal, Pencil, Trash2, RefreshCw, FileText, Globe } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { SourceStatusBadge } from "./SourceStatusBadge";
import {
  useKnowledgeSources,
  useDeleteKnowledgeSource,
  useSyncKnowledgeSource,
} from "@/hooks/useKnowledgeSources";
import { useKnowledgeStore } from "@/store/knowledge.store";
import { formatRelativeTime, SOURCE_TYPE_LABELS, truncate } from "@/lib/utils";
import type { KnowledgeSource } from "@/types";

interface SourceCatalogTableProps {
  onEdit: (source: KnowledgeSource) => void;
  onViewDocs: (source: KnowledgeSource) => void;
}

export function SourceCatalogTable({ onEdit, onViewDocs }: SourceCatalogTableProps) {
  const { data, isLoading, isError } = useKnowledgeSources();
  const deleteMutation = useDeleteKnowledgeSource();
  const syncMutation = useSyncKnowledgeSource();
  const { isSyncing } = useKnowledgeStore();
  const [confirmDelete, setConfirmDelete] = useState<string | null>(null);

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="rounded-xl border p-4 space-y-2">
            <Skeleton className="h-5 w-48" />
            <Skeleton className="h-4 w-72" />
            <div className="flex gap-2">
              <Skeleton className="h-6 w-20" />
              <Skeleton className="h-6 w-16" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (isError) {
    return (
      <div className="rounded-xl border bg-destructive/10 p-8 text-center text-sm text-destructive">
        Failed to load knowledge sources. Check that the backend is running.
      </div>
    );
  }

  const sources = data?.content ?? [];

  if (sources.length === 0) {
    return (
      <div className="rounded-xl border bg-card p-12 text-center space-y-3">
        <Globe className="h-12 w-12 mx-auto text-muted-foreground/40" />
        <h3 className="font-semibold text-lg">No knowledge sources yet</h3>
        <p className="text-muted-foreground text-sm max-w-sm mx-auto">
          Add your first knowledge source to start building your knowledge base.
          Supports websites, PDFs, sitemaps, and more.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {sources.map((source) => {
        const syncing = isSyncing(source.id) || source.syncStatus === "SYNCING";
        const isDeleting = deleteMutation.isPending && deleteMutation.variables === source.id;

        return (
          <div
            key={source.id}
            className="group rounded-xl border bg-card p-4 shadow-sm hover:shadow-md transition-all"
          >
            <div className="flex items-start justify-between gap-4">
              <div className="flex-1 min-w-0 space-y-2">
                {/* Title row */}
                <div className="flex items-center gap-2 flex-wrap">
                  <h3 className="font-semibold text-sm truncate">{source.name}</h3>
                  <SourceStatusBadge status={source.syncStatus} />
                  <span className="text-xs text-muted-foreground border rounded-full px-2 py-0.5">
                    {SOURCE_TYPE_LABELS[source.type] ?? source.type}
                  </span>
                </div>

                {/* URL */}
                {source.baseUrl && (
                  <p className="text-xs text-muted-foreground truncate">
                    {truncate(source.baseUrl, 80)}
                  </p>
                )}

                {/* Description */}
                {source.description && (
                  <p className="text-xs text-muted-foreground">
                    {source.description}
                  </p>
                )}

                {/* Stats */}
                <div className="flex items-center gap-4 text-xs text-muted-foreground">
                  <span>{source.documentCount} docs</span>
                  {source.lastSyncedAt && (
                    <span>Synced {formatRelativeTime(source.lastSyncedAt)}</span>
                  )}
                  <span>Added {formatRelativeTime(source.createdAt)}</span>
                </div>
              </div>

              {/* Actions */}
              <div className="flex items-center gap-1.5 opacity-0 group-hover:opacity-100 transition-opacity">
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => onViewDocs(source)}
                  title="View documents"
                >
                  <FileText className="h-3.5 w-3.5" />
                </Button>

                <SyncButton
                  sourceId={source.id}
                  isSyncing={syncing}
                  onSync={() => syncMutation.mutate(source.id)}
                />

                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => onEdit(source)}
                  title="Edit"
                >
                  <Pencil className="h-3.5 w-3.5" />
                </Button>

                {confirmDelete === source.id ? (
                  <div className="flex gap-1">
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => {
                        deleteMutation.mutate(source.id);
                        setConfirmDelete(null);
                      }}
                      disabled={isDeleting}
                    >
                      Confirm
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => setConfirmDelete(null)}
                    >
                      Cancel
                    </Button>
                  </div>
                ) : (
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => setConfirmDelete(source.id)}
                    title="Delete"
                  >
                    <Trash2 className="h-3.5 w-3.5 text-destructive" />
                  </Button>
                )}
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}

function SyncButton({
  sourceId,
  isSyncing,
  onSync,
}: {
  sourceId: string;
  isSyncing: boolean;
  onSync: () => void;
}) {
  return (
    <Button
      size="sm"
      variant="outline"
      onClick={onSync}
      disabled={isSyncing}
      title={isSyncing ? "Syncing..." : "Sync now"}
    >
      <RefreshCw
        className={`h-3.5 w-3.5 ${isSyncing ? "animate-spin" : ""}`}
      />
    </Button>
  );
}
