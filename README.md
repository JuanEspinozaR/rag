# RAG Platform

A production-ready, self-hosted **Retrieval-Augmented Generation (RAG)** platform built as a monorepo. Configure knowledge sources, sync documents, generate embeddings, and chat with your data through a ChatGPT-style streaming interface.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Tech Stack](#3-tech-stack)
4. [Local Setup](#4-local-setup)
5. [Environment Variables](#5-environment-variables)
6. [Docker Compose Setup](#6-docker-compose-setup)
7. [Backend Setup](#7-backend-setup)
8. [Frontend Setup](#8-frontend-setup)
9. [Database Setup](#9-database-setup)
10. [Langfuse Setup](#10-langfuse-setup)
11. [PgAdmin Setup](#11-pgadmin-setup)
12. [How Syncing Works](#12-how-syncing-works)
13. [How the RAG Pipeline Works](#13-how-the-rag-pipeline-works)
14. [Default URLs](#14-default-urls)
15. [API Documentation](#15-api-documentation)
16. [Troubleshooting](#16-troubleshooting)

---

## 1. Project Overview

RAG Platform lets you:

- **Configure knowledge sources** — websites, sitemaps, PDFs, Markdown files, or raw text
- **Sync and process documents** — fetch, parse, chunk, and embed content automatically
- **Store vectors in PostgreSQL** — using the pgvector extension for high-performance similarity search
- **Chat with your knowledge base** — stream AI responses grounded in your documents
- **Observe and trace** — monitor every LLM interaction with Langfuse
- **Self-host everything** — single `docker compose up --build` command

---

## 2. Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Monorepo Root                           │
│                                                                 │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────┐  │
│  │   Frontend   │    │   Backend    │    │    Database      │  │
│  │  (Next.js)   │◄──►│ (Spring Boot)│◄──►│  (PostgreSQL +   │  │
│  │    :3000     │    │    :8080     │    │    pgvector)     │  │
│  └──────────────┘    └──────┬───────┘    └──────────────────┘  │
│                             │                                   │
│                    ┌────────┴────────┐                          │
│                    │                 │                          │
│              ┌─────▼─────┐   ┌──────▼──────┐                   │
│              │  OpenAI   │   │  Langfuse   │                   │
│              │    API    │   │   :3001     │                   │
│              └───────────┘   └─────────────┘                   │
└─────────────────────────────────────────────────────────────────┘
```

### Clean Architecture (Backend)

```
API Layer (Controllers)
        ↓
Application Layer (Use Cases / Services)
        ↓
Domain Layer (Entities / Repository Interfaces)
        ↓
Infrastructure Layer (JPA / Connectors / Langfuse)
```

### Frontend Feature Architecture

```
app/                    ← Next.js App Router pages
components/features/    ← Feature-specific components
components/ui/          ← Reusable shadcn/ui components
services/               ← API layer abstraction
store/                  ← Zustand global state
hooks/                  ← Custom React hooks
types/                  ← TypeScript type definitions
```

---

## 3. Tech Stack

| Layer       | Technology                                                                 |
|-------------|---------------------------------------------------------------------------|
| Frontend    | Next.js 14, React 18, TypeScript, TailwindCSS, shadcn/ui, Zustand        |
| Backend     | Spring Boot 3.3, Java 21, Spring AI, Spring WebFlux, Spring Data JPA     |
| Database    | PostgreSQL 16 + pgvector extension                                         |
| Migrations  | Flyway                                                                     |
| LLM         | OpenAI (gpt-4.1-mini + text-embedding-3-small)                            |
| Tracing     | Langfuse (self-hosted)                                                     |
| Streaming   | Server-Sent Events (SSE)                                                   |
| Build       | Docker Compose, Maven, npm                                                 |

---

## 4. Local Setup

### Prerequisites

- Docker 24+ and Docker Compose v2
- Java 21+ (for running backend outside Docker)
- Node.js 20+ (for running frontend outside Docker)
- Maven 3.9+ (or use the included wrapper)
- An OpenAI API key

### Quick Start (Docker Compose)

```bash
# 1. Clone the repository
git clone <repo-url>
cd rag-platform

# 2. Set up environment
cp .env.example .env
# Edit .env and fill in your OPENAI_API_KEY and other credentials

# 3. Start everything
docker compose up --build

# Or use the Makefile
make up-logs
```

This starts: frontend, backend, postgres, pgadmin, langfuse-server, langfuse-worker, redis, clickhouse.

---

## 5. Environment Variables

All configuration is environment-variable driven. See [`.env.example`](.env.example) for the full list.

| Variable | Description | Required |
|---|---|---|
| `OPENAI_API_KEY` | Your OpenAI API key | ✅ |
| `OPENAI_CHAT_MODEL` | Chat model (default: `gpt-4.1-mini`) | ✅ |
| `OPENAI_EMBEDDING_MODEL` | Embedding model (default: `text-embedding-3-small`) | ✅ |
| `POSTGRES_DB` | PostgreSQL database name | ✅ |
| `POSTGRES_USER` | PostgreSQL username | ✅ |
| `POSTGRES_PASSWORD` | PostgreSQL password | ✅ |
| `SPRING_DATASOURCE_URL` | Full JDBC URL for backend | ✅ |
| `LANGFUSE_PUBLIC_KEY` | Langfuse project public key | ✅ |
| `LANGFUSE_SECRET_KEY` | Langfuse project secret key | ✅ |
| `LANGFUSE_HOST` | Langfuse server URL | ✅ |
| `NEXT_PUBLIC_API_URL` | Backend API URL for frontend | ✅ |
| `PGADMIN_DEFAULT_EMAIL` | PgAdmin login email | Optional |
| `PGADMIN_DEFAULT_PASSWORD` | PgAdmin login password | Optional |

---

## 6. Docker Compose Setup

The `docker-compose.yml` defines the full service graph:

```yaml
services:
  frontend     → Next.js app on :3000
  backend      → Spring Boot API on :8080
  postgres     → PostgreSQL + pgvector on :5432
  pgadmin      → Database GUI on :5050
  langfuse-server → Langfuse web UI/API on :3001
  langfuse-worker → Background job processor
  redis        → Cache for Langfuse on :6379
  clickhouse   → Analytics store for Langfuse on :8123
```

### Single command startup

```bash
docker compose up --build
```

### Environment Variable Passing

Docker Compose reads from `.env` automatically. You can override per-service in `docker-compose.yml` under `environment:`.

### Volumes

| Volume | Purpose |
|---|---|
| `postgres_data` | Persistent PostgreSQL data |
| `pgadmin_data` | PgAdmin configuration |
| `redis_data` | Redis persistence |
| `clickhouse_data` | ClickHouse analytics data |
| `langfuse_data` | Langfuse uploaded files |

---

## 7. Backend Setup

See [backend/README.md](backend/README.md) for detailed backend documentation.

### Run locally (without Docker)

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Requires a running PostgreSQL instance with pgvector installed.

### Build JAR

```bash
cd backend
./mvnw clean package -DskipTests
java -jar target/rag-platform-backend.jar
```

### Key endpoints

| Endpoint | Description |
|---|---|
| `GET /actuator/health` | Health check |
| `GET /swagger-ui.html` | Swagger UI |
| `POST /api/knowledge-sources` | Create knowledge source |
| `POST /api/knowledge-sources/{id}/sync` | Sync a source |
| `POST /api/chat` | Chat with streaming |
| `GET /api/chat/conversations` | List conversations |

---

## 8. Frontend Setup

See [frontend/README.md](frontend/README.md) for detailed frontend documentation.

### Run locally (without Docker)

```bash
cd frontend
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

### Build for production

```bash
cd frontend
npm run build
npm start
```

---

## 9. Database Setup

PostgreSQL 16 with the pgvector extension is used for both relational data and vector storage.

### Schema overview

| Table | Purpose |
|---|---|
| `knowledge_sources` | Data source configurations |
| `documents` | Fetched and parsed documents |
| `document_chunks` | Chunked document content |
| `vector_store` | Embeddings (managed by Spring AI) |
| `conversations` | Chat conversation metadata |
| `messages` | Individual chat messages |

### Migrations

Flyway runs automatically on backend startup. Migration files are in:
```
backend/src/main/resources/db/migration/
  V1__init.sql         ← Schema creation
  V2__seed.sql         ← Sample data
```

### Manual connection

```bash
make db-connect
# Or:
docker compose exec postgres psql -U raguser -d ragplatform
```

---

## 10. Langfuse Setup

Langfuse is self-hosted and provides LLM observability.

### Access

- URL: [http://localhost:3001](http://localhost:3001)
- Default credentials: set via `LANGFUSE_INIT_USER_EMAIL` and `LANGFUSE_INIT_USER_PASSWORD`

### Getting API Keys

1. Log in to Langfuse at http://localhost:3001
2. Navigate to **Settings → API Keys**
3. Create a new key pair
4. Copy the **Public Key** and **Secret Key** to your `.env` file:
   ```
   LANGFUSE_PUBLIC_KEY=pk-lf-...
   LANGFUSE_SECRET_KEY=sk-lf-...
   ```
5. Restart the backend service

### What is traced

Every chat request traces:
- User prompt
- Retrieved document chunks (context)
- Augmented prompt sent to OpenAI
- Token usage and latency
- Full conversation trace

---

## 11. PgAdmin Setup

- URL: [http://localhost:5050](http://localhost:5050)
- Login: `PGADMIN_DEFAULT_EMAIL` / `PGADMIN_DEFAULT_PASSWORD` from `.env`

### Add a server connection

1. Right-click **Servers → Register → Server**
2. Name: `RAG Platform`
3. Connection tab:
   - Host: `postgres`
   - Port: `5432`
   - Database: `ragplatform`
   - Username: `raguser`
   - Password: `ragpassword`

---

## 12. How Syncing Works

```
User clicks "Sync" → POST /api/knowledge-sources/{id}/sync
        ↓
Backend determines source type (WEBSITE_URL, PDF_URL, etc.)
        ↓
ConnectorFactory selects the right DataSourceConnector
        ↓
Connector fetches content from the URL
        ↓
Content is parsed and cleaned
        ↓
DocumentSyncService chunks text into ~512-token segments
        ↓
EmbeddingService calls OpenAI text-embedding-3-small
        ↓
Embeddings + chunks stored in PostgreSQL via pgvector
        ↓
Spring AI VectorStore indexes for similarity search
        ↓
SyncStatus updated to COMPLETED
```

### Supported source types

| Type | Description |
|---|---|
| `WEBSITE_URL` | Fetches and parses an HTML webpage |
| `SITEMAP` | Parses a sitemap.xml and syncs all listed URLs |
| `PDF_URL` | Downloads and extracts text from a PDF |
| `MARKDOWN_URL` | Fetches a raw Markdown file |
| `RAW_TEXT` | Stores text typed directly into the form |
| `GITHUB_REPO` | Future: sync a GitHub repository |

---

## 13. How the RAG Pipeline Works

```
User types a message → POST /api/chat (SSE streaming)
        ↓
ChatService embeds the user message (OpenAI embeddings)
        ↓
Similarity search in pgvector
  SELECT * FROM vector_store ORDER BY embedding <=> $query_embedding LIMIT 5
        ↓
Top-k document chunks retrieved as context
        ↓
Augmented prompt built:
  [System: "Answer using only the provided context"]
  [Context: chunk1 + chunk2 + chunk3...]
  [History: previous messages in conversation]
  [User: user's question]
        ↓
ChatClient streams response from OpenAI gpt-4.1-mini
        ↓
Tokens streamed back as Server-Sent Events
        ↓
Frontend renders tokens in real-time (ChatGPT-style)
        ↓
Langfuse trace created with all details
        ↓
Full response + user message persisted to DB
```

---

## 14. Default URLs

| Service | URL | Notes |
|---|---|---|
| Frontend | http://localhost:3000 | Next.js app |
| Backend API | http://localhost:8080 | Spring Boot |
| Swagger UI | http://localhost:8080/swagger-ui.html | API docs |
| Health Check | http://localhost:8080/actuator/health | Service health |
| PgAdmin | http://localhost:5050 | DB management |
| Langfuse | http://localhost:3001 | LLM tracing |
| ClickHouse | http://localhost:8123 | Analytics |

---

## 15. API Documentation

Swagger UI is available at http://localhost:8080/swagger-ui.html when the backend is running.

OpenAPI JSON spec: http://localhost:8080/v3/api-docs

---

## 16. Troubleshooting

### Backend fails to start

```bash
docker compose logs backend
```

Common causes:
- PostgreSQL not ready yet — backend has a healthcheck dependency, retry with `make restart-backend`
- Missing `OPENAI_API_KEY` — set it in `.env`
- Port 8080 already in use — change `SERVER_PORT` in `.env`

### pgvector extension missing

```sql
-- Run in PostgreSQL
CREATE EXTENSION IF NOT EXISTS vector;
```

Or re-run migrations: `make db-migrate`

### Frontend can't reach backend

Check `NEXT_PUBLIC_API_URL` in `.env`. For Docker, use `http://backend:8080`. For local dev, use `http://localhost:8080`.

### Langfuse not receiving traces

1. Verify `LANGFUSE_PUBLIC_KEY` and `LANGFUSE_SECRET_KEY` are set correctly
2. Check `LANGFUSE_HOST` points to the running Langfuse server
3. Check backend logs for Langfuse client errors

### Streaming chat not working

- Ensure the backend is reachable from the browser
- Check browser CORS errors in DevTools
- Verify `NEXT_PUBLIC_API_URL` is set to a URL accessible from the browser

### Out of memory during build

Increase Docker Desktop memory to 4GB+ in Settings → Resources.

### Full reset

```bash
make down-clean  # removes all containers + volumes
make up-logs     # starts fresh
```
