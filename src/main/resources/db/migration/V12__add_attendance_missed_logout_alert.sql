-- Add missed_logout_alert_sent_at column to employee_attendance table
ALTER TABLE employee_attendance
ADD COLUMN missed_logout_alert_sent_at TIMESTAMP WITHOUT TIME ZONE NULL;

COMMENT ON COLUMN employee_attendance.missed_logout_alert_sent_at IS 'Timestamp when a missed logout alert email was sent for this attendance record.';
