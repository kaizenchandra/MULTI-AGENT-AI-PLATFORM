# MASTER PROMPT: End-to-End Production-Grade Enterprise Multi-Agent AI Platform

## 1. Role and Mission

Act as a **Principal AI Architect, Enterprise Solution Architect, Staff Software Engineer, Multi-Agent Systems Architect, LLM Engineer, MLOps Engineer, Cloud Architect, Security Architect, and Platform Engineering Lead** with 15+ years of experience designing complex enterprise systems.

Your mission is to **design, architect, implement, integrate, test, secure, deploy, and document a complete Enterprise Multi-Agent AI Platform** using modern AI engineering principles, enterprise software architecture standards, distributed systems patterns, and production-grade operational practices.

**This is a complete engineering implementation task, not an architecture-only exercise.**

Build a practical platform that enables multiple specialized AI agents to securely collaborate, reason over authorized information, delegate bounded tasks, use approved tools, execute business workflows, recover from failures, and produce accurate, traceable, and verifiable results.

The platform must support enterprise scenarios such as:

- Enterprise research and knowledge assistants.
- Multi-agent document intelligence.
- Business workflow automation.
- AI-powered engineering and operational assistants.
- Enterprise data analysis.
- Customer support and case resolution.
- Controlled enterprise application integration.
- Human-supervised AI automation.

Your primary objective is to produce a **working, maintainable, scalable, secure, observable, testable, and deployable system**, not a tutorial, notebook, prototype, or collection of disconnected demos.

---

## 2. Core Engineering Principles

Follow these principles throughout implementation.

### Software Engineering

- SOLID principles.
- Clean Architecture / Hexagonal Architecture.
- Domain-Driven Design where useful.
- Separation of concerns.
- Dependency inversion.
- DRY, KISS, and YAGNI.
- Composition over inheritance.
- Explicit interfaces and contracts.
- Configuration over hardcoding.
- Secure-by-default design.
- Test-driven development where practical.
- Backward-compatible API and schema evolution.
- Maintainable code and minimal necessary abstractions.

Apply design patterns when justified, including Strategy, Factory, Adapter, Repository, Decorator, State, Command, Mediator, and Saga. Never introduce patterns merely to demonstrate their use.

### AI Engineering

- Model-agnostic architecture.
- Agent specialization with explicit responsibilities.
- Bounded autonomy.
- Context engineering.
- Prompt versioning.
- Structured output and schema validation.
- Grounded generation.
- Tool-use safety.
- Human oversight for sensitive actions.
- Evidence-based evaluation.
- Hallucination detection and mitigation.
- Least-privilege access.
- Traceable agent decisions and actions.
- Responsible AI and privacy-by-design.
- Cost-aware inference.
- Continuous AI quality improvement.

### Distributed Systems

- Idempotency.
- Fault isolation.
- Timeouts.
- Bounded retries and exponential backoff.
- Circuit breakers.
- Bulkheads.
- Concurrency control.
- Durable state.
- Eventual consistency where appropriate.
- Transactional outbox where needed.
- Backpressure.
- Graceful recovery.
- Horizontal scaling.
- Observability.

**Prefer the simplest architecture that satisfies real requirements. Do not distribute components merely because the system contains multiple agents.**

---

## 3. Execution Rules

You are responsible for implementation from beginning to end.

Before implementing:

1. Inspect the repository, existing dependencies, architecture, tests, configuration, and project conventions.
2. Follow all applicable repository instructions.
3. Identify requirements, existing constraints, security boundaries, and dependencies.
4. Define the target architecture and implementation milestones.
5. Make routine engineering decisions independently.
6. Ask clarification questions only when a missing requirement materially affects security, correctness, compatibility, cost, or deployment.
7. Document necessary assumptions and proceed with sensible defaults.

After planning, **immediately begin implementing the platform**.

Do not stop after presenting diagrams, recommendations, pseudocode, architectural proposals, or project skeletons.

Implement a working vertical slice first and expand incrementally.

Do not invent test results, code changes, deployments, or metrics. Distinguish implemented capabilities from proposed or simulated ones.

Do not make external infrastructure changes, spend money, execute destructive operations, or perform sensitive real-world actions without the necessary authorization.

---

## 4. Preferred Enterprise Technology Stack

Use the following as the default for a new project, adapting to existing repository conventions and verified dependency compatibility.

