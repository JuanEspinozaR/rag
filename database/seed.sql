-- =============================================================================
-- RAG Platform - Standalone Seed Script
-- Run: make db-seed
-- Or: psql -U raguser -d ragplatform -f seed.sql
-- =============================================================================

-- Additional sample data beyond Flyway migration V2

-- More knowledge sources
INSERT INTO knowledge_sources (name, description, type, base_url, sync_status) VALUES
    ('Next.js Documentation', 'Next.js official docs', 'SITEMAP', 'https://nextjs.org/sitemap.xml', 'PENDING'),
    ('PostgreSQL Documentation', 'PostgreSQL 16 documentation', 'WEBSITE_URL', 'https://www.postgresql.org/docs/16/index.html', 'PENDING'),
    ('Docker Compose Reference', 'Docker Compose file reference', 'MARKDOWN_URL', 'https://raw.githubusercontent.com/docker/compose/main/docs/compose-file/index.md', 'PENDING')
ON CONFLICT DO NOTHING;

-- Sample conversations with messages
DO $$
DECLARE
    v_conv_id UUID;
BEGIN
    INSERT INTO conversations (title) VALUES ('Getting Started Guide') RETURNING id INTO v_conv_id;

    INSERT INTO messages (conversation_id, role, content, model) VALUES
    (v_conv_id, 'user', 'What is RAG and how does it work?', NULL),
    (v_conv_id, 'assistant',
     'RAG (Retrieval-Augmented Generation) is a technique that enhances Large Language Models by providing them with relevant context retrieved from a knowledge base before generating a response.

**How it works:**
1. **Indexing**: Documents are chunked and converted to vector embeddings, stored in pgvector
2. **Retrieval**: When a user asks a question, it is also embedded and similar chunks are found via cosine similarity
3. **Augmentation**: Retrieved chunks are added to the prompt as context
4. **Generation**: The LLM generates a response grounded in the retrieved context

This platform handles all of these steps automatically!',
     'gpt-4.1-mini');
END;
$$;
