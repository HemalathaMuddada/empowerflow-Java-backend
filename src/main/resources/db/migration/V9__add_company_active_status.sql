-- Add is_active column to companies table
ALTER TABLE companies
ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Optional: Add a comment to the column
COMMENT ON COLUMN companies.is_active IS 'Flag to indicate if the company is active or deactivated';

-- Optional: Update existing companies to be active by default if any exist (though default TRUE should handle new ones)
-- UPDATE companies SET is_active = TRUE WHERE is_active IS NULL;
-- This line is usually not needed if the ADD COLUMN includes DEFAULT TRUE and NOT NULL,
-- as existing rows will get the default. It's more for cases where default is added later.
