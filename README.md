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


| Layer      | Technology                                                           |
| ---------- | -------------------------------------------------------------------- |
| Frontend   | Next.js 14, React 18, TypeScript, TailwindCSS, shadcn/ui, Zustand    |
| Backend    | Spring Boot 3.3, Java 21, Spring AI, Spring WebFlux, Spring Data JPA |
| Database   | PostgreSQL 16 + pgvector extension                                   |
| Migrations | Flyway                                                               |
| LLM        | OpenAI (gpt-4.1-mini + text-embedding-3-small)                       |
| Tracing    | Langfuse (self-hosted)                                               |
| Streaming  | Server-Sent Events (SSE)                                             |
| Build      | Docker Compose, Maven, npm                                           |


---

## 4. Local Setup

### Prerequisites

- Docker 24+ and Docker Compose v2
- Java 21+ (for running backend outside Docker)
- Node.js 20+ (for running frontend outside Docker)
- Maven 3.9+ (or use the included wrapper)
- An OpenAI API key

### Quick Start (Docker Compose)

The platform is split into **two independent stacks** so that Langfuse can start first and provide API keys before the main app boots.

#### Step 1 — Start the Langfuse observability stack

```bash
# 1. Clone the repository
git clone <repo-url>
cd rag-platform

# 2. Set up environment
cp .env.example .env

# 3. Start Langfuse (postgres, redis, clickhouse, minio, langfuse-server, langfuse-worker)
docker compose -f docker-compose.langfuse.yml up -d
```

Wait ~2 minutes for all services to become healthy, then:

