# API v1

All `/api/v1` routes require `Authorization: Bearer <access token>`. Local user and independent approver tokens are generated in `.env`. In production, OAuth scopes are space-separated in a verified JWT's `scope` claim. Subject and tenant are taken only from authenticated claims.

Authenticated OpenAPI JSON: `GET /v3/api-docs` (direct backend port 18080). An example:

```sh
set -a; . ./.env; set +a
curl --fail-with-body http://localhost:18080/api/v1/workflows \
 -H "Authorization: Bearer $LOCAL_USER_TOKEN" \
 -H 'Content-Type: application/json' -H 'Idempotency-Key: research-001' \
 -d '{"kind":"KNOWLEDGE","goal":"recovery policy"}'
```

The key is mandatory on workflow submission. Exact repeats return the original workflow; a different payload with the same owner-scoped key returns 409. Preserve the key if the network outcome is ambiguous. Submission returns 202; processing is asynchronous. Poll status or task metadata. SSE is not implemented.

| Method / path | Authorization / behavior |
|---|---|
| GET `/me` | Verified tenant, subject and scopes |
| GET `/agents` | Authenticated; immutable versioned logical-role contracts |
| GET `/workflow-definitions` | Authenticated; approved bounded plans |
| GET `/tools` | Authenticated; tool schemas/capability policies, no arbitrary invocation |
| GET `/models` | Authenticated; configured model capability and residency summary |
| POST `/workflows` | `workflow:write`; KNOWLEDGE also `knowledge:read` |
| GET `/workflows?page=0` | Tenant + owner, metadata only |
| GET `/workflows/{id}` | Tenant + owner; source access revalidated on completed knowledge results |
| GET `/workflows/{id}/tasks` | Tenant + owner; metadata excludes retained intermediate text |
| POST `/workflows/{id}/cancel` | `workflow:write`, owner; terminal states immutable |
| GET `/approvals?page=0` | `approval:decide`, tenant; exact proposed action and fingerprint |
| POST `/approvals/{id}/decision` | `approval:decide`, different subject, `{"approve":true,"actionHash":"..."}` |
| POST `/documents` | `knowledge:write`; `{"title":"...","text":"...","visibility":"PRIVATE"}` or TENANT |
| GET `/documents?page=0` | `knowledge:read`, authorized metadata |
| DELETE `/documents/{id}` | `knowledge:write`, tenant + owner; chunks/memory cascade |
| POST `/knowledge/search` | `knowledge:read`; `{"query":"..."}`; provenance-rich evidence |
| POST `/memories` | `memory:write` + `knowledge:read`; `{"sourceDocument":"uuid","fact":"exact source substring"}` |
| GET `/memories` | `knowledge:read`; caller-owned, authorized live source only |
| DELETE `/memories/{id}` | `memory:write`, tenant + owner |
| GET `/cases` | Caller-owned local integration results (up to 50) |
| GET `/usage` | Caller-owned last 100 usage records; configured-price estimates |
| GET `/audit?page=0` | `audit:read`, tenant |
| POST `/evaluations` | `workflow:write`; deterministic policy assertions, persists actual result |
| POST `/evaluations/workflows` | `workflow:write`, `knowledge:write`, `knowledge:read`; measured extractive pipelines, abstention and approval pause; no automatic approvals |
| GET `/evaluations` | Caller-owned last 50 measured results |

`CREATE_CASE` submission includes `action: {title, description}`. That exact action is persisted; no action-update endpoint exists. All state changes are allowlisted and typed. Unknown JSON fields and invalid UUIDs/enums are rejected. Lists accepting `page` return up to 50 records (zero-based). Results are arrays; cursor pagination and total counts are not implemented. No public generic tool-execution or dynamic code-registration endpoint exists.

Errors use RFC 9457 Spring `ProblemDetail` for application failures. 400 = invalid request; 401 = absent/invalid identity; 403 = insufficient capability; 404 = inaccessible/missing object without disclosure; 409 = state/idempotency/approval conflict or revoked result; 410 = retained result expired; 413 = request too large; 422 = policy/schema/evidence failure; 429 = rate/quota; 5xx = infrastructure/provider failure. Security-filter 401/403 may be empty standard servlet error responses; the console handles both formats.

Only `/actuator/health/**` is public. Details are not exposed. `/actuator/prometheus` is authenticated and must remain private. A dedicated metrics scope/service identity is an operational hardening requirement.
