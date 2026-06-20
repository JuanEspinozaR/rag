// =============================================================================
// Core domain types
// =============================================================================

export type SourceType =
  | "WEBSITE_URL"
  | "SITEMAP"
  | "PDF_URL"
  | "MARKDOWN_URL"
  | "RAW_TEXT"
  | "GITHUB_REPO";

export type SyncStatus = "PENDING" | "SYNCING" | "COMPLETED" | "FAILED";

export type MessageRole = "USER" | "ASSISTANT" | "SYSTEM";

// =============================================================================
// Knowledge Source
// =============================================================================

export interface KnowledgeSource {
  id: string;
  name: string;
  description?: string;
  type: SourceType;
  baseUrl?: string;
  config?: Record<string, unknown>;
  syncStatus: SyncStatus;
  lastSyncedAt?: string;
  documentCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeSourceRequest {
  name: string;
  description?: string;
  type: SourceType;
  baseUrl?: string;
  config?: Record<string, unknown>;
}

// =============================================================================
// Document
// =============================================================================

export interface Document {
  id: string;
  knowledgeSourceId: string;
  externalId?: string;
  title?: string;
  content?: string;
  checksum?: string;
  metadata?: Record<string, unknown>;
  chunkCount: number;
  syncedAt?: string;
  createdAt: string;
}

// =============================================================================
// Conversation & Messages
// =============================================================================

export interface Conversation {
  id: string;
  title: string;
  messages: Message[];
  createdAt: string;
  updatedAt: string;
}

export interface Message {
  id: string;
  conversationId: string;
  role: MessageRole;
  content: string;
  model?: string;
  promptTokens?: number;
  completionTokens?: number;
  totalTokens?: number;
  createdAt: string;
}

export interface ChatRequest {
  conversationId?: string;
  message: string;
  knowledgeSourceId?: string;
}

// =============================================================================
// API Response wrappers
// =============================================================================

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  errorCode?: string;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
}

// =============================================================================
// UI State types
// =============================================================================

export interface StreamingMessage {
  role: "user" | "assistant";
  content: string;
  isStreaming?: boolean;
  createdAt?: string;
}