1. Open **[http://localhost:3001](http://localhost:3001)**
2. Log in: `admin@langfuse.com` / `langfusepassword`
3. Go to **Settings → API Keys → Create new key**
4. Copy the **Public Key** and **Secret Key** into `.env`:

```env
LANGFUSE_PUBLIC_KEY=pk-lf-xxxxxxxxxxxxxxxx
LANGFUSE_SECRET_KEY=sk-lf-xxxxxxxxxxxxxxxx
```

#### Step 2 — Start the main application stack

```bash
# Fill in OPENAI_API_KEY and the Langfuse keys from Step 1, then:
docker compose up --build
```

This starts: `frontend (:3000)`, `backend (:8080)`, `postgres (:5432)`, `pgadmin (:5050)`.

The backend will automatically connect to the already-running `langfuse-server` container through the shared `rag-platform` Docker network.

#### Stopping everything

```bash
docker compose down                                    # stop main app
docker compose -f docker-compose.langfuse.yml down    # stop Langfuse
```

---

## 5. Environment Variables

All configuration is environment-variable driven. See `[.env.example](.env.example)` for the full list.


| Variable                   | Description                                         | Required |
| -------------------------- | --------------------------------------------------- | -------- |
| `OPENAI_API_KEY`           | Your OpenAI API key                                 | ✅        |
| `OPENAI_CHAT_MODEL`        | Chat model (default: `gpt-4.1-mini`)                | ✅        |
| `OPENAI_EMBEDDING_MODEL`   | Embedding model (default: `text-embedding-3-small`) | ✅        |
| `POSTGRES_DB`              | PostgreSQL database name                            | ✅        |
| `POSTGRES_USER`            | PostgreSQL username                                 | ✅        |
| `POSTGRES_PASSWORD`        | PostgreSQL password                                 | ✅        |
| `SPRING_DATASOURCE_URL`    | Full JDBC URL for backend                           | ✅        |
| `LANGFUSE_PUBLIC_KEY`      | Langfuse project public key                         | ✅        |
| `LANGFUSE_SECRET_KEY`      | Langfuse project secret key                         | ✅        |
| `LANGFUSE_HOST`            | Langfuse server URL                                 | ✅        |
| `NEXT_PUBLIC_API_URL`      | Backend API URL for frontend                        | ✅        |
| `PGADMIN_DEFAULT_EMAIL`    | PgAdmin login email                                 | Optional |
| `PGADMIN_DEFAULT_PASSWORD` | PgAdmin login password                              | Optional |


---

## 6. Docker Compose Setup

The platform uses **two compose files** to decouple the LLM observability stack from the main application.

### `docker-compose.yml` — Main application


| Service    | Port | Description           |
| ---------- | ---- | --------------------- |
| `frontend` | 3000 | Next.js UI            |
| `backend`  | 8080 | Spring Boot API       |
| `postgres` | 5432 | PostgreSQL + pgvector |
| `pgadmin`  | 5050 | Database GUI          |


### `docker-compose.langfuse.yml` — Observability stack


| Service               | Port      | Description                 |
| --------------------- | --------- | --------------------------- |
| `langfuse-server`     | 3001      | Langfuse Web UI + API       |
| `langfuse-worker`     | —         | Background ingestion jobs   |
| `langfuse-postgres`   | 5433      | Dedicated Langfuse database |
| `langfuse-redis`      | —         | Internal queue/cache        |
| `langfuse-clickhouse` | —         | OLAP trace analytics        |
| `langfuse-minio`      | 9090/9091 | S3 blob store (MinIO)       |


### Shared network

Both stacks join a Docker network named `rag-platform`. This allows the `rag-backend` container to reach `langfuse-server` by container name (`http://langfuse-server:3000`) without exposing Langfuse's internal port to the host.

### Volumes


| Volume                     | Stack    | Purpose              |
| -------------------------- | -------- | -------------------- |
| `postgres_data`            | Main     | PostgreSQL data      |
| `pgadmin_data`             | Main     | PgAdmin config       |
| `langfuse_postgres_data`   | Langfuse | Langfuse database    |
| `langfuse_redis_data`      | Langfuse | Redis persistence    |
| `langfuse_clickhouse_data` | Langfuse | ClickHouse analytics |
| `langfuse_minio_data`      | Langfuse | Blob storage         |


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


| Endpoint                                | Description             |
| --------------------------------------- | ----------------------- |
| `GET /actuator/health`                  | Health check            |
| `GET /swagger-ui.html`                  | Swagger UI              |
| `POST /api/knowledge-sources`           | Create knowledge source |
| `POST /api/knowledge-sources/{id}/sync` | Sync a source           |
| `POST /api/chat`                        | Chat with streaming     |
| `GET /api/chat/conversations`           | List conversations      |


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


| Table               | Purpose                           |
| ------------------- | --------------------------------- |
| `knowledge_sources` | Data source configurations        |
| `documents`         | Fetched and parsed documents      |
| `document_chunks`   | Chunked document content          |
| `vector_store`      | Embeddings (managed by Spring AI) |
| `conversations`     | Chat conversation metadata        |
| `messages`          | Individual chat messages          |


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

Langfuse is self-hosted and provides LLM observability. It runs as a **separate stack** that must be started before the main application.

### Start the Langfuse stack

```bash
docker compose -f docker-compose.langfuse.yml up -d
```

### Access


|          |                                                |
| -------- | ---------------------------------------------- |
| URL      | [http://localhost:3001](http://localhost:3001) |
| Email    | `admin@langfuse.com`                           |
| Password | `langfusepassword`                             |


### Getting API Keys

1. Log in at [http://localhost:3001](http://localhost:3001)
2. Navigate to **Settings → API Keys → Create new key**
3. Copy the **Public Key** and **Secret Key** into `.env`:
  ```env
   LANGFUSE_PUBLIC_KEY=pk-lf-...
   LANGFUSE_SECRET_KEY=sk-lf-...
  ```
4. Start (or restart) the main app stack: `docker compose up -d`

### MinIO Console (blob storage)


|          |                                                |
| -------- | ---------------------------------------------- |
| URL      | [http://localhost:9091](http://localhost:9091) |
| User     | `minio`                                        |
| Password | `miniosecret`                                  |


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


| Type           | Description                                    |
| -------------- | ---------------------------------------------- |
| `WEBSITE_URL`  | Fetches and parses an HTML webpage             |
| `SITEMAP`      | Parses a sitemap.xml and syncs all listed URLs |
| `PDF_URL`      | Downloads and extracts text from a PDF         |
| `MARKDOWN_URL` | Fetches a raw Markdown file                    |
| `RAW_TEXT`     | Stores text typed directly into the form       |
| `GITHUB_REPO`  | Future: sync a GitHub repository               |


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


| Service      | URL                                                                            | Notes          |
| ------------ | ------------------------------------------------------------------------------ | -------------- |
| Frontend     | [http://localhost:3000](http://localhost:3000)                                 | Next.js app    |
| Backend API  | [http://localhost:8080](http://localhost:8080)                                 | Spring Boot    |
| Swagger UI   | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | API docs       |
| Health Check | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) | Service health |
| PgAdmin      | [http://localhost:5050](http://localhost:5050)                                 | DB management  |
| Langfuse     | [http://localhost:3001](http://localhost:3001)                                 | LLM tracing    |
| ClickHouse   | [http://localhost:8123](http://localhost:8123)                                 | Analytics      |


---

## 15. API Documentation

Swagger UI is available at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) when the backend is running.

OpenAPI JSON spec: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

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

1. Make sure the Langfuse stack is running: `docker compose -f docker-compose.langfuse.yml ps`
2. Verify `LANGFUSE_PUBLIC_KEY` and `LANGFUSE_SECRET_KEY` in `.env` match the keys in the Langfuse UI
3. Check backend logs: `docker compose logs backend | grep -i langfuse`

### Backend can't connect to Langfuse

The backend reaches Langfuse via the shared `rag-platform` Docker network using the container name `langfuse-server`. If the Langfuse stack was started after the main app, restart the backend:

```bash
docker compose restart backend
```

### Streaming chat not working

- Ensure the backend is reachable from the browser
- Check browser CORS errors in DevTools
- Verify `NEXT_PUBLIC_API_URL` is set to a URL accessible from the browser

### Out of memory during build

Increase Docker Desktop memory to 4GB+ in Settings → Resources.

### Full reset

```bash
# Stop and remove all containers + volumes
docker compose down -v
docker compose -f docker-compose.langfuse.yml down -v

# Start fresh
docker compose -f docker-compose.langfuse.yml up -d
# (wait ~2 min, get API keys, update .env)
docker compose up --build
```

