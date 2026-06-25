import { Badge } from "@/components/ui/badge";
import { Loader2, CheckCircle2, XCircle, Clock } from "lucide-react";
import type { SyncStatus } from "@/types";

const STATUS_CONFIG: Record<
  SyncStatus,
  {
    label: string;
    variant: "default" | "secondary" | "destructive" | "outline" | "success" | "warning" | "info";
    Icon: React.ComponentType<{ className?: string }>;
  }
> = {
  PENDING: { label: "Pending", variant: "warning", Icon: Clock },
  SYNCING: { label: "Syncing", variant: "info", Icon: Loader2 },
  COMPLETED: { label: "Synced", variant: "success", Icon: CheckCircle2 },
  FAILED: { label: "Failed", variant: "destructive", Icon: XCircle },
};

interface SourceStatusBadgeProps {
  status: SyncStatus;
}

export function SourceStatusBadge({ status }: SourceStatusBadgeProps) {
  const config = STATUS_CONFIG[status] || STATUS_CONFIG.PENDING;
  const { label, variant, Icon } = config;

  return (
    <Badge variant={variant} className="gap-1.5">
      <Icon
        className={`h-3 w-3 ${status === "SYNCING" ? "animate-spin" : ""}`}
      />
      {label}
    </Badge>
  );
}
