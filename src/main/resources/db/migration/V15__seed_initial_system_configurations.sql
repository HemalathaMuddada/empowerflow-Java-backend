-- Seed initial system configuration parameters
-- The updated_by_id is set by selecting the ID of the 'superadmin' user.
-- This assumes the 'superadmin' user was created in V1__init_core_schema.sql and is available.
-- If 'superadmin' might not exist or its ID is uncertain, this part might need adjustment
-- (e.g., hardcoding a known system user ID or setting to NULL if the foreign key allows).

INSERT INTO system_configurations (config_key, config_value, description, value_type, updated_at, updated_by_id)
VALUES
(
    'MINIMUM_WORK_HOURS_PER_DAY',
    '8.0',
    'Standard minimum work hours expected for a full day. Used in attendance calculations and alerts (e.g., underwork, early logout).',
    'NUMBER',
    CURRENT_TIMESTAMP,
    (SELECT id FROM users WHERE username = 'superadmin' LIMIT 1)
),
(
    'LATE_LOGIN_THRESHOLD_TIME',
    '09:30:00',
    'Default time (HH:MM:SS format) after which a login is considered late. Used for attendance alerts.',
    'TIME',
    CURRENT_TIMESTAMP,
    (SELECT id FROM users WHERE username = 'superadmin' LIMIT 1)
),
(
    'DEFAULT_TASK_PRIORITY',
    '0',
    'Default priority for newly created tasks (e.g., 0-Normal, 1-Medium, 2-High).',
    'NUMBER',
    CURRENT_TIMESTAMP,
    (SELECT id FROM users WHERE username = 'superadmin' LIMIT 1)
),
(
    'ALLOW_EMPLOYEE_SELF_REGISTRATION',
    'false',
    'Controls if new users can self-register into the system. If false, only authorized personnel (HR/Admin) can add new users.',
    'BOOLEAN',
    CURRENT_TIMESTAMP,
    (SELECT id FROM users WHERE username = 'superadmin' LIMIT 1)
),
(
    'MAX_CONCURRENT_SESSIONS_PER_USER',
    '2',
    'Maximum number of concurrent active login sessions a single user account can have. Set to 0 or negative for unlimited.',
    'NUMBER',
    CURRENT_TIMESTAMP,
    (SELECT id FROM users WHERE username = 'superadmin' LIMIT 1)
),
(
    'PASSWORD_EXPIRY_DAYS',
    '90',
    'Number of days after which user passwords expire and must be changed. Set to 0 for no expiry.',
    'NUMBER',
    CURRENT_TIMESTAMP,
    (SELECT id FROM users WHERE username = 'superadmin' LIMIT 1)
),
(
    'SYSTEM_WIDE_ANNOUNCEMENT',
    '',
    'A short announcement message to be displayed globally, e.g., on dashboards. Leave empty for no announcement.',
    'STRING',
    CURRENT_TIMESTAMP,
    (SELECT id FROM users WHERE username = 'superadmin' LIMIT 1)
);

-- Note: The LATE_LOGIN_THRESHOLD_TIME is stored as '09:30:00'.
-- The service layer should parse this appropriately (e.g., into LocalTime).
-- The database column type is TEXT, so the format here is for storage and later parsing.
-- If the value_type 'TIME' implies a specific database time type, the INSERT format might need adjustment
-- but for TEXT storage, this is fine. The entity's valueType field guides parsing.
