-- Add offboarding related columns to the users table
ALTER TABLE users
ADD COLUMN offboarding_date DATE NULL,
ADD COLUMN reason_for_leaving TEXT NULL,
ADD COLUMN offboarding_comments_hr TEXT NULL;

COMMENT ON COLUMN users.offboarding_date IS 'Date when the employee offboarding process is completed or their official last working day.';
COMMENT ON COLUMN users.reason_for_leaving IS 'Reason for the employee''s departure, typically recorded by HR during exit interview.';
COMMENT ON COLUMN users.offboarding_comments_hr IS 'Additional comments or notes from HR regarding the employee''s offboarding process.';