| Component | Preferred technology |
|---|---|
| Core backend | Java 21, Spring Boot |
| Agent runtime | Spring AI or LangChain4j, based on required capabilities |
| Agent orchestration | Explicit orchestration layer, selected for durability and control |
| Specialized AI services | Python only when materially justified |
| Inter-service communication | REST / gRPC as appropriate |
| Async messaging | Apache Kafka |
| Workflow persistence | PostgreSQL |
| Vector storage | PostgreSQL + pgvector |
| Caching | Redis |
| Object storage | Amazon S3 |
| Identity | OAuth 2.0 / OpenID Connect |
| Authorization | RBAC + ABAC where required |
| LLM providers | Amazon Bedrock / OpenAI / Azure OpenAI through adapters |
| Agent-tool integration | MCP when appropriate, with strict security boundaries |
| Agent interoperability | Versioned internal contracts; A2A where interoperability justifies it |
| Workflow engine | Temporal or equivalent when durable execution requirements justify it |
| Frontend | React + TypeScript |
| Containerization | Docker |
| Orchestration | Kubernetes |
| Cloud | AWS, with portable architecture |
| Infrastructure | Terraform |
| CI/CD | GitHub Actions or existing CI platform |
| Observability | OpenTelemetry, Prometheus, Grafana |
| Testing | JUnit 5, Testcontainers, integration tests, AI evaluation framework |

Do not blindly use every technology listed.

Select frameworks based on verified functionality, maturity, compatibility, observability, operational complexity, and maintenance costs.

**Default architecture:** Modular monolith for the control plane, with independently scalable execution workers only when justified.

Avoid introducing an additional orchestration service if the selected runtime already provides suitable durable orchestration.

---

# PART I — MULTI-AGENT SYSTEM ARCHITECTURE

## 5. Overall Architecture

Design an enterprise platform with the following logical components:

1. API Gateway / Application API
2. Identity and Authorization
3. Agent Registry and Configuration
4. Agent Orchestrator / Supervisor
5. Task Planning and Decomposition
6. Agent Execution Runtime
7. Agent Communication and Delegation
8. Tool Registry and Secure Tool Gateway
9. Memory and Context Management
10. Knowledge Retrieval / RAG
11. LLM Gateway and Model Routing
12. Durable Workflow and Task State
13. Human-in-the-Loop Approval
14. Agent Evaluation and Policy Enforcement
15. Audit, Monitoring, and Observability
16. Enterprise Integration Adapters

Keep the logical architecture distinct from physical deployment.

Produce:

- System context diagram.
- Container/component architecture diagram.
- Agent orchestration sequence diagram.
- Tool execution and approval sequence diagram.
- Workflow state diagram.
- Data flow diagram.
- Trust boundary diagram.
- Database ER diagram.
- Deployment architecture diagram.

Use Mermaid or another maintainable diagram format supported by the repository.

Explain why each deployment boundary exists.

---

## 6. Multi-Agent Orchestration Architecture

Implement a **supervisor-based multi-agent architecture** with explicit, bounded task delegation.

Use the following as the initial reference topology:

**Supervisor / Orchestrator Agent**

Responsible for:

- Understanding user intent.
- Selecting an appropriate workflow.
- Coordinating specialized agents.
- Delegating bounded subtasks.
- Tracking progress and results.
- Enforcing execution budgets.
- Handling failures.
- Combining verified outputs.

**Planner Agent**

Responsible for:

- Decomposing complex tasks.
- Identifying dependencies.
- Producing a structured task plan.
- Specifying required agents and tools.
- Defining expected outputs.
- Identifying approval requirements.

**Research / Knowledge Agent**

Responsible for:

- Authorized information retrieval.
- Enterprise knowledge search.
- Optional RAG pipelines.
- Evidence gathering.
- Source attribution.

**Analysis Agent**

Responsible for:

- Structured reasoning over supplied evidence.
- Data analysis.
- Comparing alternatives.
- Generating actionable findings.
- Identifying unresolved uncertainty.

**Execution Agent**

Responsible for:

- Invoking approved tools.
- Interacting with enterprise services.
- Executing explicitly authorized actions.
- Reporting results and failures.
- Respecting authorization and approval limits.

**Verification / Critic Agent**

Responsible for:

- Checking outputs against task requirements.
- Validating structured results.
- Checking evidence and citations.
- Detecting inconsistencies.
- Identifying unsupported assertions.
- Recommending bounded correction or abstention.

**Synthesis Agent**

Responsible for:

- Combining verified task outputs.
- Producing coherent final responses.
- Preserving evidence provenance.
- Communicating uncertainty and limitations.

These are logical roles, not mandatory independent processes.

Use one agent or deterministic application code instead of multiple agents when delegation provides no measurable benefit.

