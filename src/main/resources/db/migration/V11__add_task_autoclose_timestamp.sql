-- Add auto_closed_at column to employee_tasks table
ALTER TABLE employee_tasks
ADD COLUMN auto_closed_at TIMESTAMP WITHOUT TIME ZONE NULL;

COMMENT ON COLUMN employee_tasks.auto_closed_at IS 'Timestamp when the task was automatically closed by the system due to deadline passing.';
