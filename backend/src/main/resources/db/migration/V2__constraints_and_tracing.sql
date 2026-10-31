ALTER TABLE workflows ADD COLUMN trace_id VARCHAR(32);
ALTER TABLE workflows ADD COLUMN parent_span_id VARCHAR(16);
ALTER TABLE workflows ADD COLUMN trace_sampled BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE workflows ADD CONSTRAINT workflow_states CHECK (state IN ('CREATED','RUNNING','WAITING','COMPLETED','FAILED','TIMED_OUT','CANCELLED','REJECTED'));
ALTER TABLE workflows ADD CONSTRAINT workflow_tenant_key UNIQUE(id,tenant);
ALTER TABLE documents ADD CONSTRAINT document_tenant_key UNIQUE(id,tenant);
ALTER TABLE tasks ADD FOREIGN KEY(workflow_id,tenant) REFERENCES workflows(id,tenant);
ALTER TABLE approvals ADD FOREIGN KEY(workflow_id,tenant) REFERENCES workflows(id,tenant);
ALTER TABLE tool_invocations ADD FOREIGN KEY(workflow_id,tenant) REFERENCES workflows(id,tenant);
ALTER TABLE cases ADD FOREIGN KEY(workflow_id,tenant) REFERENCES workflows(id,tenant);
ALTER TABLE usage_records ADD FOREIGN KEY(workflow_id,tenant) REFERENCES workflows(id,tenant);
ALTER TABLE chunks ADD FOREIGN KEY(document_id,tenant) REFERENCES documents(id,tenant) ON DELETE CASCADE;
ALTER TABLE memories ADD FOREIGN KEY(source_document,tenant) REFERENCES documents(id,tenant) ON DELETE CASCADE;
ALTER TABLE approvals ADD CONSTRAINT approval_states CHECK(state IN ('PENDING','APPROVED','REJECTED','EXPIRED','CANCELLED'));
CREATE TABLE api_rate_limits (
 tenant VARCHAR(100) NOT NULL, subject VARCHAR(200) NOT NULL, window_start TIMESTAMPTZ NOT NULL,
 requests INT NOT NULL CHECK(requests>0), PRIMARY KEY(tenant,subject)
);