### Supported collaboration patterns

Design extensibility for:

- Supervisor–worker.
- Sequential workflows.
- Parallel fan-out/fan-in.
- Planner–executor.
- Router-based specialization.
- Evaluator–optimizer.
- Human-supervised execution.

Do not implement uncontrolled agent conversations, endless reflection loops, or unrestricted recursive delegation.

Every workflow must have explicit termination conditions.

---

## 7. Agent Abstractions and Contracts

Implement clear agent contracts containing:

- Agent identifier.
- Agent version.
- Responsibility.
- Supported capabilities.
- Input schema.
- Output schema.
- Allowed tools.
- Model policy.
- Authorization scope.
- Execution timeout.
- Token budget.
- Cost budget.
- Maximum iterations.
- Retry policy.
- Failure behavior.

Design appropriate interfaces such as:

- Agent
- AgentRegistry
- AgentExecutor
- AgentOrchestrator
- TaskPlanner
- TaskScheduler
- ToolRegistry
- ToolExecutor
- ModelGateway
- ContextProvider
- MemoryRepository
- WorkflowRepository
- PolicyEnforcer
- ApprovalService
- EvaluationService

These are conceptual contracts. Consolidate them where appropriate rather than producing a large interface hierarchy unnecessarily.

Use typed input/output schemas and versioned contracts.

Do not rely on uncontrolled natural-language messages as the only mechanism for task coordination.

---

# PART II — AGENT EXECUTION AND COMMUNICATION

## 8. Agent Lifecycle Management

Implement an explicit agent execution lifecycle.

Suggested states:

`CREATED → VALIDATING → READY → RUNNING → WAITING → COMPLETED`

Also support terminal or exceptional states:

- FAILED
- TIMED_OUT
- CANCELLED
- REJECTED

Implement:

- Execution identifiers.
- Workflow identifiers.
- Parent/child task relationships.
- State persistence.
- Optimistic concurrency or equivalent controls.
- Retry attempt tracking.
- Execution deadlines.
- Cancellation propagation.
- Structured failure reasons.
- Completion and cleanup.

State transitions must be validated server-side.

Recover durable executions following crashes or process restarts.

Do not equate retrying a failed agent invocation with safely retrying its external tool side effects.

---

## 9. Agent-to-Agent Communication

Implement controlled, contract-driven communication.

Each task message should include:

- Message ID.
- Workflow ID.
- Parent task ID.
- Source and destination agent.
- Tenant and authorized identity context.
- Correlation and trace identifiers.
- Task type.
- Schema version.
- Structured payload.
- Deadline.
- Idempotency key where applicable.
- Delivery and processing status.

Supported communication patterns:

- Synchronous task delegation.
- Asynchronous task dispatch.
- Event-driven completion.
- Parallel task execution.
- Result aggregation.

Choose Kafka or another durable messaging mechanism only where asynchronous delivery and independent scaling justify it.

For Kafka-backed execution, address:

- Partition-key selection.
- Consumer groups.
- Offset commit boundaries.
- Duplicate delivery.
- Idempotent processing.
- Retry topics or bounded retry scheduling.
- Dead-letter handling.
- Poison messages.
- Out-of-order completion.
- Schema compatibility.
- Transactional outbox where appropriate.

Never assume all agents can freely call every other agent.

Enforce delegation policies and prevent privilege escalation.

Treat external A2A agents as untrusted integrations until authenticated, authorized, and contract-validated.

---

## 10. Task Planning and Orchestration

Implement a structured task-planning mechanism.

A task plan should contain:

- Task identifier.
- Goal.
- Preconditions.
- Dependencies.
- Assigned capability or agent.
- Expected output schema.
- Required evidence.
- Tool permissions.
- Deadline.
- Resource budget.
- Approval requirements.
- Completion criteria.

Support dependency-aware execution, including safe parallelism where tasks are independent.

Implement:

- DAG-based task dependencies where appropriate.
- Bounded parallel execution.
- Fan-out/fan-in.
- Timeout propagation.
- Task cancellation.
- Retry and recovery policies.
- Partial result handling.
- Compensation for supported reversible actions.
- Workflow completion and failure reporting.

Do not allow arbitrary LLM-generated plans to execute directly.

Validate every generated plan against a predefined schema, workflow policy, authorization boundary, and resource budget.

Only approved workflow steps may invoke state-changing tools.

---

# PART III — TOOL USE AND ENTERPRISE INTEGRATION

## 11. Enterprise Tool Gateway

Build a centralized **secure tool execution boundary**.

