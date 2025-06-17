CREATE TABLE employee_investment_declarations (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    declaration_year VARCHAR(10) NOT NULL, -- E.g., "2023-2024"
    it_declaration_amount DECIMAL(19,2) DEFAULT 0.00,
    fbp_opted_amount DECIMAL(19,2) DEFAULT 0.00,
    it_document_url VARCHAR(512),
    fbp_choices_json TEXT,
    status VARCHAR(50) NOT NULL, -- E.g., "SUBMITTED", "APPROVED_BY_HR", "REJECTED_BY_HR"
    submitted_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by_id BIGINT,
    reviewed_at TIMESTAMP WITHOUT TIME ZONE,
    hr_comments TEXT,
    updated_at TIMESTAMP WITHOUT TIME ZONE, -- For general entity updates by AuditingListener
    CONSTRAINT fk_inv_decl_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_inv_decl_reviewed_by FOREIGN KEY (reviewed_by_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_employee_declaration_year UNIQUE (employee_id, declaration_year)
);

-- Index to efficiently query declarations for a specific employee and year (covered by uq constraint too)
CREATE INDEX idx_inv_decl_employee_year ON employee_investment_declarations(employee_id, declaration_year);
-- Index to efficiently query declarations by status
CREATE INDEX idx_inv_decl_status ON employee_investment_declarations(status);

COMMENT ON TABLE employee_investment_declarations IS 'Stores employee investment declarations for IT and FBP for a given financial year.';
COMMENT ON COLUMN employee_investment_declarations.employee_id IS 'Foreign key to the employee who made the declaration.';
COMMENT ON COLUMN employee_investment_declarations.declaration_year IS 'Financial year for the declaration (e.g., "2023-2024").';
COMMENT ON COLUMN employee_investment_declarations.it_declaration_amount IS 'Total amount declared for income tax exemptions/deductions.';
COMMENT ON COLUMN employee_investment_declarations.fbp_opted_amount IS 'Total amount opted for under Flexible Benefit Plan.';
COMMENT ON COLUMN employee_investment_declarations.it_document_url IS 'URL or path to the uploaded proof document for IT declaration.';
COMMENT ON COLUMN employee_investment_declarations.fbp_choices_json IS 'JSON string detailing the choices made under FBP components.';
COMMENT ON COLUMN employee_investment_declarations.status IS 'Current status of the declaration (e.g., SUBMITTED, APPROVED_BY_HR, REJECTED_BY_HR).';
COMMENT ON COLUMN employee_investment_declarations.submitted_at IS 'Timestamp when the employee submitted this declaration.';
COMMENT ON COLUMN employee_investment_declarations.reviewed_by_id IS 'Foreign key to the HR user who reviewed/approved/rejected this declaration.';
COMMENT ON COLUMN employee_investment_declarations.reviewed_at IS 'Timestamp when the HR review action was taken.';
COMMENT ON COLUMN employee_investment_declarations.hr_comments IS 'Comments provided by HR during review.';
COMMENT ON COLUMN employee_investment_declarations.updated_at IS 'Timestamp of the last general update to the record (e.g. employee edit before approval).';
