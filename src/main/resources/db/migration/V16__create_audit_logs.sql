CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_username VARCHAR(100) NOT NULL,
    actor_id BIGINT,
    action_type VARCHAR(100) NOT NULL,
    target_entity_type VARCHAR(100),
    target_entity_id VARCHAR(100),
    details TEXT,
    ip_address VARCHAR(50),
    status VARCHAR(20) NOT NULL
);

CREATE INDEX idx_audit_log_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_log_actor_username ON audit_logs(actor_username);
CREATE INDEX idx_audit_log_action_type ON audit_logs(action_type);
CREATE INDEX idx_audit_log_target_entity ON audit_logs(target_entity_type, target_entity_id);

COMMENT ON TABLE audit_logs IS 'Stores audit trail records for important system events.';
COMMENT ON COLUMN audit_logs.id IS 'Unique identifier for the audit log entry.';
COMMENT ON COLUMN audit_logs.timestamp IS 'Timestamp when the event occurred (auto-generated).';
COMMENT ON COLUMN audit_logs.actor_username IS 'Username of the user who performed the action.';
COMMENT ON COLUMN audit_logs.actor_id IS 'ID of the user who performed the action (if applicable).';
COMMENT ON COLUMN audit_logs.action_type IS 'Type of action performed (e.g., USER_LOGIN_SUCCESS, COMPANY_CREATE).';
COMMENT ON COLUMN audit_logs.target_entity_type IS 'Type of the entity that was affected (e.g., User, Company).';
COMMENT ON COLUMN audit_logs.target_entity_id IS 'ID of the entity that was affected.';
COMMENT ON COLUMN audit_logs.details IS 'Additional details about the event (e.g., JSON payload of changes).';
COMMENT ON COLUMN audit_logs.ip_address IS 'IP address from which the action was performed.';
COMMENT ON COLUMN audit_logs.status IS 'Outcome of the action (e.g., SUCCESS, FAILURE).';