Support:

- Tool registration.
- Tool discovery.
- Tool schema validation.
- Capability-based access control.
- Tool execution authorization.
- Timeouts.
- Concurrency limits.
- Rate limits.
- Idempotency enforcement.
- Audit records.
- Tool result normalization.
- Error classification.
- Policy enforcement.

Implement MCP support when suitable.

### Tool security requirements

- Deny access by default.
- Never expose arbitrary shell execution by default.
- Never allow unrestricted filesystem access.
- Do not allow arbitrary network requests or unrestricted URL fetching.
- Protect against SSRF and internal endpoint access.
- Separate read-only and state-changing capabilities.
- Require human approval for configured high-impact operations.
- Never expose secrets in LLM prompts or tool outputs.
- Use scoped credentials and service identities.
- Treat tool descriptions and tool results as untrusted data.
- Prevent unauthorized tool chaining.

Provide at least two demonstrable tools:

1. A read-only knowledge/data tool.
2. A controlled state-changing tool backed by a safe local test integration.

For the state-changing tool, demonstrate approval, idempotency, audit, and failure recovery.

---

## 12. Human-in-the-Loop Approval

Implement durable approval workflows for high-risk operations.

Examples:

- Financial transactions.
- Modifying production systems.
- Deleting enterprise data.
- Sending externally visible communications.
- Changing access permissions.
- Executing sensitive automated workflows.

Support:

- Approval requests.
- Approver identity verification.
- Approval scopes.
- Approve/reject decisions.
- Expiration.
- Cancellation.
- Execution resume.
- Audit trails.
- Duplicate approval prevention.
- Revalidation before execution.

An approval must be bound to the **exact proposed action and its material parameters**.

If the action changes after approval, require revalidation and, where necessary, renewed approval.

Never execute an action simply because an agent claims that approval was granted.

---

# PART IV — MEMORY, CONTEXT AND ENTERPRISE KNOWLEDGE

## 13. Agent Memory Architecture

Design memory as distinct capabilities.

### Short-term execution context

- Current task instructions.
- Relevant task history.
- Intermediate structured results.
- Workflow execution state.
- Authorized evidence.

### Persistent memory

- Approved durable task facts.
- User or organizational preferences where authorized.
- Reusable workflow knowledge.
- Explicitly retained agent artifacts.

### Knowledge memory

- Enterprise documents.
- Structured business records.
- Retrieved knowledge and citations.

Implement:

- Tenant-scoped memory.
- Access controls.
- Retention policies.
- Explicit provenance.
- Memory versioning where needed.
- Context size management.
- Memory deletion and revocation.
- Safe summarization.
- Protection against memory poisoning.

Do not automatically store every conversation, hidden reasoning trace, or generated assertion as reliable long-term knowledge.

Memory writes should be policy-controlled and traceable.

---

## 14. Enterprise RAG Integration

Provide a reusable RAG capability accessible to authorized agents.

Implement:

1. Document ingestion.
2. Parsing and text extraction.
3. Structure-aware chunking.
4. Embedding generation.
5. Vector indexing.
6. Keyword search.
7. Hybrid retrieval.
8. Authorization-aware metadata filtering.
9. Optional reranking.
10. Context construction.
11. Evidence provenance.
12. Citation verification.

Use PostgreSQL/pgvector initially unless requirements justify a different vector store.

Include document updates, deletion, versioning, and permission revocation.

Treat retrieved documents as data, never as higher-priority instructions.

Do not allow one agent's retrieval capability to bypass another agent's authorization boundaries.

---

## 15. Context Engineering

Implement reusable context-building policies.

Prioritize:

- User intent.
- Application and agent instructions.
- Authorized task information.
- Relevant conversation state.
- Verified memory.
- Retrieved evidence.
- Token budget.
- Source provenance.

Use explicit isolation between trusted instructions and untrusted content.

Support:

- Context truncation.
- Relevance filtering.
- Summarization where suitable.
- Structured agent handoffs.
- Bounded task history.
- Context versioning.
- Prompt template versioning.

Prevent uncontrolled context accumulation across long-running workflows.

---

# PART V — MODEL MANAGEMENT AND AI QUALITY

## 16. Enterprise LLM Gateway

Build a provider-independent LLM gateway.

Support:

- Multiple model providers.
- Configurable model selection.
- Capability-based model routing.
- Model configuration.
- Provider timeout.
- Bounded retries.
- Rate-limit handling.
- Circuit breakers where justified.
- Token accounting.
- Cost estimation.
- Structured outputs.
- Error normalization.
- Provider failover when safe.
- Streaming where appropriate.

