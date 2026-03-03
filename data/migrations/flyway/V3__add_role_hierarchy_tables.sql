-- V3: Access Bundle & Business Role hierarchy tables

CREATE TABLE IF NOT EXISTS entitlement (
    id              BIGSERIAL PRIMARY KEY,
    source_sg_name  VARCHAR(255) NOT NULL,
    source_system   VARCHAR(50),
    application_service VARCHAR(255),
    business_app    VARCHAR(255),
    description     TEXT,
    owner           VARCHAR(255),
    owner_source    VARCHAR(50),
    discovered_from VARCHAR(50),
    status          VARCHAR(30) NOT NULL DEFAULT 'DISCOVERED',
    confidence      NUMERIC(5,4) DEFAULT 0.0,
    reasoning_trace TEXT,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_entitlement_sg_name ON entitlement (source_sg_name);

CREATE TABLE IF NOT EXISTS it_role (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    application_id  VARCHAR(255),
    owner           VARCHAR(255),
    status          VARCHAR(30) NOT NULL DEFAULT 'SUGGESTED',
    mining_source   VARCHAR(100),
    confidence      NUMERIC(5,4) DEFAULT 0.0,
    reasoning_trace TEXT,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_it_role_name ON it_role (name);

CREATE TABLE IF NOT EXISTS business_role (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    job_function    VARCHAR(255),
    department      VARCHAR(255),
    region          VARCHAR(100),
    owner           VARCHAR(255),
    description     TEXT,
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    confidence      NUMERIC(5,4) DEFAULT 0.0,
    reasoning_trace TEXT,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_business_role_name ON business_role (name);

CREATE TABLE IF NOT EXISTS access_bundle (
    id                  BIGSERIAL PRIMARY KEY,
    business_role_id    BIGINT NOT NULL UNIQUE REFERENCES business_role(id),
    version             INT NOT NULL DEFAULT 1,
    status              VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    approved_by         VARCHAR(255),
    approved_at         TIMESTAMP WITH TIME ZONE,
    kc27_status         VARCHAR(30) DEFAULT 'NOT_ASSESSED',
    kc27_assessed_at    TIMESTAMP WITH TIME ZONE,
    kc27_gaps           TEXT,
    evidence_hash       VARCHAR(128),
    total_entitlements  INT DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Join table: IT Role <-> Entitlement (many-to-many)
CREATE TABLE IF NOT EXISTS it_role_entitlement (
    it_role_id      BIGINT NOT NULL REFERENCES it_role(id),
    entitlement_id  BIGINT NOT NULL REFERENCES entitlement(id),
    PRIMARY KEY (it_role_id, entitlement_id)
);

-- Join table: Business Role <-> IT Role (many-to-many)
CREATE TABLE IF NOT EXISTS business_role_it_role (
    business_role_id BIGINT NOT NULL REFERENCES business_role(id),
    it_role_id       BIGINT NOT NULL REFERENCES it_role(id),
    PRIMARY KEY (business_role_id, it_role_id)
);

-- User to Business Role assignments
CREATE TABLE IF NOT EXISTS user_business_role (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             VARCHAR(100) NOT NULL,
    business_role_id    BIGINT NOT NULL REFERENCES business_role(id),
    assigned_at         TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    assigned_by         VARCHAR(255),
    UNIQUE (user_id, business_role_id)
);
