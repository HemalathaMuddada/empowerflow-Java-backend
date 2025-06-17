CREATE TABLE hiring_resumes (
    id BIGSERIAL PRIMARY KEY,
    candidate_name VARCHAR(255) NOT NULL,
    resume_link VARCHAR(2048) NOT NULL,
    skills TEXT,
    category VARCHAR(255) NOT NULL,
    notes TEXT,
    uploaded_by_id BIGINT NOT NULL,
    uploaded_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hiring_resumes_uploaded_by FOREIGN KEY(uploaded_by_id) REFERENCES users(id) ON DELETE RESTRICT -- Or SET NULL if uploader can be deleted
);

CREATE INDEX idx_hiring_resumes_category ON hiring_resumes(category);
CREATE INDEX idx_hiring_resumes_skills ON hiring_resumes(skills); -- May not be very effective for LIKE '%skill%' on TEXT
-- For full-text search on skills, consider GIN/GIST index if using PostgreSQL specific features:
-- CREATE INDEX idx_hiring_resumes_skills_fts ON hiring_resumes USING GIN (to_tsvector('english', skills));
