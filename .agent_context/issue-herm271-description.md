Execute the fifth and final bounded `PKB-BL-026` consumer migration: replace `tooling/validation/pkb001_code_baseline.py` with a Java 17 API and packaged CLI that preserves its full observable behavior, following slices 1–4 (`pkb001_scenario_forward_gate.py`, `pkb001_component_compare.py`, `pkb001_blind_review.py`, `pkb001_next_run_gate.py`) as the pattern.

## Authorization and binding

- **Human decision**: Herman Wang (Human Reviewer), recorded on HERM-268 (comment `01a07253-3d58-7897-8860-f87278f7f840`): selected options 1, 2, 3 as sequential BL-026 slices 3–5. Slice 4 (HERM-270) is closed (`done`, PASS@`a35e59f` + explicit Human closure authorization `Approved`, comment `01a0739a-50f1-79da-a0de-2fba9ebe6f7b`). This issue covers **slice 5 only** — the last slice of the pre-authorized sequence.
- **Backlog item**: `PKB-BL-026` (status `IN_PROGRESS`; remains open — whether BL-026 moves to a completed delivery state is decided after this slice's closure evidence, by the Human Reviewer). **Spec binding**: `FRAMEWORK-SPEC.md` at `891e497968000c32984f26437eab811c063ec4cf`; requirement `PKB-JAVA-001`.
- **Repository**: `/Users/herman_mbp2023/ClawProjects/skills/Software-Factory`. **Base commit: `a35e59fe80a2e3894d66b003b0ad0af2664c9475`** (slice-4 reviewed candidate on branch `agent/delivery-engineer/herm-270`; contains slices 1–4). Continue on that branch line; it is not merged into `codex/project-folder-reorg`.
- Before any repository action, read `AGENTS.md` and the five active control documents in mandatory order from this exact checkout and revision. On conflict with active controls, stop and report `CONTEXT_CONFLICT`; do not guess.

## Authorized inputs

- The transitional consumer `tooling/validation/pkb001_code_baseline.py` (189 LOC; 1 test caller / 6 collected characterization cases; existing `argparse` CLI — a Java API **and** packaged CLI replacement is required). Behavior surface is many-input JSON aggregation; per HERM-268 option 3 it is small and isolated, but the thin characterization suite (6 cases) means the acceptance standard — full observable parity with the original Python consumer — must be verified with extra care (consider supplementary parity probes on an independent copied root, as slices 3–4 did).
- Its active caller(s) per the inventory (`validation/pkb001/java-migration/python-framework-inventory.json`): 1 test caller / 6 cases. Only the replaced Python source and its direct Python-only tests may be removed, after verified parity and caller cutover.

## Constraints

- One bounded consumer at a time; plan revision (`IMPLEMENTATION-PLAN.md` selected-work section) gates all code work — record slice-5 selection there per the established slice pattern.
- Immutable historical artifacts and external Graphify stay unchanged; no new Python framework behavior.
- Update the inventory cutover entry for this consumer following the slice-3/4 pattern (inventory JSON + cutover test), keeping historical entries intact.
- Preserve full observable behavior of the CLI (exit codes, stdout/stderr byte-level, generated artifacts) — the acceptance standard is parity with the original Python consumer.

## Definition of done

1. Java API + packaged CLI cutover complete; Python source and its direct Python-only tests removed only after verified parity.
2. Full verification at the new candidate, results recorded factually in the handoff:
   - `MAVEN_OPTS='-Xmx2g' ./mvnw test` — all Java tests pass.
   - `./mvnw -q package` — packaged JAR builds.
   - Packaged-JAR smoke of the new CLI — exit code / stdout / generated artifacts parity with the original Python consumer (on a copied input root where applicable).
   - `python3 -m pytest -q` — full suite green, with the collection arithmetic vs base explained (removed characterization cases + new cutover test), exit 0.
   - `python3 validation/pkb001/task7-evaluation/public_validate.py .` — 9/9.
   - `git diff --check` clean; no sealed input modified or regenerated.
3. Handoff comment naming the exact candidate commit, changed scope, tests and results, limitations, unresolved risks, and required next reviewer (Independent Adjudicator). **The handoff comment must be the last write of your run and must mention @Delivery Coordinator** so the handoff triggers immediate routing. Leave the issue active (`in_progress`); do not set `done`.

## Lifecycle

Implementation by Delivery Engineer → Coordinator verifies handoff completeness and routes one independent exact-revision review (Independent Adjudicator) → verdict binds to the exact candidate tuple → Human closure authorization required to set `done`. This is the final pre-authorized slice; no further BL-026 consumers are selected or implied by this issue.
