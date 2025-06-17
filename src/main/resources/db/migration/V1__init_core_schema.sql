-- DDL for companies table
CREATE TABLE companies (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE,
  address TEXT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- DDL for roles table
CREATE TABLE roles (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL UNIQUE
);

-- DDL for users table
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,
  username VARCHAR(100) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  date_of_birth DATE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  company_id BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_company FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE SET NULL
);

-- DDL for user_roles join table
CREATE TABLE user_roles (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_role FOREIGN KEY(role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Add Indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_roles_name ON roles(name);
CREATE INDEX idx_companies_name ON companies(name);

-- Seed Data for roles table
INSERT INTO roles (name) VALUES ('ROLE_EMPLOYEE');
INSERT INTO roles (name) VALUES ('ROLE_LEAD');
INSERT INTO roles (name) VALUES ('ROLE_HR');
INSERT INTO roles (name) VALUES ('ROLE_MANAGER');
INSERT INTO roles (name) VALUES ('ROLE_SUPER_ADMIN');

-- Seed Data for a Default Company (Optional but useful for SUPER_ADMIN)
INSERT INTO companies (name, address) VALUES ('Default Global Corp', 'Global Headquarters');

-- Seed Data for a ROLE_SUPER_ADMIN User
-- Password for 'superadmin' is '$2a$10$0LAFEXAX3SoqW17Y9/N65.Vfts2PU72T2Spmb7PSgB3615zR9q9La'
INSERT INTO users (first_name, last_name, username, email, password, is_active, company_id)
VALUES ('Super', 'Admin', 'superadmin', 'superadmin@example.com', '$2a$10$0LAFEXAX3SoqW17Y9/N65.Vfts2PU72T2Spmb7PSgB3615zR9q9La', TRUE, NULL);
-- company_id is NULL as SUPER_ADMIN might not belong to any specific company initially.
-- Alternatively, it could be linked to 'Default Global Corp' like this:
-- (SELECT id FROM companies WHERE name = 'Default Global Corp')

-- Link ROLE_SUPER_ADMIN to the superadmin user
INSERT INTO user_roles (user_id, role_id) VALUES
((SELECT id FROM users WHERE username = 'superadmin'), (SELECT id FROM roles WHERE name = 'ROLE_SUPER_ADMIN'));

-- Trigger function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
   NEW.updated_at = NOW();
   RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply trigger to companies table
CREATE TRIGGER update_companies_updated_at
BEFORE UPDATE ON companies
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Apply trigger to users table
CREATE TRIGGER update_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
