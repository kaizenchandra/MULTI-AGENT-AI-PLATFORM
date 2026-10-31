# Architecture and implementation boundaries

## Implementation plan

1. Foundation: Java 21, Spring Boot, PostgreSQL/pgvector, Flyway, OIDC/local auth, versioned contracts.
2. Runtime: code-reviewed registry, deterministic bounded planner, persisted task envelopes and validated state transitions.
3. Orchestration: supervisor ticks one dependency-ready stage per transaction, verified final evidence.
4. Tools: allowlists, local write ledger, separate-human exact-action approval and cancellation.
5. Knowledge: text ingestion, chunks, full text + lexical vectors, private/tenant visibility, source-bound memory and revocation.
6. Reliability/security: row locks, crash rollback, retries, deadlines, tenant predicates, request and execution quotas, adversarial tests.
7. Interface/operations: React console, usage, audit, trace continuity, metrics and alerts.
8. Delivery: policy/workflow evaluation, local containers, browser/restart tests, deployment foundation and readiness gaps.

These milestones describe the implemented sequence; not all requested enterprise extensions are present. The report is authoritative about verification and gaps.

## System context

```mermaid
flowchart LR
 U[Enterprise user] --> C[Agent operations console]
 H[Independent human approver] --> C
 C --> P[Kaizen platform]
 I[OIDC identity provider] -->|signed access tokens and JWKS| P
 D[Authorized enterprise text] --> P
 P -->|optional authorized inference| O[OpenAI API]
 P --> L[Local case store]
 P --> M[Telemetry collector]
```

## Physical containers and internal components

```mermaid
flowchart TB
 subgraph Browser
 UI[React console]
 end
 subgraph ControlPlane[Single Spring Boot deployment / replicable]
 API[Versioned REST API] --> ID[JWT identity / scopes / tenant boundary]
 ID --> WS[Workflow and approval services]
 WS --> REG[Agent registry / deterministic plan validator]
 WS --> DB[(PostgreSQL + pgvector)]
 WORK[Scheduled supervisor] --> REG
 WORK --> RESEARCH[Research and context]
 WORK --> ANALYSIS[Model gateway]
 WORK --> VERIFY[Verifier and synthesis]
 WORK --> TOOLS[Secure tool gateway]
 RESEARCH --> DB
 VERIFY --> DB
 TOOLS --> DB
 ANALYSIS --> EX[Extractive strategy]
 ANALYSIS --> OA[Fixed-destination OpenAI transport]
 end
 UI -->|same-origin nginx proxy| API
 WORK --> DB
 OA --> EXT[OpenAI optional]
```

The frontend boundary separates browser assets from credentials and execution. PostgreSQL is separate because state, source access, idempotency, and approvals must survive application restarts. Execution shares the application deployment to avoid an unnecessary broker/service boundary. Replicas coordinate using row locks; each replica has one scheduled worker. Provider egress is an explicit trust boundary. There is no Kafka, Redis, Temporal, S3, MCP, or external A2A service in this version.

## Knowledge workflow sequence

```mermaid
sequenceDiagram
 actor User
 participant API
 participant DB as PostgreSQL
 participant Supervisor
 participant Planner
 participant Research
 participant Analysis
 participant Verifier
 participant Synthesis
 User->>API: goal + idempotency key + access token
 API->>API: validate identity, scopes, quota, typed request
 API->>Planner: compile allowlisted Plan.v1
 API->>DB: persist workflow and TaskEnvelope.v1 metadata
 loop bounded dependency-ready stages, separate transactions
 Supervisor->>DB: SELECT ... FOR UPDATE SKIP LOCKED
 Supervisor->>Planner: confirm plan (stage 0)
 Supervisor->>Research: authorized hybrid retrieval (stage 1)
 Research->>DB: tenant / owner / visibility / expiry filtering
 Supervisor->>Analysis: bounded context / extractive or JSON provider (stage 2)
 Supervisor->>Verifier: exact citation and access checks (stage 3)
 Supervisor->>Synthesis: publish only verified quotes (stage 4)
 Supervisor->>DB: commit output, audit, task and workflow version
 end
 User->>API: poll status and task metadata
 API->>DB: recheck source access before returning result
 API-->>User: evidence claims and uncertainty, or revocation error
```

The planner is deterministic application code, not an autonomous model. All specialist calls are synchronous, typed handoffs inside the process. Task metadata includes IDs, workflow/parent, source/destination role, tenant, schema version, deadline, attempt and correlation ID; authorization is inherited only from a validated workflow and capability allowlist. Payloads are stored as workflow context and task outputs. No unrestricted agent-to-agent entrypoint exists.

