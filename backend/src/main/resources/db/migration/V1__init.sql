-- =============================================================================
-- V1: Initial Schema
-- RAG Platform Database
-- =============================================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;

-- =============================================================================
-- knowledge_sources: Configurable data source connections
-- =============================================================================
CREATE TABLE knowledge_sources (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    type        VARCHAR(50)  NOT NULL,
    base_url    TEXT,
    config      JSONB        DEFAULT '{}'::jsonb,
    sync_status VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    last_synced_at TIMESTAMP WITH TIME ZONE,
    document_count INTEGER    DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE knowledge_sources IS 'Configurable data source connections for the RAG pipeline';
COMMENT ON COLUMN knowledge_sources.type IS 'WEBSITE_URL | SITEMAP | PDF_URL | MARKDOWN_URL | RAW_TEXT | GITHUB_REPO';
COMMENT ON COLUMN knowledge_sources.sync_status IS 'PENDING | SYNCING | COMPLETED | FAILED';

-- =============================================================================
-- documents: Fetched and parsed documents from knowledge sources
-- =============================================================================
CREATE TABLE documents (
    id                  UUID    PRIMARY KEY DEFAULT uuid_generate_v4(),
    knowledge_source_id UUID    NOT NULL REFERENCES knowledge_sources(id) ON DELETE CASCADE,
    external_id         VARCHAR(500),
    title               TEXT,
    content             TEXT,
    checksum            VARCHAR(64),
    metadata            JSONB   DEFAULT '{}'::jsonb,
    chunk_count         INTEGER DEFAULT 0,
    synced_at           TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE documents IS 'Documents fetched from knowledge sources';

-- =============================================================================
-- document_chunks: Chunked content ready for embedding
-- =============================================================================
CREATE TABLE document_chunks (
    id          UUID    PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID    NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content     TEXT    NOT NULL,
    token_count INTEGER,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE document_chunks IS 'Document content split into chunks for embedding';

-- =============================================================================
-- vector_store: Spring AI managed embedding store (pgvector)
-- =============================================================================
CREATE TABLE IF NOT EXISTS vector_store (
    id        UUID    PRIMARY KEY DEFAULT uuid_generate_v4(),
    content   TEXT,
    metadata  JSONB,
    embedding vector(1536)
);

COMMENT ON TABLE vector_store IS 'Spring AI PgVectorStore: embeddings for similarity search';

-- =============================================================================
-- conversations: Chat conversation sessions
-- =============================================================================
CREATE TABLE conversations (
    id         UUID    PRIMARY KEY DEFAULT uuid_generate_v4(),
    title      VARCHAR(500) NOT NULL DEFAULT 'New Conversation',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE conversations IS 'Chat conversation sessions';

-- =============================================================================
-- messages: Individual messages in a conversation
-- =============================================================================
CREATE TABLE messages (
    id              UUID    PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID    NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,
    content         TEXT    NOT NULL,
    model           VARCHAR(100),
    prompt_tokens   INTEGER,
    completion_tokens INTEGER,
    total_tokens    INTEGER,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN messages.role IS 'user | assistant | system';

-- =============================================================================
-- Indexes
-- =============================================================================

-- knowledge_sources
CREATE INDEX idx_knowledge_sources_type       ON knowledge_sources(type);
CREATE INDEX idx_knowledge_sources_sync_status ON knowledge_sources(sync_status);

-- documents
CREATE INDEX idx_documents_knowledge_source   ON documents(knowledge_source_id);
CREATE INDEX idx_documents_checksum           ON documents(checksum);
CREATE INDEX idx_documents_external_id        ON documents(external_id);

-- document_chunks
CREATE INDEX idx_chunks_document              ON document_chunks(document_id);
CREATE INDEX idx_chunks_document_index        ON document_chunks(document_id, chunk_index);

-- vector_store: IVFFlat index for approximate nearest neighbor search
CREATE INDEX idx_vector_store_embedding       ON vector_store
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

CREATE INDEX idx_vector_store_metadata        ON vector_store USING gin(metadata);

-- conversations
CREATE INDEX idx_conversations_created       ON conversations(created_at DESC);

-- messages
CREATE INDEX idx_messages_conversation        ON messages(conversation_id);
CREATE INDEX idx_messages_conversation_time   ON messages(conversation_id, created_at);

-- =============================================================================
-- Triggers: auto-update updated_at
-- =============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_knowledge_sources_updated_at
    BEFORE UPDATE ON knowledge_sources
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_conversations_updated_at
    BEFORE UPDATE ON conversations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
