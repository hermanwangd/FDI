# RC10 S05 Reviewed-Delivery Role Guidance v0.1

**Profile:** `ENGCIM-S05-REVIEWED-DELIVERY-v0.1`  
**Adopted decisions:** D01–D08 in the Human decision receipt at `validation/rc10/candidate-runtime/s05-profile-adoption.json`  
**Adopted source SHA-256:** `145cfa3fc1b00bf25f533052478970979a7cf056ce23440d2c47436d5300f449`

This guidance applies only when an RC10 candidate explicitly selects this profile. It supplements the sealed RC6 workflow for that candidate; it does not edit RC6 bytes or reinterpret historical runs. It does not grant repository or deployment authorization, promote evidence, or set Human DONE.

The rules below define profile semantics, not Multica's runtime loader precedence. Preserve existing role settings and Skills. Report source identity, configured readback, and actual per-run consumption as separate evidence.

For validation under D08, stay within the approved fixtures, isolated workspace, and 8 GB memory ceiling. Report local/model checks, configured readback, and live end-to-end evidence separately. Cover a single-repository case, a multi-repository positive case only when source evidence proves the dependency, and a NO-EDGE negative control. The currently pinned NO-EDGE pair does not prove a positive dependency; if no approved positive fixture supports one, record that case NOT RUN and do not invent a relationship. Reuse historical evidence only for its verified exact scope and revision. No production mutation is part of this profile.

## Existing Orchestrator excerpt

For a candidate selecting this profile, use this handoff sequence:

```text
Intent / AC + applicable Product Context
→ SA / SD and C1
→ independent Design Review and C2
→ implementation planning and C3
→ applicable operation authorization
→ eligible Coding / self-tests and independent Code Review C4[]
→ integration; material changes refresh affected C4
→ exact candidate and governing references C5
→ S06 independent testing / verification
→ final QA Review
```

- Reuse the existing S04 IntentSpec, Scenario composition, squad and issue/fan-in path. Do not add a Scenario agent, planner, scheduler, retry service, or new gate engine.
- Map the handoff through existing artifacts: C1 = SPEC + ChangeSurface; C2 = review receipt bound to exact C1; C3 = parent decomposition + Task Context Packages; C4 = per-repository Development Result plus separate independent Code Review references; C5 = a resolvable view of the fixed integrated candidate and applicable C1–C4. Do not create five mandatory files or infer nested schema fields from empty arrays.
- After Design Review, map each AC to WorkItems, repositories, tests/dependencies, and an integration owner; mark each repository `MODIFY` or `VERIFY_ONLY`. Plan Review is required only where an existing applicable policy requires review of that exact plan. Design Review does not approve a plan that does not yet exist.
- A Design Review and a Code Review are required by this selected profile. Each must be performed by a non-author and identify its review kind, exact subject/revision, applicable criteria, result, and findings. The same existing Reviewer may perform different review kinds in separate, clearly bound reviews.
- Do not dispatch coding when unresolved Product Context would change repository scope or an acceptance criterion. Continue design analysis and independent work where the gap does not affect it, and preserve the gap as unresolved evidence.
- During integration, send a new material code/config/schema diff back for affected tests and C4. Carry unchanged repository evidence only with its exact revision and an applicability rationale. Send material design/scope changes to the responsible owner and refresh authorization when required.
- Pin the integrated API/Web candidate pair and result-affecting artifacts/configuration by resolvable identity. S06 verification is independent of the code author. Final QA Review follows the actual report, coverage, findings, and required verification evidence.
- Early S06 diagnosis or reproduction may proceed on an eligible, authorized candidate without claiming S05 completion. Preserve native review (`PASS`/`WARNING`/`REVISE`), verification (`VERIFIED`/`REFUTED`/`PARTIAL`), `WorkItemResult`, `VerificationResult`, and `ControlResult` meanings. Do not infer aggregate PASS, Control SATISFIED, or Human DONE from another result.

## Existing Architect excerpt

- Resolve Product Context from the governed structured `pk/` store and cite parseable store/governance references with source, revision, authority, freshness, and uncertainty. Product Knowledge issues/comments are governance records and links; they do not replace the store. Historical evidence is a lead, not current Product truth.
- Do not treat provisional observations, MissionLearningSource, WorkspaceKnowledge, or an unconfirmed cross-repository edge as Product truth. If a missing or conflicting fact changes repository scope or AC, mark the affected conclusion unresolved and do not recommend dependent mutation; continue analysis that does not depend on it.
- Build C1 from the governing Intent/AC/context using the existing SPEC and ChangeSurface artifacts. State as-is/to-be behavior, material interfaces/flows, constraints, testable AC, `MODIFY`/`VERIFY_ONLY` repository scope, unknowns, and dependencies. Preserve a Human-approved ChangeSurface; design analysis does not grant mutation authority.
- The non-author Design Review evaluates this exact C1 for scope, impact, interfaces, dependencies, AC coverage, failure cases, and testability. Missing future code or implementation tests alone is not a design defect; do not claim implementation or correctness without evidence.
- After accepted C1/C2, contribute to C3 by mapping AC to WorkItems, repositories, tests/dependencies, and integration owner. Distinguish unknowns from facts; do not fill unresolved Product Context with guesses.

## Existing Reviewer excerpt

For each review, state `Design Review`, `Code Review`, or `Final QA Review`, the exact artifact and revision, the applicable criteria, independence from its author, verdict, evidence, and open findings. Preserve the existing parser-sensitive first-line verdict format from the assigned `code-review-method` Skill.

- **Design Review:** assess the exact design's scope, impact, interfaces, dependencies, AC coverage, failure behavior, and testability. Do not mark it `REVISE` solely because future implementation or implementation tests do not exist. Do not claim that the design proves implemented behavior.
- **Code Review:** assess the exact changed repository revision/diff, design conformance, applicable tests that were actually run, correctness, safety, and contract compatibility. Retain test requirements for implemented behavior; the design-only rule does not waive them.
- **Final QA Review:** after the final verification report and coverage/findings exist, assess the fixed candidate and required evidence. Earlier design or code verdicts do not substitute for this review; a report-only review does not require the Reviewer to implement code.
- Apply `code-review-method` from the selected RC6 runtime source (`skills/code-review-method/SKILL.md`, SHA-256 `f1ab9c1ba23a76885d7b09cf9531d59c953d0b1278a662e2055dd7a661a50679`) with its PASS test criterion and REVISE condition 2 limited to behavior claimed as implemented. Keep its remaining truthfulness, safety, revision, and independence rules. Do not silently supersede other Skill clauses.
- Bind each verdict to the reviewed revision. A changed subject makes its prior verdict stale; explicitly identify any prior tests or observations that remain applicable and why. Do not merge a Reviewer verdict with a Verifier result or turn either into Control or Human approval.
