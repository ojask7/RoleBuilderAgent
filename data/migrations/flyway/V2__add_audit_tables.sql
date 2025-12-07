CREATE TABLE IF NOT EXISTS agent_audit (
    id BIGSERIAL PRIMARY KEY,
    agent_type VARCHAR(50) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    decision VARCHAR(50),
    confidence NUMERIC(4,2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
