import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatDate(dateString: string | undefined): string {
  if (!dateString) return "Never";
  const date = new Date(dateString);
  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

export function formatRelativeTime(dateString: string | undefined): string {
  if (!dateString) return "Never";
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffSecs = Math.floor(diffMs / 1000);
  const diffMins = Math.floor(diffSecs / 60);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffSecs < 60) return "just now";
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  if (diffDays < 7) return `${diffDays}d ago`;
  return formatDate(dateString);
}

export function truncate(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text;
  return text.slice(0, maxLength) + "...";
}

export const SOURCE_TYPE_LABELS: Record<string, string> = {
  WEBSITE_URL: "Website URL",
  SITEMAP: "Sitemap",
  PDF_URL: "PDF",
  MARKDOWN_URL: "Markdown",
  RAW_TEXT: "Raw Text",
  GITHUB_REPO: "GitHub Repo",
};

export const SYNC_STATUS_COLORS: Record<string, string> = {
  PENDING: "text-yellow-600 bg-yellow-50 dark:bg-yellow-950/30 border-yellow-200",
  SYNCING: "text-blue-600 bg-blue-50 dark:bg-blue-950/30 border-blue-200",
  COMPLETED: "text-green-600 bg-green-50 dark:bg-green-950/30 border-green-200",
  FAILED: "text-red-600 bg-red-50 dark:bg-red-950/30 border-red-200",
};
