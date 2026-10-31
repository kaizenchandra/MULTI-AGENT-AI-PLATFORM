# Operations and recovery

## Start, inspect, stop

```sh
docker compose up -d --build --wait
docker compose ps
docker compose logs --tail=100 backend
docker compose stop
```

`docker compose down` preserves the named database volume. `down -v` destroys data and is not part of verification or rollback. Never operate on unrelated Compose projects. Local API port is 18080 to avoid the existing application on 8080.

Health: public `/actuator/health/liveness` and `/actuator/health/readiness`. Database health is included in readiness; liveness should not depend on a transient provider outage. Inspect metadata-only workflow tasks and tenant audit events before retrying. Do not copy source text, authorization headers, `.env`, or provider errors into tickets.

## Recovery semantics

| Operation | Idempotency / timeout / retry | Recovery |
|---|---|---|
| Workflow submission | Tenant + owner + key + request fingerprint, active/hourly quota | Replay original key; conflict if payload differs |
| Stage execution | Locked workflow, validated dependency, stage/version progress; 25s transaction | Process death rolls back uncommitted work; next replica picks it up |
| Provider analysis | 15s complete response, 64 KiB response cap, at most 3 attempts with 2/4s delays | Transient timeout/429/5xx retry; schema/policy/config errors fail closed; possible duplicate billed inference after crash |
| Local case creation | Unique workflow case + ledger; exact approval checked; single transaction | Case/ledger/audit/progress commit atomically or roll back; repeat returns same case ID |
| Approval | Same row lock order as worker; separate subject; bound action and expiry | WAITING survives restart; duplicate/late/cancelled decision denied |
| Knowledge access | Tenant + owner/visibility + TTL, rechecked repeatedly | Source deletion revokes future reads; completed result returns conflict, no retained-text leak through task API |
| Cancellation | Owner scope and workflow lock | No new stages after commit; currently locked stage can finish first; terminal case cannot be undone by cancellation |
| DB unavailable | Bounded pool acquisition; poll loop logs classified failure and metrics | Polling continues; DB is the source of truth. Verify backup restore separately |

There is no at-least-once Kafka consumer in this release. `FOR UPDATE SKIP LOCKED` supplies the durable work queue. Transaction rollback is recovery, not a blind retry of an external side effect. Never add remote modification inside the local-case transaction and assume it inherits atomicity.

If an execution fails permanently, inspect the classification, correct the cause and submit a **new workflow with a new key** after human review. No public endpoint force-resets terminal workflows. Never edit approved action_json in SQL. Do not manually mark tools complete to bypass reconciliation.

## Backup, restore and rollback

Use encrypted PostgreSQL backups and point-in-time recovery. Record approved RPO/RTO before production launch; no RPO/RTO is established here. Restore into an isolated database, prevent provider/connector egress, validate Flyway schema and counts, run security and recovery acceptance, then switch the connection under an approved maintenance plan. Do not resume queued external actions until reconciled with destination systems.

Migrations are forward-only. Roll back application images using immutable digests only if the old application is compatible with the current schema. Otherwise deploy a reviewed forward fix; never drop production tables to make Flyway pass. `kubectl rollout undo deployment/backend -n kaizen-agents` is an operator action after compatibility review, not an automated command in this project. Maintain one migration owner, inspect lock contention, and separate runtime privileges.

## Deployment checklist and boundaries

Kustomize renders namespace, two backend replicas, two console replicas, services, disruption budget and restrictive network policies. Before applying, supply `platform-config` using your secret manager: DATABASE_URL/USER/PASSWORD, OIDC_ISSUER/AUDIENCE, and approved optional provider settings. Replace image tags with scanned digests. Configure TLS ingress, DNS, exact RDS/OIDC/JWKS/collector egress, private authenticated metrics scrape and alert receivers. The base denies those destinations, so it intentionally cannot serve a production deployment unchanged.

Terraform is an RDS foundation in an existing network, not a complete AWS landing zone. It requires subnet IDs, security-group IDs and KMS key ARN. Review `terraform plan` with an authorized operator before any apply; costs, IAM, state encryption, account controls and restore testing are external responsibilities. No cloud apply was run.

## Telemetry

The Micrometer/OpenTelemetry bridge retains API trace parent identifiers with workflow state. Worker spans carry workflow ID, agent/version and safe failure classifications; tool/provider spans inherit them. Prompts, goals, documents and approval bodies are excluded. Local extraction records 0 tokens and $0 inference cost. Provider records include reported token usage and configured-price estimate, not an invoice. Failed provider responses and responses lost before database commit can be billed without a usage row; reconcile with provider billing before treating usage as financial accounting.

Enable OTLP export explicitly. `infra/observability/otel-collector.yaml` is a collector pipeline example (debug metadata export); replace the exporter with an authenticated approved backend. Prometheus scrape credentials are operator-supplied. Import `dashboard.json`; install `alerts.yml` in your monitoring stack. These files have not been deployed to an actual monitoring service. Worker counters and agent duration histograms are emitted; business usage and approval status are available through APIs. Queue-age, approval-latency and database saturation alerts remain to be added.

## Proposed service targets, not measured SLOs

Initial review targets: 99.9% API availability/month; p95 authenticated metadata read under 250ms; p95 extractive research completion under 10s at an agreed load; queue delay under 5s; zero unapproved/duplicate local case commits. Human approval time is measured separately from machine execution. These are proposals, not established service commitments. Run representative load, failover and provider evaluations, then approve targets and error budgets. The included small read-load script and local browser acceptance do not establish availability or scalability.
