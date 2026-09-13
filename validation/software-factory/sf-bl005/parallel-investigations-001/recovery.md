# Handoff recovery investigation — SF-BL-005-PARALLEL-INVESTIGATIONS-001 / stage 1 RECOVERY

Author: Validation Steward (HERM-505). Document review only; no runtime
verification claim, execution, or reproduced loss.

## Pinned sources

| ID | Source | Revision | SHA-256 |
|---|---|---|---|
| S1 | `validation/software-factory/sf-bl005/fdp-reconciliation-realworld-003.md` | `8f18785ceef6e6120f6d367a39517202a3d2a233` | `de2835001016cfa8c1c81042b9b60e6b12b8a4f30ec41f5917cdafdc33d93353` |
| S2 | `validation/pkb001/operations/MULTICA-SLICE-OPTIMIZATION.md` | `8f18785ceef6e6120f6d367a39517202a3d2a233` | `a6d0b9de5cd5af06d61c495dd388325d9ccdf695cfed4ba28000d8e79f568f8e` |
| S3 | `validation/software-factory/sf-bl005/execution-envelope-parallel-investigations-001.json` | working tree | `c28e9ce1337b6cbe926921c418f599e0d34aa808bbd1658aa31bbf4d355c6b76` |
| S4 | `IMPLEMENTATION-PLAN.md` stage 1 "Durable handoff and recovery contract" | control commit (full SHA: S1 row) | `f9f0aac3a6b988e478eb8d412d1502091ff25eea3f49b9f1f20cb809a3f2ee31` |

Preflight: base `86e96e8d317b9b7ebfc435f54918d62ae157a431` → control
`8f18785ceef6e6120f6d367a39517202a3d2a233` → dispatch
`d517e4b3396a7c6c7dc404a5f63958c3a29a2980` all resolve as commits with
required ancestry. Envelope bytes match S3 digest. The managed worktree runs on
daemon-created branch `agent/validation-steward/herm-505`; the envelope names
`codex/sf-bl002-route-aware-correction` — a dispatch-side naming discrepancy,
not a checkout mutation (start commit and branch preserved).

## Observed facts (F), inferences (I), proposals (P)

| # | Label | Finding | Source |
|---|---|---|---|
| F1 | FACT | REALWORLD-003 stage-2 artifacts were written under `.fdi-work/realworld-ee17e31`, outside the Plan-owned `validation/` namespace. | S1 |
| F2 | FACT | Execution records describe scoring re-execution after artifact loss, contradicting exactly-once. | S1 |
| F3 | FACT | The replacement receipt is `receipt-003.md`, outside the Plan-owned `receipt-002.md` path. | S1 |
| F4 | FACT | Records report Java 23 for scoring against the Java 17 requirement; runtime binaries not retained (reported evidence, not locally reproduced). | S1 |
| F5 | FACT | Original producer/scorer JAR bytes were lost; source hashes alone cannot verify those exact binaries. | S1 |
| F6 | FACT | Scorer reports `SCORING_MECHANICS_ONLY` and `experimentDecision NOT_RUN`; receipt PASS does not override envelope deviations. | S1 |
| F7 | FACT | Diagnostic metrics were read from exact comparison bytes (`5f07244f…:selected-evidence.json`, `5f07244f…:improved.json`, `c9f669cf…:comparison-002.json`); recall is 0 in both arms; both chainCoverage values are null (two chain definitions missing). | S1 |
| F8 | FACT | The Plan's stage-1 contract requires full commit IDs, owned paths, retained runtime location/digest, byte lengths, SHA-256 values, JAR bytes retained outside disposable worktrees, and independent receiver read-back before cleanup. | S4 |
| F9 | FACT | The Plan's recovery rule: a lost artifact may only be restored from a verified identical copy; else PLAN_BLOCKED / PLAN_CHANGE_REQUIRED; preserve failed receipts; no exactly-once claim when a prior scorer outcome is unknown. | S4 |
| F10 | FACT | Operational guidance requires exactly one start trigger (assignment or mention, never both); an equivalent later run is COALESCED_DUPLICATE with no second artifact or Coordinator mention. | S2 |
| F11 | FACT | Handoff must name exact candidate, changed paths, results, limitations, blockers, and reviewer, with one structured Delivery Coordinator mention as sole trigger; the worker does not reassign or move to in_review. | S2 |
| I1 | INFERENCE | F1–F3 are one causal pattern: handoff bytes were not durably externalized before worktree disposal, so recovery substituted new outputs in a new namespace instead of restoring identical bytes; receipt path and namespace changes are symptoms, not independent defects. | From F1–F3, S4 |
| I2 | INFERENCE | F4–F5 mean no exact-runtime or exact-binary claim can be reconstructed for REALWORLD-003 scoring: rebuilt JARs differ in bytes, and source-hash equality never substitutes for binary-hash equality. Future verification claims must be rebased onto newly retained binaries. | From F4–F5, S4 |
| I3 | INFERENCE | F2 plus F9's "no exactly-once claim" rule make the REALWORLD-003 scoring outcome non-replay-auditable; the zero-recall metrics remain valid diagnostic evidence for existing bytes, but the provenance chain is broken at the loss point. | From F2, F9, S1 |
| I4 | INFERENCE | F10–F11 show the trigger/handoff protocol is instruction-level only; without controller-recorded idempotency keys and receiver digest read-back, duplicate dispatch and lost-handoff classes remain open (D2, D3). | From F10–F11, S2 |
| P1 | PROPOSAL | Adopt the single minimum lifecycle change below (L1). Other remedies (diagnostic slice, adapter, re-enveloped rerun) remain separate Plan selections, unchanged here. | S1 recommendation, S4 |
| P2 | PROPOSAL | Precedence proposal for a future envelope (per S4): integrity/compliance failure first; missing mandatory metrics => INCONCLUSIVE; only complete valid metrics permit a threshold recommendation. Human approval needed if this changes acceptance semantics. | S4 |

