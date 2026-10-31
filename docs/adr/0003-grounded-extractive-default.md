# ADR 0003: Verifiable quotations before autonomous reasoning

Status: accepted with documented capability limits.

Default analysis is an extractive strategy over authorized evidence, followed by deterministic source membership and access checks. A configurable OpenAI JSON-mode adapter can select exact evidence quotes through a fixed egress transport. Providers cannot choose tools, authorize actions, change workflow topology, or publish arbitrary uncertainty text. Unsupported questions abstain. Prompt version is grounded-1.

Trade-off: this controls unsupported generated assertions but offers less synthesis than a full research assistant. Lexical hash64-v1 vectors are reproducible and cost-free; they do not replace a learned embedding model. Real-provider quality, calibrated reasoning, semantic retrieval and additional provider adapters must be evaluated before claiming those capabilities. No paid inference is needed to test orchestration and security.
