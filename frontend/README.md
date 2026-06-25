# RAG Platform Frontend

Next.js 14 (App Router) frontend for the RAG Platform.

## Pages

| Route | Description |
|---|---|
| `/` | Redirects to `/dashboard` |
| `/dashboard` | Overview, quick actions, getting started |
| `/dashboard/knowledge` | Knowledge source management CRUD |
| `/dashboard/chat` | New chat interface |
| `/dashboard/chat/[id]` | Specific conversation with history |

## Components

### Layout
- `Sidebar` — Collapsible sidebar navigation with active state
- `Header` — Page title + theme toggle
- `ThemeProvider` — next-themes dark/light mode
- `QueryProvider` — TanStack Query client provider

### Knowledge Components
- `SourceCatalogTable` — Table listing all sources with status, actions
- `SourceFormDialog` — Create/edit modal with source type selection
- `SourceStatusBadge` — Color-coded sync status pill
- `DocumentViewerDrawer` — Side drawer listing fetched documents with pagination

### Chat Components
- `ChatSidebar` — Conversation history list with delete action
- `ChatWindow` — Main chat area with message rendering and input
- `MessageBubble` — Individual message with user/assistant styling
- `PromptInput` — Auto-resizing textarea with send/stop buttons
- `StreamingRenderer` — Markdown renderer with syntax highlighting

### UI Primitives (shadcn-style)
- `Button`, `Input`, `Textarea`, `Label`
- `Badge` (with custom variants: success, warning, info)
- `Dialog`, `Select`, `ScrollArea`, `Separator`, `Skeleton`

## Stores

### `useKnowledgeStore` (Zustand)
- Tracks which sources are currently syncing
- Holds selected source for drawer view

### `useChatStore` (Zustand)
- `messages` — conversation message history
- `streamingContent` — accumulator for SSE tokens
- `isStreaming` — prevents concurrent streams
- `conversationId` — current active conversation

## Hooks

### `useKnowledgeSources(page, size)`
TanStack Query wrapper for listing knowledge sources.

### `useCreateKnowledgeSource()` / `useUpdateKnowledgeSource()` / `useDeleteKnowledgeSource()`
Mutations with toast feedback and cache invalidation.

### `useSyncKnowledgeSource()`
Triggers sync and polls for status updates every 3 seconds.

### `useConversations(page, size)` / `useConversation(id)`
TanStack Query wrappers for conversation management.

### `useStreamingChat()`
- Calls `chatService.streamChat()` with fetch + ReadableStream
- Accumulates tokens into `useChatStore.streamingContent`
- Commits final message on completion

## API Integration

All API calls go through `src/lib/api.ts`:
- `apiClient` — axios instance pointed at `NEXT_PUBLIC_API_URL`
- `get()`, `post()`, `put()`, `del()` — typed helpers unwrapping `ApiResponse<T>`
- `streamChat()` — fetch-based SSE reader

Service files in `src/services/`:
- `knowledge.service.ts` — CRUD + sync for knowledge sources
- `chat.service.ts` — Conversations + streaming chat

## Streaming Flow

```
User presses Enter / Send
       ↓
useStreamingChat.sendMessage(message)
       ↓
fetch POST /api/chat { conversationId, message }
Accept: text/event-stream
       ↓
ReadableStream reader + TextDecoder
       ↓
Parse SSE lines: "data: <token>"
       ↓
useChatStore.appendStreamingToken(token)
       ↓
React re-renders ChatWindow with streamingContent
       ↓
StreamingRenderer shows partial markdown
       ↓
onDone → commitStreamedMessage()
       ↓
Message added to messages[] list
```

## Environment Variables

| Variable | Description |
|---|---|
| `NEXT_PUBLIC_API_URL` | Backend API URL (e.g. `http://localhost:8080`) |

## Run Commands

```bash
npm install       # Install dependencies
npm run dev       # Development server on :3000
npm run build     # Production build
npm start         # Start production server
npm run lint      # ESLint
npm run type-check # TypeScript check
```
