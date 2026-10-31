# Security architecture and threat model

## Assets and trust

Protected assets: enterprise source text, retained evidence and memory, proposed business actions, exact human decisions, tenant identities, provider credentials, model usage, audit records and workflow state. Threat actors include malicious users, hostile source documents, compromised tools/providers, adjacent tenants and compromised application credentials. Database administrators, identity administrators and cluster operators are privileged trust dependencies.

| Threat | Implemented control | Remaining assurance work |
|---|---|---|
| Tenant/owner data access | Claims-derived tenant and subject, predicates on public reads/writes, composite tenant FKs, private/tenant document ACLs | RLS defense in depth, external IdP tenant provisioning review |
| Forged/expired JWT | Spring Resource Server signature, issuer, audience, time validation; bounded tenant/subject | Live issuer/JWKS rotation and availability tests |
| Delegation escalation | Fixed schema/plan topology, role tool allowlists, no user-controlled tool endpoint | Expand policies only with reviewed versioned contracts |
| Direct/indirect injection | Untrusted JSON data isolation, models never authorize tools, exact citation verification, no autonomous planner | Real-model adversarial evaluations and human quote review |
| Tool result laundering | No general tool chain, application-controlled outputs, provenance checks | Remote connector schema/signature validation when introduced |
| Approval bypass/replay | Separate subject and scope, exact action hash + full JSON equality, expiry, row locks, immutable public proposal, no duplicate decisions | Upstream identity separation/approver assignment governance |
| Duplicate side effect | Unique workflow case key, invocation ledger, row lock, one transaction, replay conflict detection | Remote actions require downstream idempotency/reconciliation; not implemented |
| SSRF/RCE/file access | No fetch/shell/file tool, fixed HTTPS provider destination, no redirects | Controlled egress proxy/network policy and DNS/credential boundary review |
| Memory poisoning | Explicit writes require authorized source substring, source FK, expiry and audit | Truth of the source itself still requires governance |
| Unauthorized provider disclosure | Access checked before provider dispatch and publication; default local extraction; no failover | Residency and account data policies, DPA/governance approval, PII classification/redaction |
| Resource exhaustion | Body/context/output/attempt/time/active/hourly/API quotas, bounded threads and pod resources | Distributed tenant fairness, queue aging, capacity/load validation |
| Secret and private-content logging | No prompt/source/token/provider-body logs; trace IDs and safe metadata only; generic provider errors | Central telemetry review, redaction regression checks and access control |
| UI injection/token theft | React text escaping, no raw HTML, in-memory token, CSP, no CDN dependency, no token URLs | Browser SSO/PKCE, hardened TLS ingress, XSS penetration testing |
| Audit tampering | Audit writes in effect transaction, tenant-scoped read API, no update/delete API | DB role append-only grants, external immutable/WORM sink, signed chains |

## Authorization policy

Tenant claims must match `[a-zA-Z0-9_-]{1,100}`. Scopes are explicit: workflow:write, knowledge:read, knowledge:write, memory:write, approval:decide, audit:read. Requester and approver must have different subject IDs. Approver scope currently permits all approvals in that tenant; organizational approver groups/assignment and fine-grained ABAC are not implemented. Workflow ownership is stricter than tenant membership.

Executions carry a validated submission capability snapshot, limited to the fixed workflow for at most ten minutes. Document revocation/deletion is rechecked before model dispatch, verification, publication and completed-result reads. **Immediate IdP scope revocation during already-running workflows is not enforced:** JWTs are offline validated; use short-lived tokens and cancellation, or add token introspection/revocation/version policies before high-impact integrations. Never interpret these local controls as permission to connect production-modifying tools.

## Local and deployment safeguards

Local authentication requires two distinct random secrets of at least 32 characters. It is fixed to tenant `local` and is never the default application mode. Compose binds loopback only. Production configuration must use OIDC, TLS on ingress and JDBC (`sslmode=verify-full` and approved CA), encrypted volumes/backups, private database networking, restricted workload credentials and external secrets. Terraform enables RDS encryption, private reachability, Multi-AZ, backup retention and deletion protection. It does not configure a whole compliance environment.

Separate migration/owner credentials from runtime credentials in deployment. Runtime should have SELECT/INSERT/UPDATE on workflow/task/approval/usage/case tables, SELECT/INSERT/DELETE on knowledge/memory, sequence usage, and only SELECT/INSERT on audit_events. Pre-install pgvector with a privileged migration process. The current container runs Flyway on startup with its configured datasource: local uses the development database owner. Production migration-role separation must be supplied before rollout. RLS is not implemented. Workers need access across tenants and rely on application enforcement and composite keys.

## Retention and privacy

Documents expire after 30 days and memory after 7 days; reads enforce expiry immediately. An hourly job deletes expired sources/memory and purges task outputs and workflow context/results seven days after workflow deadlines. Source deletion cascades chunks and source-bound memory. Already-returned quotations cannot be recalled from clients. Workflow goal/action, approvals, local cases, usage and audit metadata remain business records; organization-specific deletion, legal holds and backup erasure policies are not implemented. Database backups may retain deleted source data until their retention expires. Retention purging is not proof of regulatory erasure.

Do not store hidden chain-of-thought. Task outputs contain only plans, evidence, structured quote selection and tool results. Observable action metadata is audited without requesting private reasoning traces.

## Security release gate

Run dependency/container scanning and adversarial tests; then obtain a threat-model review, penetration test, issuer/role review, live provider privacy evaluation, backup/restore exercise and scale/abuse testing. No regulatory, certification, fairness or production-readiness claim is made. Unsupported assertions are prevented by restricting output to verified source quotations; source accuracy, completeness and bias are not guaranteed.
