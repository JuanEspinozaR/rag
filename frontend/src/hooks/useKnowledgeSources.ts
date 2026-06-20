"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { knowledgeService } from "@/services/knowledge.service";
import { useKnowledgeStore } from "@/store/knowledge.store";
import { toast } from "sonner";
import type { KnowledgeSourceRequest } from "@/types";

export const KNOWLEDGE_SOURCES_KEY = ["knowledge-sources"] as const;

export function useKnowledgeSources(page = 0, size = 20) {
  return useQuery({
    queryKey: [...KNOWLEDGE_SOURCES_KEY, page, size],
    queryFn: () => knowledgeService.list(page, size),
  });
}

export function useKnowledgeSource(id: string | null) {
  return useQuery({
    queryKey: [...KNOWLEDGE_SOURCES_KEY, id],
    queryFn: () => knowledgeService.getById(id!),
    enabled: !!id,
  });
}

export function useCreateKnowledgeSource() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: KnowledgeSourceRequest) => knowledgeService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: KNOWLEDGE_SOURCES_KEY });
      toast.success("Knowledge source created successfully");
    },
    onError: (err: Error) => {
      toast.error(err.message);
    },
  });
}

export function useUpdateKnowledgeSource() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: KnowledgeSourceRequest }) =>
      knowledgeService.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: KNOWLEDGE_SOURCES_KEY });
      toast.success("Knowledge source updated successfully");
    },
    onError: (err: Error) => {
      toast.error(err.message);
    },
  });
}

export function useDeleteKnowledgeSource() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => knowledgeService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: KNOWLEDGE_SOURCES_KEY });
      toast.success("Knowledge source deleted");
    },
    onError: (err: Error) => {
      toast.error(err.message);
    },
  });
}

export function useSyncKnowledgeSource() {
  const { setSyncing } = useKnowledgeStore();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => {
      setSyncing(id, true);
      return knowledgeService.sync(id);
    },
    onSuccess: (_, id) => {
      toast.success("Sync started — processing in background");
      // Poll for updates
      const interval = setInterval(async () => {
        await queryClient.invalidateQueries({ queryKey: KNOWLEDGE_SOURCES_KEY });
        const sources = queryClient.getQueryData<{ content: Array<{ id: string; syncStatus: string }> }>(
          [...KNOWLEDGE_SOURCES_KEY, 0, 20]
        );
        const source = sources?.content.find((s) => s.id === id);
        if (source && source.syncStatus !== "SYNCING") {
          setSyncing(id, false);
          clearInterval(interval);
          if (source.syncStatus === "COMPLETED") {
            toast.success("Sync completed!");
          } else if (source.syncStatus === "FAILED") {
            toast.error("Sync failed. Check the logs.");
          }
        }
      }, 3000);

      // Safety timeout after 5 minutes
      setTimeout(() => {
        clearInterval(interval);
        setSyncing(id, false);
      }, 300_000);
    },
    onError: (err: Error, id) => {
      setSyncing(id, false);
      toast.error(err.message);
    },
  });
}

export function useDocuments(sourceId: string | null, page = 0, size = 20) {
  return useQuery({
    queryKey: ["documents", sourceId, page, size],
    queryFn: () => knowledgeService.listDocuments(sourceId!, page, size),
    enabled: !!sourceId,
  });
}
