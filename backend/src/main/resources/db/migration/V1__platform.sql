CREATE TABLE documents (
 id UUID PRIMARY KEY, tenant VARCHAR(100) NOT NULL, owner_id VARCHAR(200) NOT NULL,
 title VARCHAR(200) NOT NULL, body TEXT NOT NULL, visibility VARCHAR(20) NOT NULL CHECK (visibility IN ('PRIVATE','TENANT')),
 version INT NOT NULL DEFAULT 1, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX documents_tenant ON documents(tenant,owner_id);
CREATE TABLE chunks (
 id UUID PRIMARY KEY, document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
 tenant VARCHAR(100) NOT NULL, ordinal INT NOT NULL, content TEXT NOT NULL, embedding vector(64) NOT NULL,
 terms TSVECTOR GENERATED ALWAYS AS (to_tsvector('english',content)) STORED,
 UNIQUE(document_id,ordinal)
);
CREATE INDEX chunks_tenant ON chunks(tenant);
CREATE INDEX chunks_terms ON chunks USING gin(terms);
CREATE TABLE workflows (
 id UUID PRIMARY KEY, tenant VARCHAR(100) NOT NULL, owner_id VARCHAR(200) NOT NULL,
 kind VARCHAR(20) NOT NULL CHECK (kind IN ('KNOWLEDGE','CREATE_CASE')), goal VARCHAR(2000) NOT NULL,
 action_json TEXT NOT NULL, request_hash VARCHAR(64) NOT NULL, idempotency_key VARCHAR(100) NOT NULL,
 state VARCHAR(30) NOT NULL, stage INT NOT NULL DEFAULT 0, version BIGINT NOT NULL DEFAULT 0,
 context_json TEXT NOT NULL DEFAULT '[]', result_json TEXT NOT NULL DEFAULT '{}', failure VARCHAR(100),
 attempts INT NOT NULL DEFAULT 0, next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), deadline TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), UNIQUE(tenant,owner_id,idempotency_key)
);
CREATE INDEX workflows_queue ON workflows(state,next_attempt_at,created_at);
CREATE TABLE tasks (
 id UUID PRIMARY KEY, workflow_id UUID NOT NULL REFERENCES workflows(id), tenant VARCHAR(100) NOT NULL,
 parent_id UUID, source_agent VARCHAR(100) NOT NULL, agent VARCHAR(100) NOT NULL, agent_version VARCHAR(20) NOT NULL,
 schema_version INT NOT NULL DEFAULT 1, ordinal INT NOT NULL, dependency_ordinal INT,
 state VARCHAR(30) NOT NULL, attempt INT NOT NULL DEFAULT 0, deadline TIMESTAMPTZ NOT NULL,
 correlation_id UUID NOT NULL, output_json TEXT NOT NULL DEFAULT '{}', duration_ms BIGINT NOT NULL DEFAULT 0,
 UNIQUE(workflow_id,ordinal)
);
CREATE TABLE approvals (
 id UUID PRIMARY KEY, workflow_id UUID NOT NULL UNIQUE REFERENCES workflows(id), tenant VARCHAR(100) NOT NULL,
 action_hash VARCHAR(64) NOT NULL, action_json TEXT NOT NULL, state VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 requested_by VARCHAR(200) NOT NULL, decided_by VARCHAR(200), expires_at TIMESTAMPTZ NOT NULL,
 decided_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE tool_invocations (
 id UUID PRIMARY KEY, tenant VARCHAR(100) NOT NULL, workflow_id UUID NOT NULL REFERENCES workflows(id),
 tool VARCHAR(100) NOT NULL, action_hash VARCHAR(64) NOT NULL, state VARCHAR(20) NOT NULL,
 result_json TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), UNIQUE(tenant,workflow_id,tool)
);
CREATE TABLE cases (
 id UUID PRIMARY KEY, tenant VARCHAR(100) NOT NULL, workflow_id UUID NOT NULL UNIQUE REFERENCES workflows(id),
 title VARCHAR(200) NOT NULL, description VARCHAR(2000) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE memories (
 id UUID PRIMARY KEY, tenant VARCHAR(100) NOT NULL, owner_id VARCHAR(200) NOT NULL,
 fact VARCHAR(2000) NOT NULL, source_document UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), expires_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE audit_events (
 sequence BIGSERIAL PRIMARY KEY, tenant VARCHAR(100) NOT NULL, actor VARCHAR(200) NOT NULL,
 workflow_id UUID, event_type VARCHAR(100) NOT NULL, detail VARCHAR(500) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX audit_tenant ON audit_events(tenant,sequence);
CREATE TABLE usage_records (
 id UUID PRIMARY KEY, tenant VARCHAR(100) NOT NULL, workflow_id UUID NOT NULL REFERENCES workflows(id),
 provider VARCHAR(40) NOT NULL, model VARCHAR(100) NOT NULL, prompt_version VARCHAR(30) NOT NULL,
 input_tokens INT NOT NULL, output_tokens INT NOT NULL, estimated_usd NUMERIC(14,8) NOT NULL,
 duration_ms BIGINT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE evaluation_runs (
 id UUID PRIMARY KEY, tenant VARCHAR(100) NOT NULL, actor VARCHAR(200) NOT NULL,
 dataset_version VARCHAR(30) NOT NULL, result_json TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
