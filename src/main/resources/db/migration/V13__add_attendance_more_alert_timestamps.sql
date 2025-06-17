-- Add columns for early logout and late login alert timestamps to employee_attendance table
ALTER TABLE employee_attendance
ADD COLUMN early_logout_alert_sent_at TIMESTAMP WITHOUT TIME ZONE NULL,
ADD COLUMN late_login_alert_sent_at TIMESTAMP WITHOUT TIME ZONE NULL;

COMMENT ON COLUMN employee_attendance.early_logout_alert_sent_at IS 'Timestamp when an early logout alert was sent for this record';
COMMENT ON COLUMN employee_attendance.late_login_alert_sent_at IS 'Timestamp when a late login alert was sent for this record';
