-- Add index on is_active column in users table for filtering
CREATE INDEX idx_users_is_active ON users(is_active);

-- Explicitly add index on company_id in users table if not already automatically created by FK constraint
-- Most modern databases (like PostgreSQL) automatically create an index for foreign key columns.
-- This is more of a "just in case" or for databases that don't.
-- Check your specific database behavior. If an index like users_company_id_fkey_idx already exists, this might be redundant.
-- Example: CREATE INDEX IF NOT EXISTS idx_users_company_id ON users(company_id);
-- For this script, we'll assume it might be beneficial or for cross-db compatibility if it wasn't auto-created.
-- However, to avoid errors if it *does* exist (e.g. PostgreSQL default naming `users_company_id_idx`),
-- it's safer to use `IF NOT EXISTS` if the SQL dialect supports it, or check manually.
-- Given the tool environment, a simple CREATE INDEX is usually fine. If it fails due to existing index, it's informative.

-- Let's assume we want to ensure it, but be cautious of existing auto-indexes.
-- For the purpose of this exercise, we'll add it. If it fails in a real scenario, investigate.
CREATE INDEX idx_users_fk_company_id ON users(company_id);

-- Composite index for common HR/Manager roster filtering
CREATE INDEX idx_users_company_is_active ON users(company_id, is_active);

COMMENT ON INDEX idx_users_is_active IS 'Index on the isActive flag in the users table for faster filtering of active/inactive users.';
COMMENT ON INDEX idx_users_fk_company_id IS 'Explicit index on the company_id foreign key in the users table.';
COMMENT ON INDEX idx_users_company_is_active IS 'Composite index for filtering users by company and active status.';
