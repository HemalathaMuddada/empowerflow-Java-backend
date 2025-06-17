-- Add manager_id column to users table
ALTER TABLE users
ADD COLUMN manager_id BIGINT;

-- Add foreign key constraint for manager_id
ALTER TABLE users
ADD CONSTRAINT fk_user_manager
FOREIGN KEY (manager_id)
REFERENCES users(id)
ON DELETE SET NULL; -- If a manager is deleted, their reportees' manager_id becomes NULL.
                     -- ON DELETE RESTRICT would prevent deletion if they manage others.

-- Add index for faster queries on manager_id
CREATE INDEX idx_users_manager_id ON users(manager_id);
