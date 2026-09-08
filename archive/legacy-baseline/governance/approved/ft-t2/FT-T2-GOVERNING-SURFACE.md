# HERM-211 FT-T2 Feature Closure — Locked Governing Surface

> **Status:** APPROVED SEMANTICS — NORMALIZED STANDALONE PHYSICAL REPRESENTATION  
> **Position:** helper capability under `T2-Delivery-Spec-Skill`; not Layer 1 itself and not a fifth canonical stage  
> **Upstream identity:** HERM-211 locked helper surface  
> **Byte identity:** Original upstream package byte identity is **NOT_CLAIMED**. This standalone package materializes the locked semantics so Multica does not require an external import.

## Locked surface

Exactly six helper contracts:

```text
IntentSpec
CandidateRepoSet
ChangeSurfaceSet
EvidenceRecord
ClosurePackage
ClosureReview
```

Exactly five helper Skills:

```text
feature-intent-analysis
repo-discovery
changesurface-investigation
dependency-closure
closure-review
```

Helper closure status only:

```text
OPEN
PARTIAL
CLOSED_WITHIN_DECLARED_SCOPE
```

Review outcome only:

```text
ACCEPT_CLOSED_WITHIN_DECLARED_SCOPE
REOPEN
NEEDS_MORE_EVIDENCE
```

Canonical T2 gate only:

```text
SPEC_READY | BLOCKED
```

`CLOSED_WITHIN_DECLARED_SCOPE` never implies `SPEC_READY`, never authorizes T3, and is reopenable when new evidence appears.

All helpers pin the exact active `intention.md` with `INTENTION_READY` and `ACTIVE`. `feature-intent-analysis` is a helper projection from Intention into `IntentSpec`; it does not replace T1 and does not consume raw Human Signal as canonical authority.

Change Surface uses Layer 1 semantics:

```text
status:
CANDIDATE | CONFIRMED | EXCLUDED | UNRESOLVED

required_action:
CHANGE | VERIFY_ONLY | NO_CHANGE | NOT_APPLICABLE | UNDECIDED
```

Layer 2 Product Intelligence and Structural Intelligence can generate/orient/prioritize candidates. Current `CONFIRMED` or `EXCLUDED` disposition requires current feature-specific Evidence when material. Structural Intelligence must not introduce a `GRAFEL` CandidateRepoSet basis; structural candidates are grounded through PA-03 repository identity and use a legal PA-03 basis.

The physical contract files and Skill files colocated in this standalone project are the execution representation of this locked surface. Any future semantic change requires explicit governing approval rather than an implementation-only edit.
