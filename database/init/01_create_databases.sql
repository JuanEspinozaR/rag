-- =============================================================================
-- Database initialization: Create databases and extensions
-- Runs automatically on first PostgreSQL container start
-- =============================================================================

-- Create Langfuse database (separate from main app DB)
CREATE DATABASE langfuse;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE langfuse TO raguser;