Do not assume different models have identical context windows, tool semantics, schema guarantees, pricing, or data-handling policies.

Implement a model capability registry.

Validate tool invocation and structured responses server-side.

Provider switching must preserve security, privacy, and data residency policies.

---

## 17. Prompt Engineering and AI Safety

Use versioned prompts with explicit:

- Agent role.
- Task goal.
- Available evidence.
- Authorized capabilities.
- Tool instructions.
- Output schema.
- Prohibited operations.
- Uncertainty handling.
- Termination conditions.

Do not rely solely on prompt instructions for authorization, security, or action approval.

Enforce these protections in deterministic application code.

Implement defenses against:

- Direct prompt injection.
- Indirect prompt injection.
- Malicious tool output.
- Cross-agent instruction laundering.
- Sensitive data disclosure.
- Excessive agency.
- Unbounded tool use.
- Hallucinated tool results.
- Unsupported citations.
- Malicious memory updates.
- Output format manipulation.

Apply relevant OWASP LLM application security guidance.

Use evidence verification and output validation rather than assuming model self-criticism provides correctness guarantees.

---

## 18. AI Evaluation Framework

Implement systematic evaluation at both agent and workflow levels.

Measure, where applicable:

### Agent quality
- Task completion rate.
- Correct tool selection.
- Tool argument validity.
- Structured output validity.
- Instruction compliance.
- Evidence correctness.
- Hallucination rate.
- Appropriate abstention.

### Multi-agent quality
- End-to-end task success.
- Delegation correctness.
- Workflow completion.
- Inter-agent contract compliance.
- Unnecessary delegation.
- Error recovery.
- Human escalation correctness.
- Final answer quality.

### Operational quality
- Execution latency.
- Tokens per task.
- Cost per completed workflow.
- Tool failure rate.
- Retry rate.
- Resource consumption.
- Approval latency.

### Security quality
- Unauthorized tool attempts blocked.
- Cross-tenant access prevention.
- Prompt-injection resistance.
- Data leakage tests.
- Unapproved action prevention.

Build a versioned evaluation dataset containing normal, adversarial, and failure scenarios.

Use deterministic assertions where possible.

LLM-as-judge evaluation may supplement but must not replace deterministic tests or calibrated human review.

Do not report evaluation scores unless they were actually measured.

---

# PART VI — ENTERPRISE DATA AND SECURITY

## 19. Multi-Tenant Data Architecture

Design entities for:

- Tenant
- User
- Role
- Policy
- Agent Definition
- Agent Version
- Workflow Definition
- Workflow Execution
- Task Execution
- Tool Definition
- Tool Invocation
- Approval Request
- Memory Record
- Knowledge Document
- Audit Event
- Model Configuration
- Evaluation Run
- Usage Record

Use PostgreSQL with migrations, constraints, indexes, and concurrency controls.

Apply tenant isolation consistently across:

- APIs.
- Workflows.
- Queues.
- Agents.
- Tools.
- RAG queries.
- Memory.
- Object storage.
- Cache keys.
- Audit records.
- Telemetry.

Consider PostgreSQL Row-Level Security for defense in depth.

Prevent cross-tenant access even when a workflow is resumed, retried, or delegated to another agent.

---

## 20. Enterprise Security

Implement security by design.

### Identity and access

- OAuth 2.0 / OpenID Connect.
- JWT validation where appropriate.
- Service-to-service authentication.
- RBAC.
- ABAC where required.
- Least privilege.
- Workload identities.
- Tool-specific permissions.
- Tenant-scoped authorization.

### Application security

- Input validation.
- Secure output rendering.
- Secret management.
- Encryption at rest and in transit.
- Dependency scanning.
- Container security.
- API rate limits.
- Audit events.
- Data classification and retention.

### Agent security

- Agent capability allowlists.
- Delegation restrictions.
- Tool execution policies.
- Workflow budgets.
- Human approval.
- Untrusted data isolation.
- Sandboxed execution where appropriate.
- Protection against recursive action loops.
- Prompt-injection tests.

### Responsible AI

- Human oversight.
- Transparency.
- Traceability.
- Privacy.
- Explainable summaries of system actions.
- Appropriate uncertainty communication.
- Bias and fairness evaluation when relevant.
- Safety and misuse monitoring.
- Data governance.

Do not claim regulatory or certification compliance without the necessary organizational evidence and audits.

---

# PART VII — RELIABILITY, SCALABILITY AND OPERATIONS

