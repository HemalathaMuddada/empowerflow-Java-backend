CREATE TABLE performance_reviews (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    review_cycle_id BIGINT NOT NULL,
    goals_objectives TEXT,
    employee_self_appraisal TEXT,
    manager_evaluation TEXT,
    employee_comments TEXT,
    overall_rating_by_manager INTEGER,
    final_rating INTEGER,
    status VARCHAR(50) NOT NULL,
    submitted_by_employee_at TIMESTAMP WITHOUT TIME ZONE,
    reviewed_by_manager_at TIMESTAMP WITHOUT TIME ZONE,
    acknowledged_by_employee_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pr_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_pr_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_pr_review_cycle FOREIGN KEY (review_cycle_id) REFERENCES performance_review_cycles(id) ON DELETE RESTRICT,
    CONSTRAINT uq_employee_review_cycle UNIQUE (employee_id, review_cycle_id)
);

CREATE INDEX idx_pr_employee_cycle ON performance_reviews(employee_id, review_cycle_id);
CREATE INDEX idx_pr_reviewer_status ON performance_reviews(reviewer_id, status);
CREATE INDEX idx_pr_status ON performance_reviews(status);
CREATE INDEX idx_pr_review_cycle_id ON performance_reviews(review_cycle_id);


COMMENT ON TABLE performance_reviews IS 'Stores individual employee performance review records for specific review cycles.';
COMMENT ON COLUMN performance_reviews.employee_id IS 'Employee being reviewed.';
COMMENT ON COLUMN performance_reviews.reviewer_id IS 'Manager/Reviewer conducting the review.';
COMMENT ON COLUMN performance_reviews.review_cycle_id IS 'The performance review cycle this review belongs to.';
COMMENT ON COLUMN performance_reviews.goals_objectives IS 'Goals and objectives set for the review period.';
COMMENT ON COLUMN performance_reviews.employee_self_appraisal IS 'Self-appraisal submitted by the employee.';
COMMENT ON COLUMN performance_reviews.manager_evaluation IS 'Evaluation provided by the manager/reviewer.';
COMMENT ON COLUMN performance_reviews.employee_comments IS 'Comments from the employee after viewing manager evaluation.';
COMMENT ON COLUMN performance_reviews.overall_rating_by_manager IS 'Overall rating given by the manager (e.g., 1-5).';
COMMENT ON COLUMN performance_reviews.final_rating IS 'Final calibrated rating, if different from manager rating.';
COMMENT ON COLUMN performance_reviews.status IS 'Current stage/status of the review (e.g., PENDING_GOAL_SETTING, PENDING_SELF_APPRAISAL).';
COMMENT ON COLUMN performance_reviews.submitted_by_employee_at IS 'Timestamp when employee submitted their self-appraisal.';
COMMENT ON COLUMN performance_reviews.reviewed_by_manager_at IS 'Timestamp when manager submitted their evaluation.';
COMMENT ON COLUMN performance_reviews.acknowledged_by_employee_at IS 'Timestamp when employee acknowledged the review.';
COMMENT ON COLUMN performance_reviews.created_at IS 'Timestamp when the review record was initiated.';
COMMENT ON COLUMN performance_reviews.updated_at IS 'Timestamp of the last modification to this review record.';
