import { create } from "zustand";
import type { KnowledgeSource } from "@/types";

interface KnowledgeState {
  selectedSource: KnowledgeSource | null;
  syncingIds: Set<string>;
  setSelectedSource: (source: KnowledgeSource | null) => void;
  setSyncing: (id: string, syncing: boolean) => void;
  isSyncing: (id: string) => boolean;
}

export const useKnowledgeStore = create<KnowledgeState>((set, get) => ({
  selectedSource: null,
  syncingIds: new Set(),

  setSelectedSource: (source) => set({ selectedSource: source }),

  setSyncing: (id, syncing) => {
    const next = new Set(get().syncingIds);
    if (syncing) {
      next.add(id);
    } else {
      next.delete(id);
    }
    set({ syncingIds: next });
  },

  isSyncing: (id) => get().syncingIds.has(id),
}));
