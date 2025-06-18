-- Add published_at column to employee_hikes table
ALTER TABLE employee_hikes
ADD COLUMN published_at TIMESTAMP WITHOUT TIME ZONE NULL;

-- Optional: Add a comment to the column
COMMENT ON COLUMN employee_hikes.published_at IS 'Timestamp when the hike record was published/communicated to the employee';
