CREATE TABLE performance_review_cycles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL, -- E.g., "PLANNING", "ACTIVE", "COMPLETED", "ARCHIVED"
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_review_cycle_dates CHECK (end_date >= start_date) -- Ensure end_date is after or same as start_date
);

COMMENT ON TABLE performance_review_cycles IS 'Defines periods for performance reviews (e.g., annual, mid-year).';
COMMENT ON COLUMN performance_review_cycles.name IS 'Unique name for the review cycle (e.g., "Annual Review 2023", "Mid-Year 2024 H1").';
COMMENT ON COLUMN performance_review_cycles.start_date IS 'Start date of the review cycle period.';
COMMENT ON COLUMN performance_review_cycles.end_date IS 'End date of the review cycle period.';
COMMENT ON COLUMN performance_review_cycles.status IS 'Current status of the review cycle (e.g., PLANNING, ACTIVE, COMPLETED, ARCHIVED).';
COMMENT ON COLUMN performance_review_cycles.created_at IS 'Timestamp when the review cycle was created.';
COMMENT ON COLUMN performance_review_cycles.updated_at IS 'Timestamp when the review cycle was last updated.';
