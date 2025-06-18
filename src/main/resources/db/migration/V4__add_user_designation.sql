-- Add designation column to users table
ALTER TABLE users
ADD COLUMN designation VARCHAR(255) NULL; -- Allow NULL initially, can be made NOT NULL if required by business logic later

-- Example of how to update existing users if a default designation is needed (optional):
-- UPDATE users SET designation = 'Default Designation' WHERE designation IS NULL;