## Tool and approval sequence

```mermaid
sequenceDiagram
 actor Requester
 actor Approver
 participant API
 participant Supervisor
 participant Gateway
 participant DB
 Requester->>API: CREATE_CASE + exact Action.v1
 API->>DB: store immutable proposed action
 Supervisor->>DB: request approval with SHA-256 action fingerprint
 Supervisor->>DB: WAITING, commit
 Approver->>API: approve exact hash
 API->>API: verify approval scope and distinct subject
 API->>DB: lock workflow then approval; validate state/expiry/hash
 API->>DB: APPROVED + RUNNING, commit
 Supervisor->>Gateway: case.create (allowlisted execution role)
 Gateway->>DB: lock owner-scoped workflow; check exact unexpired approval
 Gateway->>DB: case + invocation result + audit + completion in transaction
 Note over DB: All effects commit together or all roll back
 Supervisor-->>Requester: case ID available through status API
```

External integrations must not reuse the local transaction claim. Remote actions require destination idempotency plus reconciliation, or an outbox and a state machine for ambiguous outcomes. Automatic blind retries of non-idempotent remote effects are prohibited. Remote tools are not implemented.

## Workflow state machine

```mermaid
stateDiagram-v2
 [*] --> CREATED
 CREATED --> RUNNING: validated fixed plan
 RUNNING --> WAITING: approval requested
 WAITING --> RUNNING: valid independent approval
 WAITING --> REJECTED: rejection
 RUNNING --> COMPLETED: all stages verified
 CREATED --> CANCELLED
 RUNNING --> CANCELLED
 WAITING --> CANCELLED
 CREATED --> TIMED_OUT
 RUNNING --> TIMED_OUT
 WAITING --> TIMED_OUT
 CREATED --> FAILED
 RUNNING --> FAILED
 WAITING --> FAILED
 COMPLETED --> [*]
 REJECTED --> [*]
 CANCELLED --> [*]
 TIMED_OUT --> [*]
 FAILED --> [*]
```

Task CREATED → RUNNING → COMPLETED commits as one bounded stage; approval tasks commit WAITING. Failure handling writes FAILED, retry scheduling or cancellation in a separate version-checked transaction after the stage rolls back. There are no hidden in-memory completion dependencies.

## Data flow and provenance

```mermaid
flowchart LR
 Source[Authorized plain text] --> Ingest[Validate title, text, visibility and size]
 Ingest --> Doc[Document ID / owner / tenant / version / retention]
 Doc --> Chunk[Paragraph chunks with overlap]
 Chunk --> FTS[Postgres full-text index]
 Chunk --> Vec[hash64-v1 normalized lexical vector]
 Query[User goal] --> Filter[Tenant / visibility / expiry filter]
 FTS --> Filter
 Vec --> Filter
 Filter --> Evidence[At most 6 provenance-rich chunks]
 Evidence --> Context[Bounded untrusted-data context]
 Context --> Claims[Structured quote claims]
 Claims --> Check[Exact source membership and current access]
 Check --> Result[Verified quotes and explicit uncertainty]
```

Vectors are deterministic lexical features; search requires a full-text match and uses cosine similarity in ranking. This is not dense semantic retrieval. Text is chunked at paragraph boundaries, with 1,000-character windows and 100-character overlap for long paragraphs. Document update currently means delete/re-ingest with a new ID; in-place version history, rich formats, learned embeddings and reranking remain gaps.

## Trust boundaries

```mermaid
flowchart TB
 subgraph Untrusted[Untrusted inputs]
 Browser[Browser / user goal]
 Docs[Document and tool text]
 Model[Provider JSON]
 end
 subgraph App[Application enforcement boundary]
 Auth[JWT signature + issuer + aud + tenant + scopes]
 Plan[Allowlisted workflow and delegation validator]
 Budget[Request / context / cost / step / time bounds]
 Approve[Exact action / separate human / expiry]
 Verify[Schema + citation + source access verifier]
 Tool[Typed tool gateway, no arbitrary network or shell]
 end
 subgraph Data[Restricted storage boundary]
 DB[(Owner and tenant predicates / composite tenant foreign keys)]
 Ledger[Case and idempotency ledger]
 end
 Browser --> Auth --> Plan --> Budget
 Docs --> Budget --> Verify
 Model --> Verify
 Plan --> Approve --> Tool --> Ledger
 Verify --> DB
 Auth --> DB
```

