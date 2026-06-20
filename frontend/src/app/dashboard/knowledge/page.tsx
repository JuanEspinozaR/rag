"use client";

import { useState } from "react";
import { Plus, Database } from "lucide-react";
import { Button } from "@/components/ui/button";
import { SourceCatalogTable } from "@/components/knowledge/SourceCatalogTable";
import { SourceFormDialog } from "@/components/knowledge/SourceFormDialog";
import { DocumentViewerDrawer } from "@/components/knowledge/DocumentViewerDrawer";
import type { KnowledgeSource } from "@/types";

export default function KnowledgePage() {
  const [showForm, setShowForm] = useState(false);
  const [editingSource, setEditingSource] = useState<KnowledgeSource | null>(null);
  const [viewingSource, setViewingSource] = useState<KnowledgeSource | null>(null);

  const handleEdit = (source: KnowledgeSource) => {
    setEditingSource(source);
    setShowForm(true);
  };

  const handleFormClose = (open: boolean) => {
    setShowForm(open);
    if (!open) {
      setEditingSource(null);
    }
  };

  return (
    <div className="p-6 max-w-5xl mx-auto space-y-6">
      {/* Page header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10">
            <Database className="h-5 w-5 text-primary" />
          </div>
          <div>
            <h1 className="text-xl font-bold">Knowledge Sources</h1>
            <p className="text-sm text-muted-foreground">
              Manage your data sources and sync them into the vector store
            </p>
          </div>
        </div>
        <Button onClick={() => setShowForm(true)} className="gap-2">
          <Plus className="h-4 w-4" />
          Add Source
        </Button>
      </div>

      {/* Source catalog */}
      <SourceCatalogTable
        onEdit={handleEdit}
        onViewDocs={setViewingSource}
      />

      {/* Form dialog */}
      <SourceFormDialog
        open={showForm}
        onOpenChange={handleFormClose}
        editingSource={editingSource}
      />

      {/* Document viewer drawer */}
      <DocumentViewerDrawer
        source={viewingSource}
        onClose={() => setViewingSource(null)}
      />
    </div>
  );
}
