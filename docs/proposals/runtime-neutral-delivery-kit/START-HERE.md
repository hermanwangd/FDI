# Runtime-neutral Feature Delivery and Execution Kit

Status: **PROPOSED — design/adoption package, not an approved execution envelope.**

This package fixes the architecture review and targets company AI adoption. Its
reference is Software-Factory commit `2877007af6f6f4ebcc23a393c0d2424292cb6f0e`.
The seven untracked main/docs rc4 files are candidate inputs, not committed main
truth. See [comparison](architecture/BASELINE-COMPARISON.md) and
[provenance](SOURCE-MANIFEST.json). No active controls are changed by this package.

Read in order:

1. This file and [package instructions](AGENTS.md).
2. [Authority](architecture/AUTHORITY-AND-DECISION-MODEL.md).
3. [Architecture](architecture/ARCHITECTURE.md) and [execution semantics](architecture/EXECUTION-SEMANTICS.md).
4. [Contracts](contracts/README.md), [portability](architecture/WORKFLOW-PORTABILITY.md), and [security/operations](architecture/SECURITY-AND-OPERATIONS.md).
5. [Roles](agents/ROLES.md), [skill catalog](skills/catalog.json), and only the skills needed for the assigned work.
6. [Conformance](conformance/ACCEPTANCE.md), [adoption sequence](adoption/ADOPTION-SEQUENCE.md), and [company AI handoff](adoption/AI-HANDOFF.md).

Audience: company Feature Delivery actors, Execution Plane implementers,
independent reviewers and Human Authority. Objective: preserve engineering
authority and reliable behavior while replacing an execution runtime.

The kit includes declarative contracts, skills, fixture examples and reviewable
acceptance cases. It does not ship a functioning runtime or claim behavioral
validation. Existing Software-Factory framework implementation remains Java 17 /
Spring Boot 3.4.1. Runtime neutrality does not authorize a language migration.

Mandatory upstream invariants are preserved; additional serialized profiles are
proposal extensions requiring review. Imported copies under `reference/` are
exact source evidence, never receiving-project active controls. Source status
is a snapshot, not live remediation progress. Do not apply this package to the
ongoing SF-BL-002 execution or create a new selected Backlog item automatically.

Company repositories with no shared baseline must adapt this design under their
own controls. `REFERENCE_ONLY`, `DO_NOT_APPLY_BLINDLY`, `NO_SHARED_BASELINE`;
automatic application is prohibited. This is a design kit, not an SF-BL-004
change-reference exporter output or a patch.

Package integrity is listed in `package-lock.json`. Verify all paths, byte
counts and SHA-256 values before use. A digest proves byte identity, not trusted
authorship: approve the root lock digest through the company's trusted channel.

See [review resolution](REVIEW-RESOLUTION.md) for the fixes and remaining gates.
