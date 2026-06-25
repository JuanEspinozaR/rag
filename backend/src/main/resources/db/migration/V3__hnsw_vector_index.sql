-- =============================================================================
-- V3: Replace IVFFlat vector index with HNSW
--
-- IVFFlat with lists=100 on a small dataset (< 1000 rows) causes near-zero
-- recall because the default probes=1 searches only 1 of 100 clusters and
-- most clusters are empty. HNSW has no such limitation and works correctly
-- from 0 to millions of vectors without tuning.
-- =============================================================================

DROP INDEX IF EXISTS idx_vector_store_embedding;

CREATE INDEX idx_vector_store_embedding
    ON vector_store
    USING hnsw (embedding vector_cosine_ops);