## Decision table — six loss cases

Each case names STOP (halt condition), RECOVERY (only authorized action), and
OWNER (decider). Rule from S4/S2: restore only from a verified identical copy;
never regenerate, rename outputs, or rerun a scorer.

| # | Loss case | STOP rule | Recovery rule | Owner |
|---|---|---|---|---|
| D1 | Missing bytes | Halt when a required artifact (report, receipt, JAR, envelope, evidence file) cannot be retrieved at its pinned path. | Restore only a byte-identical copy verified by SHA-256; if none exists, report PLAN_BLOCKED / PLAN_CHANGE_REQUIRED and stop. No silent regeneration, new output location, or rerun. | Execution Plane detects/stops; FDP authorizes replan/new envelope. |
| D2 | Wrong hash | Halt when computed SHA-256 differs from the pinned digest; the artifact counts as absent. Never "close enough", never repair-in-place. | Re-obtain exact bytes from the pinned source and re-verify; if the pinned digest is wrong, report PLAN_CONFLICT. Binary-hash equality is mandatory; source-hash equality never substitutes for it. | Worker stops; FDP resolves identity conflict. |
| D3 | Duplicate trigger | Halt when an equivalent earlier run is queued, running, or completed, or the exact sealed result exists; classify COALESCED_DUPLICATE. | None for the duplicate: change no artifact, emit no second verdict or Coordinator mention, record the attempt, stop. Coordinator reconciles the original run. | Specialist self-classifies; Coordinator owns routing. |
| D4 | Unknown scorer outcome | Halt when a prior scorer invocation's outcome is unknown (e.g., re-execution after loss); no exactly-once claim, no metric verdict from that run. | Preserve failed/original receipts untouched; return the documented state to FDP; re-execution needs a new envelope and unused namespace. | Execution Plane reports; FDP decides semantics; Human approves semantics change. |
| D5 | Runtime loss | Halt when the exact runtime (Java executable path/version, JAR bytes) is unavailable; forbid any exact-runtime verification claim. Java other than 17 (reported Java 23) fails the requirement. | Re-verify only with newly retained, digest-pinned runtime binaries and recorded executable path/version; until then report the runtime claim unavailable, not approximate. | Execution Plane retains/rerecords runtime; FDP validates claims. |
| D6 | Output collision | Halt when output would land on an existing pinned path or another execution's namespace; never overwrite, append, or rename an existing receipt/artifact. | Write only into the execution's own declared namespace; if the owned path holds foreign content, report PLAN_CONFLICT. Receipt replacement (receipt-002 vs receipt-003) needs FDP replanning. | Worker stops; FDP replans namespace. |

## Receiver read-back criteria

Before any worktree cleanup or stage advance, a receiver (distinct run/actor
per S2 independence rules) MUST verify, from durable storage outside the
producer's disposable worktree:

1. Every handoff artifact at its declared path: byte length and SHA-256 match
   the producer manifest.
2. JARs/runtime inputs: binary bytes verified; absolute Java executable
   path/version recorded; non-Java-17 rejected (S4).
3. Full 40-character base/control/dispatch commits, ancestry, and an owned-path
   diff limited to the ChangeClaim.
4. Receipt continuity: original receipts preserved; replacements declared and
   replanned, never silent.
5. Read-back result (identity, digest, time, receiver run ID) committed as
   evidence; a passing read-back alone authorizes cleanup.

## Minimum recommended lifecycle change (L1)

Add one mandatory durable-handoff gate: no producer worktree is cleaned up until
an independent receiver run retrieves every handoff artifact (reports, receipts,
JARs, envelope) from durable storage and verifies byte length + SHA-256 against
the producer manifest, committing the read-back record as evidence. Had this
gate preceded the REALWORLD-003 worktree disposal, it would have blocked D1–D6
at the loss point. It adds no new authority (Execution Plane mechanics only) and
needs no Spec change.

## Limitations and unresolved risks

- Document review against S1–S4 only; no runtime, build, or scoring verification
  performed or claimed.
- F4 (Java 23) is S1-reported evidence, not locally reproduced.
- F7 metrics are valid for the pinned comparison bytes but inherit the broken
  provenance chain (I3); they must not anchor acceptance verdicts.
- Envelope branch naming vs managed worktree branch
  (`agent/validation-steward/herm-505`) is a dispatch-side discrepancy recorded
  for FDP; start commit and branch preserved per policy.
