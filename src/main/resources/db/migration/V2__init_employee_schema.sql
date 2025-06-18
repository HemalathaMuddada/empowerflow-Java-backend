-- Ensure the trigger function from V1 exists (or include it here if V2 might run independently)
-- CREATE OR REPLACE FUNCTION update_updated_at_column()
-- RETURNS TRIGGER AS $$
-- BEGIN
--    NEW.updated_at = NOW();
--    RETURN NEW;
-- END;
-- $$ language 'plpgsql';

-- 1. employee_leave_requests Table
CREATE TABLE employee_leave_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    manager_comment TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_leave_requests_employee FOREIGN KEY(employee_id) REFERENCES users(id) ON DELETE RESTRICT
);
CREATE INDEX idx_leave_requests_employee_id ON employee_leave_requests(employee_id);
CREATE INDEX idx_leave_requests_status ON employee_leave_requests(status);
CREATE INDEX idx_leave_requests_leave_type ON employee_leave_requests(leave_type);
CREATE TRIGGER update_employee_leave_requests_updated_at
BEFORE UPDATE ON employee_leave_requests
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 2. employee_attendance Table
CREATE TABLE employee_attendance (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    login_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    logout_time TIMESTAMP WITHOUT TIME ZONE,
    work_date DATE NOT NULL,
    total_hours DOUBLE PRECISION,
    is_regularized BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendance_employee FOREIGN KEY(employee_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT uq_attendance_employee_work_date UNIQUE (employee_id, work_date)
);
CREATE INDEX idx_attendance_employee_id ON employee_attendance(employee_id);
CREATE INDEX idx_attendance_work_date ON employee_attendance(work_date);
CREATE TRIGGER update_employee_attendance_updated_at
BEFORE UPDATE ON employee_attendance
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 3. employee_payslips Table
CREATE TABLE employee_payslips (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    pay_period_start DATE NOT NULL,
    pay_period_end DATE NOT NULL,
    gross_salary DECIMAL(19,2) NOT NULL,
    deductions DECIMAL(19,2) NOT NULL,
    net_salary DECIMAL(19,2) NOT NULL,
    file_url VARCHAR(512),
    generated_date DATE NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payslips_employee FOREIGN KEY(employee_id) REFERENCES users(id) ON DELETE RESTRICT
);
CREATE INDEX idx_payslips_employee_id ON employee_payslips(employee_id);
CREATE INDEX idx_payslips_pay_period ON employee_payslips(pay_period_start, pay_period_end);
CREATE TRIGGER update_employee_payslips_updated_at
BEFORE UPDATE ON employee_payslips
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 4. company_holidays Table
CREATE TABLE company_holidays (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT,
    name VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    description TEXT,
    is_global BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_holidays_company FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE CASCADE -- Cascade if company is deleted
);
CREATE INDEX idx_holidays_company_id ON company_holidays(company_id);
CREATE INDEX idx_holidays_date ON company_holidays(date);
CREATE INDEX idx_holidays_is_global ON company_holidays(is_global);
CREATE TRIGGER update_company_holidays_updated_at
BEFORE UPDATE ON company_holidays
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 5. employee_documents Table
CREATE TABLE employee_documents (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT,
    company_id BIGINT,
    document_type VARCHAR(50) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL, -- Stored name, e.g., UUID based
    file_url VARCHAR(512) NOT NULL,
    uploaded_by_id BIGINT,
    description TEXT,
    is_company_wide BOOLEAN NOT NULL DEFAULT FALSE,
    is_global_policy BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_documents_employee FOREIGN KEY(employee_id) REFERENCES users(id) ON DELETE CASCADE, -- Cascade if user is deleted
    CONSTRAINT fk_documents_company FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE CASCADE, -- Cascade if company is deleted
    CONSTRAINT fk_documents_uploaded_by FOREIGN KEY(uploaded_by_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX idx_documents_employee_id ON employee_documents(employee_id);
CREATE INDEX idx_documents_company_id ON employee_documents(company_id);
CREATE INDEX idx_documents_document_type ON employee_documents(document_type);
CREATE TRIGGER update_employee_documents_updated_at
BEFORE UPDATE ON employee_documents
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 6. employee_work_status_reports Table
CREATE TABLE employee_work_status_reports (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    report_date DATE NOT NULL,
    tasks_completed TEXT NOT NULL,
    tasks_pending TEXT NOT NULL,
    blockers TEXT,
    submitted_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_work_reports_employee FOREIGN KEY(employee_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_work_reports_employee_id ON employee_work_status_reports(employee_id);
CREATE INDEX idx_work_reports_report_date ON employee_work_status_reports(report_date);
CREATE TRIGGER update_employee_work_status_reports_updated_at
BEFORE UPDATE ON employee_work_status_reports
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 7. employee_concerns Table
CREATE TABLE employee_concerns (
    id BIGSERIAL PRIMARY KEY,
    raised_by_id BIGINT NOT NULL,
    raised_against_employee_id BIGINT,
    raised_against_lead_id BIGINT,
    raised_against_manager_id BIGINT,
    concern_text TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    category VARCHAR(100),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_concerns_raised_by FOREIGN KEY(raised_by_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_concerns_against_employee FOREIGN KEY(raised_against_employee_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_concerns_against_lead FOREIGN KEY(raised_against_lead_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_concerns_against_manager FOREIGN KEY(raised_against_manager_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX idx_concerns_raised_by_id ON employee_concerns(raised_by_id);
CREATE INDEX idx_concerns_status ON employee_concerns(status);
CREATE INDEX idx_concerns_category ON employee_concerns(category);
CREATE TRIGGER update_employee_concerns_updated_at
BEFORE UPDATE ON employee_concerns
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 8. employee_hikes Table
CREATE TABLE employee_hikes (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    hike_percentage DECIMAL(5,2),
    hike_amount DECIMAL(19,2),
    old_salary DECIMAL(19,2) NOT NULL,
    new_salary DECIMAL(19,2) NOT NULL,
    effective_date DATE NOT NULL,
    promotion_title VARCHAR(255),
    hike_letter_document_url VARCHAR(512),
    comments TEXT,
    processed_by_id BIGINT NOT NULL,
    processed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hikes_employee FOREIGN KEY(employee_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_hikes_processed_by FOREIGN KEY(processed_by_id) REFERENCES users(id) ON DELETE RESTRICT
);
CREATE INDEX idx_hikes_employee_id ON employee_hikes(employee_id);
CREATE INDEX idx_hikes_effective_date ON employee_hikes(effective_date);
CREATE TRIGGER update_employee_hikes_updated_at
BEFORE UPDATE ON employee_hikes
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 9. employee_tasks Table
CREATE TABLE employee_tasks (
    id BIGSERIAL PRIMARY KEY,
    assigned_to_id BIGINT NOT NULL,
    assigned_by_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    deadline TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'TODO',
    priority INTEGER DEFAULT 0,
    related_project VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_tasks_assigned_to FOREIGN KEY(assigned_to_id) REFERENCES users(id) ON DELETE CASCADE, -- Cascade if assignee is deleted
    CONSTRAINT fk_tasks_assigned_by FOREIGN KEY(assigned_by_id) REFERENCES users(id) ON DELETE SET NULL -- Set null if assigner is deleted
);
CREATE INDEX idx_tasks_assigned_to_id ON employee_tasks(assigned_to_id);
CREATE INDEX idx_tasks_assigned_by_id ON employee_tasks(assigned_by_id);
CREATE INDEX idx_tasks_status ON employee_tasks(status);
CREATE INDEX idx_tasks_deadline ON employee_tasks(deadline);
CREATE TRIGGER update_employee_tasks_updated_at
BEFORE UPDATE ON employee_tasks
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 10. employee_regularization_requests Table
CREATE TABLE employee_regularization_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    attendance_record_id BIGINT UNIQUE, -- A regularization request is unique for an attendance record
    request_date DATE NOT NULL,
    reason_type VARCHAR(50) NOT NULL,
    custom_reason TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    approver_comment TEXT,
    approved_by_id BIGINT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reg_requests_employee FOREIGN KEY(employee_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reg_requests_attendance FOREIGN KEY(attendance_record_id) REFERENCES employee_attendance(id) ON DELETE CASCADE, -- If attendance record is deleted, this should also go
    CONSTRAINT fk_reg_requests_approved_by FOREIGN KEY(approved_by_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX idx_reg_requests_employee_id ON employee_regularization_requests(employee_id);
CREATE INDEX idx_reg_requests_status ON employee_regularization_requests(status);
CREATE INDEX idx_reg_requests_reason_type ON employee_regularization_requests(reason_type);
CREATE TRIGGER update_employee_regularization_requests_updated_at
BEFORE UPDATE ON employee_regularization_requests
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
