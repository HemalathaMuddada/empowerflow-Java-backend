CREATE TABLE system_configurations (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value TEXT NOT NULL,
    description TEXT,
    value_type VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    updated_by_id BIGINT,
    CONSTRAINT fk_config_updated_by FOREIGN KEY (updated_by_id) REFERENCES users(id) ON DELETE SET NULL
);

COMMENT ON TABLE system_configurations IS 'Stores system-wide configuration parameters as key-value pairs.';
COMMENT ON COLUMN system_configurations.config_key IS 'Unique key for the configuration parameter (e.g., MINIMUM_WORK_HOURS_PER_DAY).';
COMMENT ON COLUMN system_configurations.config_value IS 'Value of the configuration parameter, stored as a string.';
COMMENT ON COLUMN system_configurations.description IS 'Description of what this configuration parameter does.';
COMMENT ON COLUMN system_configurations.value_type IS 'Hint for the expected data type of the value (e.g., NUMBER, STRING, TIME, BOOLEAN).';
COMMENT ON COLUMN system_configurations.updated_at IS 'Timestamp of the last update.';
COMMENT ON COLUMN system_configurations.updated_by_id IS 'User ID of the admin who last updated this configuration.';
