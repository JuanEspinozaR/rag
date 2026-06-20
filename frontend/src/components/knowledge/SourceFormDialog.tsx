"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  useCreateKnowledgeSource,
  useUpdateKnowledgeSource,
} from "@/hooks/useKnowledgeSources";
import { SOURCE_TYPE_LABELS } from "@/lib/utils";
import type { KnowledgeSource } from "@/types";

const sourceTypes = Object.entries(SOURCE_TYPE_LABELS);

const formSchema = z.object({
  name: z.string().min(1, "Name is required").max(255),
  description: z.string().max(1000).optional(),
  type: z.enum([
    "WEBSITE_URL",
    "SITEMAP",
    "PDF_URL",
    "MARKDOWN_URL",
    "RAW_TEXT",
    "GITHUB_REPO",
  ]),
  baseUrl: z.string().optional(),
});

type FormValues = z.infer<typeof formSchema>;

interface SourceFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  editingSource?: KnowledgeSource | null;
}

export function SourceFormDialog({
  open,
  onOpenChange,
  editingSource,
}: SourceFormDialogProps) {
  const createMutation = useCreateKnowledgeSource();
  const updateMutation = useUpdateKnowledgeSource();
  const isEditing = !!editingSource;

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: { type: "WEBSITE_URL" },
  });

  const selectedType = watch("type");

  useEffect(() => {
    if (editingSource) {
      reset({
        name: editingSource.name,
        description: editingSource.description,
        type: editingSource.type,
        baseUrl: editingSource.baseUrl,
      });
    } else {
      reset({ type: "WEBSITE_URL" });
    }
  }, [editingSource, reset]);

  const onSubmit = async (values: FormValues) => {
    const payload = {
      name: values.name,
      description: values.description,
      type: values.type,
      baseUrl: values.type !== "RAW_TEXT" ? values.baseUrl : undefined,
    };

    if (isEditing) {
      await updateMutation.mutateAsync({ id: editingSource.id, data: payload });
    } else {
      await createMutation.mutateAsync(payload);
    }
    onOpenChange(false);
    reset();
  };

  const isLoading = createMutation.isPending || updateMutation.isPending;
  const needsUrl = selectedType !== "RAW_TEXT";

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>
            {isEditing ? "Edit Knowledge Source" : "Add Knowledge Source"}
          </DialogTitle>
          <DialogDescription>
            Configure a data source to sync and embed into your knowledge base.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="name">Name *</Label>
            <Input
              id="name"
              placeholder="My Documentation"
              {...register("name")}
            />
            {errors.name && (
              <p className="text-xs text-destructive">{errors.name.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">Description</Label>
            <Input
              id="description"
              placeholder="Optional description"
              {...register("description")}
            />
          </div>

          <div className="space-y-2">
            <Label>Source Type *</Label>
            <Select
              value={selectedType}
              onValueChange={(v) => setValue("type", v as FormValues["type"])}
            >
              <SelectTrigger>
                <SelectValue placeholder="Select type" />
              </SelectTrigger>
              <SelectContent>
                {sourceTypes.map(([value, label]) => (
                  <SelectItem key={value} value={value}>
                    {label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {needsUrl && (
            <div className="space-y-2">
              <Label htmlFor="baseUrl">
                {selectedType === "RAW_TEXT" ? "Content" : "URL *"}
              </Label>
              <Input
                id="baseUrl"
                placeholder={
                  selectedType === "SITEMAP"
                    ? "https://example.com/sitemap.xml"
                    : selectedType === "PDF_URL"
                    ? "https://example.com/document.pdf"
                    : "https://example.com/docs"
                }
                {...register("baseUrl")}
              />
            </div>
          )}

          {selectedType === "RAW_TEXT" && (
            <div className="space-y-2">
              <Label htmlFor="baseUrl">Content</Label>
              <Textarea
                id="baseUrl"
                rows={6}
                placeholder="Paste your text content here..."
                {...register("baseUrl")}
                className="resize-none"
              />
            </div>
          )}

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isLoading}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading
                ? "Saving..."
                : isEditing
                ? "Update"
                : "Create Source"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
