# Tasks 3–5 Minimum Improvement Adoption Design

**Status:** Approved design pending written-spec review  
**Scope:** `SF-BL-002` effectiveness successor Tasks 3–5  
**Decision:** Adopt only improvements that directly raise delivery correctness,
reviewability, or measurable workflow performance before the next dispatch.

## 1. Objective

Turn the verified workflow, coding-quality, and KPI findings from
`SF-BL-002-SCENARIO-INTENT-003` into executable constraints for the next
Tasks 3–5 delivery. Do not reopen the accepted Task 1 candidate, expand the
Framework Spec, or start the deferred `SF-BL-003` refactor.

## 2. Scope boundaries

This adoption may update the selected `IMPLEMENTATION-PLAN.md`, `STATUS.json`,
the execution envelope, and supporting workflow/evidence instructions. It may
add Task 3–5 Java production code, tests, and new immutable `*-002.json`
artifacts after dispatch.

It must not:

- modify accepted Task 1 proposal or acceptance artifacts;
- modify PKB-001 experimental evidence or any existing `*-001.json` artifact;
  the supporting operations guide may receive only the approved HERM-364 KPI
  record before the next assigned control commit is frozen;
- change Product meaning, accepted Behavior Scenarios, evaluator truth, or
  acceptance thresholds;
- select `SF-BL-003` or perform a broad scenario-pipeline refactor;
- add a new governance layer or active control document.

## 3. Execution design

Implementation parallelism and runtime artifact ordering are separate:

```text
Task 3 matcher code/tests ───────────────┐
                                         ├─ combined Java integration
Task 4 Graphify-run code/tests + fixture ┘             │
                                                        ▼
production artifacts: Task 3 → Task 4 → Task 5 evaluation
```

Task 3 and Task 4 implementation may run concurrently only when their owned
production/test paths do not overlap and Task 4 uses a frozen synthetic matcher
fixture. Task 5 evaluation code/tests may also be prepared against frozen
fixtures when its ownership is disjoint. Production artifact generation remains
ordered because each downstream artifact seals the exact upstream digest.

The Delivery Coordinator creates the complete child skeleton before starting
implementation, records stable routing keys and the integration order, and
proves distinct worktrees and non-overlapping mutation ownership. One combined
candidate receives full regression and an independent exact-candidate review.

If safe fixture-driven parallelism cannot be established, the Coordinator must
report the concrete blocker. Task numbering or downstream data flow alone is
not sufficient justification for sequential implementation.

## 4. Coding-quality adoption

The assigned control commit must be a descendant of
`277100f6bcf18a7cba5b565f7d44f71967021a1f`. Every producer and independent
reviewer must read the bound `JAVA-CODING-GUIDELINES.md`; the reviewer reports
Spec conformance and applicable guideline findings in the same review run.

The Tasks 3–5 execution envelope makes the following completion requirements
explicit:

1. Every consumed accepted-intent, assignment, Graphify, and evaluator-boundary
   artifact is validated as a complete document before use: schema/shape,
   authority, exact source revision, governing digest, record uniqueness, and
   expected upstream artifact digest must fail closed on mismatch.
2. New keyword/ranking policy collections are immutable. Mutable arrays or
   collections must not be exposed through accessors.
3. The matcher names and tests one explicit tokenization policy. It must not
   silently select between the existing different Latin-token behaviors.
4. Before duplicating hashing, sealed-input verification, or collision-safe
   writing, the implementer checks for an existing compatible mechanism.
   Reuse must not merge distinct trust-boundary policy. If extraction would
   widen scope or couple independent domains, retain the local mechanism and
   record a guideline deviation with reason, compensating tests, and
   `SF-BL-003` disposition.
5. Class size is only a cohesion signal. Separation requires a clear contract;
   no micro-class split is performed solely to meet a line target.

The accepted Task 1 generator is grandfathered. Its mutable `String[]`, broad
responsibility, and tokenizer consistency findings remain deferred to
`SF-BL-003`; changing it now would invalidate its reviewed candidate and add an
unnecessary remediation cycle.

## 5. Verification and evidence

Each implementation slice must use TDD and report its focused command. Combined
integration must run the complete Maven suite with `MAVEN_OPTS='-Xmx2g'`,
keeping aggregate command memory below 8 GiB. The combined verifier also proves:

- deterministic double-run bytes and SHA-256 digests;
- create-new/collision refusal;
- full artifact-level mixed-revision and wrong-digest rejection;
- evaluator inputs remain inaccessible until every non-evaluator input is
  sealed;
- no change under `validation/pkb001/` or to existing `*-001.json` files;
- proposal-only authority and semantic-publication refusal;
- independent review actor/run/candidate identity.

The existing experimental thresholds remain unchanged: scenario trace coverage
at least `6/10`, exact chain recall greater than `0.0`, and exact PRIMARY
precision at least `0.70`. Safety verification passing does not imply these
effectiveness thresholds pass.

## 6. KPI adoption

The completed HERM-364 execution receives one compact per-slice record in the
existing `MULTICA-SLICE-OPTIMIZATION.md`; no new KPI document is created.

Every Tasks 3–5 slice and combined integration package records:

- full run IDs and roles;
- implementation start and completion;
- review start and verdict time;
- review-routing wait and total cycle time;
- first-pass result and remediation count;
- tool-call count;
- input, output, and cache-read tokens;
- telemetry source, collection time, and completeness.

Missing usage is recorded as:

```text
token_usage: UNKNOWN
source: multica issue usage API and recorded run evidence
completeness: UNAVAILABLE
```

It is never reported as zero or an unqualified `N/A`. A workflow improvement is
accepted only when token cost, cycle time, and first-pass quality are considered
together. One metric may not be optimized by weakening tests, independent
review, or evidence requirements.

## 7. Acceptance

The adoption is ready for dispatch planning only when:

1. the Plan contains an exact slice matrix, ownership, fixtures, dependencies,
   integration order, exact input digests, and verification commands;
2. the assigned control commit descends from the coding-guideline commit and
   the envelope explicitly binds the guideline;
3. artifact-level validation and tokenizer policy are hard acceptance cases;
4. the HERM-364 KPI record distinguishes known, unknown, and incomplete data;
5. the five active controls remain internally consistent and no execution is
   marked active before dispatch.

After execution, compare the new cohort with prior comparable slices using the
three existing KPIs. Defer broader maintainability work to `SF-BL-003` unless a
finding blocks correctness in the current Tasks 3–5 scope.