The database is trusted infrastructure. Application tenant predicates and composite FKs are implemented; PostgreSQL RLS and a separate queue service identity are not. Local mode has exactly one fixed tenant and two explicitly configured tokens. Production claims are never sourced from arbitrary client headers.

## Database relationships

```mermaid
erDiagram
 WORKFLOWS ||--o{ TASKS : decomposes
 WORKFLOWS ||--o| APPROVALS : pauses_for
 WORKFLOWS ||--o{ TOOL_INVOCATIONS : records
 WORKFLOWS ||--o| CASES : creates
 WORKFLOWS ||--o{ USAGE_RECORDS : meters
 WORKFLOWS ||--o{ AUDIT_EVENTS : correlates
 DOCUMENTS ||--o{ CHUNKS : indexes
 DOCUMENTS ||--o{ MEMORIES : grounds
 WORKFLOWS { uuid id PK string tenant string owner_id string state int stage bigint version string trace_id }
 TASKS { uuid id PK uuid workflow_id FK uuid parent_id string tenant string agent int ordinal string state }
 APPROVALS { uuid id PK uuid workflow_id FK string tenant string action_hash string action_json string state timestamp expires_at }
 TOOL_INVOCATIONS { uuid id PK uuid workflow_id FK string tool string action_hash string result_json }
 CASES { uuid id PK uuid workflow_id FK string tenant string title }
 DOCUMENTS { uuid id PK string tenant string owner_id string visibility int version timestamp expires_at }
 CHUNKS { uuid id PK uuid document_id FK string tenant vector embedding tsvector terms }
 MEMORIES { uuid id PK uuid source_document FK string tenant string owner_id timestamp expires_at }
 USAGE_RECORDS { uuid id PK uuid workflow_id FK int input_tokens int output_tokens decimal estimated_usd }
 AUDIT_EVENTS { bigint sequence PK string tenant string actor uuid workflow_id string event_type }
 EVALUATION_RUNS { uuid id PK string tenant string actor string dataset_version string result_json }
 API_RATE_LIMITS { string tenant PK string subject PK timestamp window_start int requests }
```

Tenant/user/role/policy identities come from OIDC claims and application policy. Agent/workflow/tool definitions are code-versioned, not database-managed resources. Those requested management entities/APIs remain an extension rather than empty tables.

## Deployment architecture

```mermaid
flowchart TB
 User --> TLS[Operator-managed TLS ingress]
 subgraph Cluster[Existing approved Kubernetes cluster]
 TLS --> Web[Console replicas / non-root nginx]
 Web --> API[Spring Boot replicas / one bounded worker each]
 API --> Collector[OTLP collector]
 Prom[Authenticated Prometheus scrape] --> API
 Prom --> Grafana[Grafana dashboards and alerts]
 end
 API -->|restricted TCP / TLS| RDS[(Private encrypted PostgreSQL + pgvector)]
 API -->|HTTPS / approved egress| IDP[OIDC issuer and JWKS]
 API -. optional approved egress .-> Provider[OpenAI]
 RDS --> Backup[Managed backups / restore exercises]
```

Kubernetes manifests are a fail-closed foundation; ingress, collector installation, monitoring credentials, private networking, workload secrets and endpoint allowlists are environment inputs. Terraform provisions only RDS in supplied private subnets and security groups, using a managed master secret. It does not deploy EKS or provision resources automatically.

## Operational limits

At most 20 active and 200 new workflows per tenant/hour; 300 authenticated API requests per subject/minute; 1 MiB HTTP body; 1,000 retained documents per tenant; 6 evidence chunks; 6 maximum plan steps; 10-minute workflow/approval deadline. Worker stages have a 25-second transaction timeout, database statements 20 seconds, lock waits 2 seconds, provider end-to-end response 15 seconds and response limit 64 KiB. Provider transient errors retry at 2/4 seconds with at most three attempts. No autonomous repair loops or cross-provider fallback occur. One scheduled worker per replica is the bulkhead; HTTP/database pools and pod resources are bounded. There is no SLO-derived autoscaler yet.

Framework references checked during implementation: [Spring JWT validation](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html), [Spring Boot tracing](https://docs.spring.io/spring-boot/3.5/reference/actuator/tracing.html), [PostgreSQL SKIP LOCKED](https://www.postgresql.org/docs/17/sql-select.html), [OpenAI Chat Completions contract](https://developers.openai.com/api/reference/resources/chat/subresources/completions/methods/create). Live tests, not these references alone, determine integration readiness.
