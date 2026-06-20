-- =============================================================================
-- V2: Seed Data
-- Sample knowledge sources and conversations for development
-- =============================================================================

-- Sample knowledge sources
INSERT INTO knowledge_sources (id, name, description, type, base_url, sync_status) VALUES
(
    uuid_generate_v4(),
    'Spring AI Documentation',
    'Official Spring AI documentation website',
    'WEBSITE_URL',
    'https://docs.spring.io/spring-ai/reference/',
    'PENDING'
),
(
    uuid_generate_v4(),
    'OpenAI API Docs',
    'OpenAI API reference documentation',
    'WEBSITE_URL',
    'https://platform.openai.com/docs/introduction',
    'PENDING'
),
(
    uuid_generate_v4(),
    'RAG Platform README',
    'The readme for this platform as sample text',
    'RAW_TEXT',
    NULL,
    'PENDING'
);

-- Sample conversation
INSERT INTO conversations (id, title, created_at, updated_at) VALUES
(
    uuid_generate_v4(),
    'Welcome to RAG Platform',
    NOW(),
    NOW()
);
