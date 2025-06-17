-- Add is_restricted_to_hr column to employee_documents table
ALTER TABLE employee_documents
ADD COLUMN is_restricted_to_hr BOOLEAN NOT NULL DEFAULT FALSE;

-- Optional: Add a comment to the column
COMMENT ON COLUMN employee_documents.is_restricted_to_hr IS 'Flag to indicate if the document is restricted for HR visibility only';
