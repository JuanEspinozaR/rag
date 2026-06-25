# RAG Platform Backend

Spring Boot 3.3 + Java 21 backend for the RAG Platform.

## Architecture

This backend follows **Clean Architecture** (Ports & Adapters):

```
api/              ← REST controllers, exception handlers, response wrappers
application/      ← Use case interfaces (ports), DTOs, service implementations
domain/           ← Entity models, repository interfaces, enums
infrastructure/   ← JPA implementations, connectors, Langfuse client, RAG pipeline
config/           ← Spring configuration classes
```

Dependency rule: `api` → `application` → `domain` ← `infrastructure`

## Packages

| Package | Contents |
|---|---|
| `api.controller` | REST endpoints |
| `api.response` | `ApiResponse<T>`, `PageResponse<T>` wrappers |
| `api.exception` | `GlobalExceptionHandler`, `ResourceNotFoundException` |
| `application.dto` | Request/Response DTOs |
| `application.port` | Use case interfaces |
| `application.service` | Service implementations |
| `domain.model` | JPA entities: `KnowledgeSource`, `Document`, `Conversation`, `Message` |
| `domain.repository` | Spring Data JPA repositories |
| `domain.enums` | `SourceType`, `SyncStatus`, `MessageRole` |
| `infrastructure.connector` | Pluggable data connectors |
| `infrastructure.langfuse` | Langfuse HTTP client |
| `infrastructure.rag` | Vector search, document sync pipeline |
| `config` | CORS, Spring AI, WebClient, OpenAPI, App properties |

## Run Commands

### Prerequisites

- Java 21+
- Maven 3.9+ (or use `./mvnw`)
- PostgreSQL 16 with pgvector extension

### Local Development

```bash
# Set environment variables
export OPENAI_API_KEY=sk-...
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ragplatform
export SPRING_DATASOURCE_USERNAME=raguser
export SPRING_DATASOURCE_PASSWORD=ragpassword

# Run with local profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Build

```bash
./mvnw clean package -DskipTests
java -jar target/rag-platform-backend.jar
```

### Test

```bash
./mvnw test
```

## Migrations

Flyway runs automatically on startup. Migration files are in:
```
src/main/resources/db/migration/
  V1__init.sql   ← Full schema (extensions, tables, indexes, triggers)
  V2__seed.sql   ← Sample data
```

To run manually:
```bash
./mvnw flyway:migrate
```

## Vector Search Flow

1. **Indexing** (sync): Document → chunks → `EmbeddingModel.embed()` → `VectorStore.add()`
2. **Retrieval** (chat): Query → `EmbeddingModel.embed()` → `VectorStore.similaritySearch()` → top-k chunks
3. **Generation**: chunks + history → augmented prompt → `ChatClient.stream()` → SSE

```
User Query
    ↓
EmbeddingModel (text-embedding-3-small)
    ↓
VectorStore.similaritySearch(topK=5, threshold=0.5)
    ↓
Top-K document chunks
    ↓
Build system prompt with context
    ↓
ChatClient.stream() → Flux<String>
    ↓
SSE tokens to frontend
```

## Spring AI Setup

Spring AI is configured via `application.yml`:

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4.1-mini
      embedding:
        options:
          model: text-embedding-3-small
    vectorstore:
      pgvector:
        dimensions: 1536
        distance-type: cosine_distance
        initialize-schema: false  # managed by Flyway
```

Beans auto-configured by Spring AI:
- `ChatModel` → `ChatClient` (built in `SpringAIConfig`)
- `EmbeddingModel` (injected into `DocumentSyncService`)
- `VectorStore` / `PgVectorStore` (injected into `DocumentSyncService`, `RagPipelineService`)

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/health` | Health check |
| `GET` | `/api/info` | App info |
| `POST` | `/api/knowledge-sources` | Create source |
| `GET` | `/api/knowledge-sources` | List sources |
| `GET` | `/api/knowledge-sources/{id}` | Get source |
| `PUT` | `/api/knowledge-sources/{id}` | Update source |
| `DELETE` | `/api/knowledge-sources/{id}` | Delete source |
| `POST` | `/api/knowledge-sources/{id}/sync` | Trigger sync |
| `GET` | `/api/knowledge-sources/{id}/documents` | List documents |
| `POST` | `/api/chat` | Stream chat (SSE) |
| `GET` | `/api/chat/conversations` | List conversations |
| `GET` | `/api/chat/conversations/{id}` | Get conversation |
| `DELETE` | `/api/chat/conversations/{id}` | Delete conversation |
| `GET` | `/actuator/health` | Spring Boot health |
| `GET` | `/swagger-ui.html` | Swagger UI |

## Adding a New Connector

1. Create a class implementing `DataSourceConnector`
2. Annotate with `@Component`
3. Implement `supportedType()` returning your new `SourceType` enum value
4. Implement `fetch(url, config)` returning `List<FetchedDocument>`
5. The `ConnectorFactory` auto-discovers it via Spring DI

Example:
```java
@Component
public class GithubRepoConnector implements DataSourceConnector {
    @Override
    public SourceType supportedType() { return SourceType.GITHUB_REPO; }
    
    @Override
    public List<FetchedDocument> fetch(String url, Map<String, Object> config) {
        // fetch repo files from GitHub API
    }
}
```
