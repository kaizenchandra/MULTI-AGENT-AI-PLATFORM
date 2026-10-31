# Kaizen Enterprise Agent Platform

A Java 21 / Spring Boot control plane with durable PostgreSQL workflows, bounded specialist roles, a secured tool gateway, approval-gated local case creation, evidence verification, and a React/TypeScript operations console.

**Status:** runnable local implementation with a deliberately constrained action surface. This is not a certification of production readiness or completion of every item in `PROMPT.md`. See [verification and gaps](docs/IMPLEMENTATION_REPORT.md), [architecture](docs/ARCHITECTURE.md), [security](docs/SECURITY.md), and [operations](docs/OPERATIONS.md).

## Local start

Prerequisites: Docker Engine/Compose with approximately 3 GB available. Java 21, Maven 3.9+, and Node 24 are needed only for host development.

```sh
sh scripts/init-local.sh
docker compose up -d --build --wait
```

Open **http://localhost:5173**. The API is **http://localhost:18080**, PostgreSQL is loopback port 5432. Local bindings never expose the development identities to the LAN. `.env` contains generated credentials, is ignored, and is created with mode 600. Enter `LOCAL_USER_TOKEN` in the console; use `LOCAL_APPROVER_TOKEN` in a separate browser session for approvals. Tokens remain in browser memory. Do not use local authentication for a shared environment.

1. In Knowledge, ingest a text document about a recovery policy.
2. In Workflows, submit a Knowledge research goal containing terms from that document.
3. Inspect persisted planner → research → analysis → verification → synthesis tasks and exact source quotations.
4. Select Create a local case and enter the exact proposed title and description.
5. Sign in as the separate approver, review the action and SHA-256 fingerprint, and approve it.
6. Observe the resumed workflow and local case identifier. No email, production modification, shell, or external business action is available.
7. In Monitoring, run policy assertions. `POST /api/v1/evaluations/workflows` additionally measures three real local workflow executions; the browser console includes a button for it.

Extractive mode is the default: it produces authorized source quotations and explicit uncertainty, uses no LLM, and reports zero model tokens/cost. It is useful for testing orchestration and evidence controls; it does **not** simulate intelligent reasoning or semantic embeddings.

## Host development and verification

```sh
sh scripts/init-local.sh
set -a; . ./.env; set +a
docker compose up -d postgres
# Terminal 1 (Java 21 must be selected):
AUTH_MODE=local SERVER_PORT=18080 mvn -f backend/pom.xml spring-boot:run
# Terminal 2:
npm --prefix frontend ci
npm --prefix frontend run dev
# Testcontainers needs access to Docker. Tests never skip if Docker is absent.
mvn -f backend/pom.xml verify
npm --prefix frontend run build
npm --prefix frontend test
# Against a running Compose environment:
cd frontend
npx playwright install chromium
npm run e2e
```

On macOS, correct a stale JAVA_HOME using `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`. OrbStack may require `DOCKER_HOST=unix://$HOME/.orbstack/run/docker.sock` and `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`. Test resources set Docker API 1.44 for modern daemons. Do not disable Testcontainers cleanup.

```sh
python3 scripts/recovery-smoke.py
python3 scripts/load-smoke.py
terraform -chdir=infra/terraform init -backend=false
terraform -chdir=infra/terraform validate
kubectl kustomize infra/k8s
```

The recovery script restarts **only this Compose project's backend**, and verifies durable approval and exactly one local case. The load script is a small authenticated-read smoke measurement, not capacity validation. Neither command provisions cloud resources.

## Production configuration

Default authentication is OIDC and fails startup without an HTTPS issuer. Supply `AUTH_MODE=oidc`, `OIDC_ISSUER`, `OIDC_AUDIENCE`, and database credentials via a secret manager. Accepted JWTs need a signature, valid timestamps, matching issuer/audience, `sub`, `tenant_id`, and space-separated `scope` claims. See [API authorization](docs/API.md). The UI accepts access tokens; automated browser SSO/PKCE is not implemented.

To enable the optional OpenAI adapter, explicitly configure `MODEL_MODE=openai`, `OPENAI_API_KEY`, `OPENAI_MODEL`, `MODEL_INPUT_PRICE`, and `MODEL_OUTPUT_PRICE` (USD per million tokens) in your deployment. Compose intentionally fixes extractive mode. Choose a Chat Completions model supporting JSON mode and `max_completion_tokens`; no model name, pricing, residency, or provider compatibility is assumed. This sends authorized evidence to OpenAI and incurs charges. Operator privacy/cost approval and live contract evaluation are prerequisites. No paid inference was used during development.

Set `TRACING_EXPORT_ENABLED=true` and `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` for an OTLP/HTTP collector. API → persisted workflow → agent spans retain a trace ID across restarts; local tool and provider spans are children. Prometheus is authenticated at `/actuator/prometheus`. Source texts, goals, tokens, and model bodies are excluded from logs and span attributes by default. See observability files under `infra/observability`.

## Layout

- `backend`: REST application, runtime, security, retrieval, local integration, Flyway migrations, tests.
- `frontend`: functional React console, API-client tests, real-backend Playwright test.
- `scripts`: credential initialization, verification, recovery and read-load acceptance.
- `evaluations`: versioned deterministic scenarios.
- `infra`: non-root containers, Kubernetes foundation, AWS RDS Terraform foundation, telemetry configuration.
- `docs`: diagrams, ADRs, threat model, API guide, recovery procedures, readiness report.

Kubernetes resources deliberately deny external egress until an environment overlay supplies exact database, identity, collector and optional provider destinations. Supply secret `platform-config`, immutable image digests, TLS ingress, and restricted database roles before deployment. Terraform creates billable RDS infrastructure only if an operator runs an authorized apply. No apply is part of startup, CI, or verification.