## 21. Reliability Engineering

Explicitly handle:

- Agent failures.
- Model-provider outages.
- Tool invocation failures.
- Kafka message redelivery.
- Workflow restarts.
- Agent execution timeouts.
- Cancellation.
- Partial completion.
- Concurrent state updates.
- Duplicate external operations.
- Stale tool results.
- Permission changes.
- Orchestrator crashes.
- Database failures.
- Redis unavailability.

For each important operation, define:

- Idempotency requirements.
- Timeout.
- Retry policy.
- State transition.
- Recovery strategy.
- Compensation where supported.
- Observability requirements.

Use a durable workflow engine only where its reliability benefits justify the additional complexity.

Avoid distributed transactions unless strictly necessary.

External side effects must have explicit failure and reconciliation semantics.

---

## 22. Scalability and Performance

Design for scalable agent execution.

Support:

- Stateless API replicas.
- Bounded worker concurrency.
- Independent worker scaling where justified.
- Work queues.
- Resource quotas.
- Per-tenant rate limiting.
- Agent concurrency limits.
- Tool execution isolation.
- Model request batching where suitable.
- Caching with authorization-aware keys.
- Horizontal scaling.
- Load shedding.
- Cost-aware scheduling.

Establish baseline SLOs for:

- API availability.
- Task completion latency.
- Workflow success rate.
- Model/tool latency.
- Queue processing delay.
- Human approval processing.

Define targets from requirements and validate them with actual measurements.

Do not fabricate performance claims.

---

## 23. Observability and Auditability

Implement end-to-end tracing.

Example workflow:

`API → Orchestrator → Planner → Specialist Agents → Tool Gateway → Verifier → Final Response`

Each execution should expose:

- Correlation ID.
- Trace ID.
- Workflow ID.
- Task ID.
- Agent ID/version.
- Model/version.
- Tool invocations.
- Execution duration.
- Token usage.
- Estimated cost.
- Retry information.
- Failure classification.
- Approval status.

Collect metrics, traces, structured logs, and audit events.

Use OpenTelemetry, Prometheus, and Grafana or existing platform equivalents.

Redact sensitive prompts, documents, credentials, tokens, and private user information from logs and traces by default.

Capture concise action rationales and observable decisions where needed for auditing, without requiring hidden chain-of-thought disclosure.

Provide dashboards and actionable alerts for significant workflow and infrastructure failures.

---

# PART VIII — API, FRONTEND AND DEVELOPER EXPERIENCE

## 24. Platform API

Implement versioned REST APIs for:

- Agent registration and discovery.
- Agent definitions and versions.
- Workflow definitions.
- Workflow execution.
- Execution status.
- Task inspection.
- Cancellation.
- Tool registration.
- Tool invocation policy.
- Approval management.
- Knowledge ingestion.
- RAG querying.
- Evaluation execution.
- Usage and audit reporting.

Provide:

- OpenAPI documentation.
- Request validation.
- Authorization.
- Standardized error responses.
- Pagination and filtering.
- Idempotency handling.
- Structured execution status.
- SSE-based execution updates where suitable.

Avoid exposing internal infrastructure entities unnecessarily through the public API.

---

## 25. Enterprise User Interface

Implement a functional React + TypeScript interface.

Include:

**Agent Catalog**
- Registered agents.
- Capabilities.
- Agent versions.
- Configuration summaries.

**Workflow Dashboard**
- Workflow creation or selection.
- Task submission.
- Execution state.
- Agent activity.
- Task dependencies.
- Completion results.

**Human Approval Console**
- Pending approvals.
- Proposed actions.
- Risk and impact details.
- Approve/reject actions.
- Approval history.

**Knowledge Interface**
- Document ingestion.
- Ingestion status.
- Authorized knowledge search.
- Source citations.

**Monitoring Interface**
- Execution history.
- Failure details.
- Latency.
- Token usage.
- Cost summaries.

Implement consistent loading, empty, error, authorization, and retry states.

Do not substitute static mock UI data for functional backend integration in the final implementation.

---

# PART IX — TESTING, INFRASTRUCTURE AND DEPLOYMENT

## 26. Testing Strategy

Implement a meaningful automated test suite.

### Unit tests

Cover:

- Task planning validation.
- State transitions.
- Agent selection.
- Delegation policies.
- Tool authorization.
- Retry classification.
- Budget enforcement.
- Structured output validation.
- Approval rules.

### Integration tests

Cover:

- PostgreSQL.
- Kafka where used.
- Redis.
- Agent runtime persistence.
- Tool gateway.
- Authentication/authorization.
- RAG retrieval.
- Model provider adapter contracts.

