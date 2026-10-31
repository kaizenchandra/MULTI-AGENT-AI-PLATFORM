# ADR 0002: Exact-action independent approval and local atomic effects

Status: accepted.

The only mutating integration creates a local database case. The proposal is typed and immutable through public APIs. An approval stores its canonical serialized action and SHA-256 fingerprint; a different authorized subject must decide before expiry. The gateway checks both hash and full action equality under the workflow lock. Case, invocation ledger, audit and stage progress share a transaction.

This permits safe rollback/recovery for the demonstrable integration. It intentionally does not pretend that a database rollback can undo a remote API call. Introducing external writes requires explicit destination idempotency/reconciliation semantics and independent review. Duplicate user submissions have separate owner-scoped idempotency binding.
