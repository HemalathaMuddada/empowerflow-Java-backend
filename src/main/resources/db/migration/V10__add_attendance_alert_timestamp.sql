-- Add underwork_alert_sent_at column to employee_attendance table
ALTER TABLE employee_attendance
ADD COLUMN underwork_alert_sent_at TIMESTAMP WITHOUT TIME ZONE NULL;

COMMENT ON COLUMN employee_attendance.underwork_alert_sent_at IS 'Timestamp when an underwork alert email was sent for this attendance record.';