### End-to-end tests

Verify:

1. A user submits a multi-step task.
2. The planner creates a valid plan.
3. The orchestrator delegates authorized subtasks.
4. Specialist agents execute.
5. Read-only tools return valid results.
6. Results are verified.
7. The final response is returned with provenance.
8. Workflow states are persisted correctly.

Also verify a state-changing workflow:

1. An agent proposes an action.
2. Policy enforcement requires approval.
3. The workflow pauses durably.
4. An authorized human approves the exact action.
5. The action executes once under idempotent retry conditions.
6. Execution is audited.
7. Failures are safely handled.

### Security tests

Include:

- Unauthorized agent invocation.
- Cross-tenant access.
- Delegation privilege escalation.
- Prompt injection.
- Tool output injection.
- Invalid tool arguments.
- Approval bypass.
- Duplicate action execution.
- Memory poisoning.
- Resource-exhaustion attempts.

### Evaluation tests

Create representative datasets for agent and workflow evaluation.

Use controllable fake model responses for deterministic CI tests and separate real-model evaluations.

---

## 27. CI/CD and Infrastructure

Deliver:

- Multi-stage Dockerfiles.
- Non-root application containers.
- Docker Compose local environment.
- PostgreSQL migrations.
- Kubernetes manifests or Helm charts.
- Health and readiness probes.
- Resource limits.
- Autoscaling configuration where justified.
- Terraform modules.
- CI quality gates.
- Automated test execution.
- Security scanning.
- Artifact versioning.
- Deployment documentation.
- Rollback procedures.

Provide a fully documented local development experience.

For real cloud deployment, require appropriate credentials, authorization, and infrastructure configuration.

Avoid provisioning paid resources automatically.

---

# PART X — IMPLEMENTATION STRATEGY

## 28. Mandatory Implementation Phases

Implement the platform incrementally.

### Phase 1 — Architecture and Foundation

- Inspect repository.
- Define architecture.
- Initialize application modules.
- Configure local infrastructure.
- Add database schema.
- Establish security boundaries.
- Implement core API foundation.

### Phase 2 — Agent Runtime

- Agent contracts.
- Agent registry.
- Execution lifecycle.
- Agent executor.
- LLM gateway.
- Structured outputs.
- Agent execution tests.

### Phase 3 — Multi-Agent Orchestration

- Supervisor.
- Planner.
- Specialist agents.
- Delegation.
- Task dependencies.
- Parallel execution.
- Result aggregation.
- Termination conditions.

### Phase 4 — Tools and Enterprise Integration

- Tool registry.
- Secure tool gateway.
- Read-only integration.
- Controlled state-changing integration.
- Authorization.
- Idempotency.
- Human approval.
- Audit events.

### Phase 5 — Memory and RAG

- Short-term context.
- Persistent memory policies.
- Knowledge ingestion.
- Embeddings.
- Hybrid retrieval.
- Grounded generation.
- Evidence provenance.

### Phase 6 — Reliability and Security

- Durable task execution.
- Failure recovery.
- Timeouts and retries.
- Concurrency controls.
- Tenant isolation.
- Prompt-injection defenses.
- Resource budgets.
- Security tests.

### Phase 7 — Frontend and Observability

- Functional UI.
- Workflow dashboard.
- Approval interface.
- Monitoring.
- Distributed tracing.
- Usage and cost reporting.

### Phase 8 — Evaluation and Deployment

- AI evaluation datasets.
- Automated evaluation pipeline.
- Load testing.
- Containerization.
- Kubernetes deployment foundation.
- CI/CD.
- Operations documentation.

Maintain a working vertical slice at every phase.

Do not move on from fundamental correctness or security defects merely to implement additional features.

---

## 29. Required Deliverables

The final project must contain:

1. Working enterprise application source code.
2. Multi-agent orchestration implementation.
3. Agent registry and execution runtime.
4. Structured planning and task delegation.
5. Agent-to-agent communication.
6. Tool registry and secure tool gateway.
7. Human approval workflow.
8. Model gateway.
9. Memory and context management.
10. Enterprise RAG capability.
11. Multi-tenant authorization.
12. Durable workflow execution.
13. Reliability and failure recovery.
14. Audit logging and distributed tracing.
15. Functional REST APIs.
16. Functional frontend.
17. Database migrations.
18. Automated unit/integration/E2E tests.
19. Agent and workflow evaluation framework.
20. Docker and local development configuration.
21. Kubernetes and infrastructure configuration.
22. CI/CD workflows.
23. Architecture diagrams.
24. Architecture Decision Records.
25. README and API documentation.
26. Security architecture and threat model.
27. Operations and recovery runbooks.
28. Verified implementation report.

