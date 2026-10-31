# ADR 0001: Modular monolith and database work queue

Status: accepted for this bounded implementation.

A single Spring Boot application owns APIs, supervision and tool policy. PostgreSQL stage transactions and row locks provide durable sequential progression. One worker per replica supplies a bounded execution bulkhead; SKIP LOCKED permits independent workflows across replicas. No Kafka, Redis or Temporal is needed for two short fixed workflows.

Trade-off: model calls hold a row/connection during a bounded transaction and cannot be made exactly once for billing. Long-running remote effects, high concurrency or independent worker scaling would justify claim leases, separate worker deployment and/or a durable workflow engine. They are not justified by a speculative agent count. Current plans are sequential; general parallel DAGs and compensating sagas are not claimed.
