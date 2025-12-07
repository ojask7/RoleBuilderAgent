CREATE TABLE IF NOT EXISTS security_group (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS application_service (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255),
    owner VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS business_application (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255),
    compliance_tier VARCHAR(20)
);