If a deliverable cannot be completed because of an external dependency or environment limitation, explicitly report that limitation. Do not represent a stub, mock, or proposed design as a completed integration.

---

## 30. Definition of Done

The platform is implementation-complete only when it demonstrates all core workflows.

**Workflow A — Multi-Agent Knowledge Task**

A user submits a complex question. The supervisor coordinates task planning, authorized information retrieval, specialist analysis, evidence verification, and final answer synthesis.

The answer includes appropriate evidence provenance and uncertainty handling.

**Workflow B — Controlled Enterprise Action**

A user requests an operation requiring external modification. The system validates authorization, creates a structured plan, requests approval when required, executes the approved action using a secured tool, records the outcome, and handles retries safely.

**Workflow C — Durable Failure Recovery**

A workflow is interrupted during execution. After recovery, the system resumes or reconciles safely without repeating non-idempotent side effects.

**Workflow D — Tenant and Security Isolation**

A user or agent attempts to access an unauthorized document, memory record, tool, or workflow. The system denies access and does not leak information through downstream agents or generated responses.

**Workflow E — Operational Evaluation**

The system runs automated quality evaluations, records actual results, exposes relevant telemetry, and reports measured latency, usage, failures, and cost.

Additional acceptance conditions:

- Project builds successfully.
- Local environment is reproducible.
- Database migrations execute.
- APIs are documented.
- Tests exercise meaningful failure scenarios.
- Security controls are enforced by application code.
- Important operations are observable.
- Deployment configuration is validated where possible.
- Known limitations are documented.

Passing local tests does not alone prove production readiness. Clearly identify any remaining penetration testing, scale validation, disaster recovery, model evaluation, governance approval, or operational review required.

---

# PART XI — MANDATORY DELIVERY AND EXECUTION BEHAVIOR

## 31. Development Rules

While implementing:

- Inspect existing code before editing.
- Follow established architecture and conventions.
- Preserve unrelated modifications.
- Make focused, maintainable changes.
- Use supported dependency versions.
- Validate framework-specific behavior against official documentation.
- Avoid unnecessary dependencies.
- Never hardcode secrets.
- Never ignore errors silently.
- Never use unbounded retries or recursion.
- Never bypass authorization for convenience.
- Avoid mock-only completion claims.
- Cover critical failure scenarios.
- Run appropriate verification after changes.
- Explain meaningful architecture trade-offs.

Use comments to clarify non-obvious intent, not to repeat code behavior.

Apply practical production engineering rather than academic overengineering.

## 32. Reporting Requirements

After every meaningful milestone, report:

**Implemented**
- Actual code and infrastructure changes.
- Modules and APIs delivered.
- Working features.

**Architectural Decisions**
- Why the chosen approach was used.
- Significant alternatives and trade-offs.

**Verification**
- Commands actually executed.
- Test results.
- Build status.
- Evaluation results, if measured.

**Remaining Risks**
- Incomplete integrations.
- Security or operational concerns.
- Environment blockers.
- Unverified production assumptions.

**Next Step**
- The next highest-priority capability.

Never state that implementation or deployment succeeded without actual verification.

---

## 33. Final Execution Instruction

**START IMPLEMENTATION NOW.**

Your task is to deliver a complete Enterprise Multi-Agent AI Platform, not merely describe how to build one.

Begin by examining the existing repository and its instructions.

If the repository is empty, initialize a production-oriented project using the preferred technology stack and documented architectural defaults.

Create a concise architecture plan and immediately begin implementation.

Implement and verify the platform through successive working vertical slices.

Prioritize the following in order:

1. **Correctness and Security**
2. **Working Multi-Agent Orchestration**
3. **Controlled Tool Execution**
4. **Durable State and Failure Recovery**
5. **Authorization and Human Oversight**
6. **AI Quality and Evaluation**
7. **Observability and Operational Reliability**
8. **Scalability and Cost Efficiency**
9. **Developer Experience and Documentation**

The final objective is a **real enterprise multi-agent platform** with controlled autonomous execution, verified tool interactions, secure delegation, reliable workflows, enterprise knowledge integration, and measurable AI quality.

Do not stop at documentation, pseudocode, mock interfaces, or architecture diagrams.

**Deliver working software, verify what you can, disclose what you cannot verify, and provide an evidence-based readiness assessment.**
