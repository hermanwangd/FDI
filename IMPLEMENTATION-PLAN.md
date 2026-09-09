# Software Factory Implementation Plan

## Selected work

`SF-BL-002` is selected as one bounded Java correction. `SF-BL-001` remains
`BLOCKED_DEPENDENCY`; this plan does not select, clone, index, or modify an
SVSPC repository.

### Goal and construction

Recover production calls only when the receiver type is provably declared under
a configured production source root, keep external and test-helper calls out of
production mappings, and allow frozen scenarios to select known direct
production evidence as proposal-only realization seeds. Graphify may expand
only from those seeds. Existing PKB-001 evidence remains immutable.

### TDD tasks

1. **Production receiver recovery**
   - Test first in `JavaParserTestBehaviorExtractorTests`: an unresolved method
     call on a production-root receiver yields a production reference with an
     explicit fallback basis; the same shape on a test helper yields no
     production reference.
   - Implement minimally in `JavaParserTestBehaviorExtractor` and
     `RelationshipBasis`; preserve deterministic extraction and source-revision
     binding.
   - Verify with `MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=JavaParserTestBehaviorExtractorTests test`.
2. **Evidence-backed scenario mapping**
   - Test first in `ScenarioGroundedForwardMapperTests`: a scenario may select
     only an existing direct production evidence ref; unknown refs fail closed;
     unassigned scenarios remain `UNRESOLVED`; publication remains false.
   - Implement minimal proposal construction in
     `ScenarioGroundedForwardMapper`; direct seeds are `PRIMARY` and bounded
     Graphify neighbours are `SUPPORTING`, with complete trace provenance.
   - Verify with `MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=ScenarioGroundedForwardMapperTests test`.
3. **Auditable scenario-to-observation assignments**
   - Add `ScenarioObservationAssignmentGeneratorTests` first. The tests require
     one record per frozen scenario with `capabilityId`, `scenarioId`, selected
     direct-evidence refs, selected gap refs, selection rationale, source
     revision, semantics digest, test-evidence digest, and `PROPOSAL_ONLY`
     authority.
   - Implement `ScenarioObservationAssignmentGenerator` under
     `product/realization/scenarioforward`. It may rank candidates from frozen
     scenario text, test class/method identity, observed expressions, and
     mechanically resolved production symbols. It must not read evaluator
     gold, expected components, an evaluator crosswalk, or post-run metrics.
   - A selected evidence ref must exist in the exact new test-behavior evidence;
     duplicate, unknown, test-helper, external-only, mixed-revision, or
     non-production selections fail closed. A scenario with no defensible
     candidate is emitted with no direct refs and an explicit gap; it is never
     force-mapped to improve coverage.
   - Write the first new artifact to
     `validation/software-factory/sf-bl002/scenario-observation-assignments-001.json`
     with create-new/collision protection and a SHA-256 manifest entry.
   - Verify with `MAVEN_OPTS='-Xmx2g' ./mvnw -q
     -Dtest=ScenarioObservationAssignmentGeneratorTests test`.
4. **Immutable scenario mapping run**
   - Add `SfBl002ScenarioMappingRunTests` first. Cover a mapped scenario, an
     unresolved scenario, deterministic byte reproduction, changed-output
     collision, unknown assignment evidence, evaluator-vocabulary rejection,
     and semantic-publication refusal.
   - Implement `SfBl002ScenarioMappingRun` under
     `product/realization/scenarioforward`. It seals the accepted semantics,
     assignment artifact, new test-behavior evidence, exact Petclinic revision,
     and exact Graphify snapshot/expansion inputs before composing mappings.
   - Directly selected production evidence becomes `PRIMARY`; Graphify output
     can become `SUPPORTING` only through a bound relationship trace starting
     at that exact seed. No Graphify result receives formal direct credit.
   - Write only
     `validation/software-factory/sf-bl002/scenario-mapping-proposal-001.json`
     and its non-evaluator evidence manifest. Existing `validation/pkb001/`
     bytes must remain unchanged.
   - Verify with `MAVEN_OPTS='-Xmx2g' ./mvnw -q
     -Dtest=SfBl002ScenarioMappingRunTests test`.
5. **Evaluator-only hierarchical scoring**
   - Add `SfBl002HierarchicalEvaluationTests` first. The generator phase must
     complete and seal all non-evaluator inputs before evaluator truth can be
     opened. Mutating any sealed input must fail before evaluator access.
   - Reuse `HierarchicalForwardEvaluation` matching semantics; do not invent a
     capability crosswalk. Without a sealed HYP-to-PET crosswalk, capability
     alignment and scenario semantic precision/recall/F1 remain explicitly
     `NOT_COMPARABLE`, while scenario trace coverage remains descriptive.
   - Compute and report:
     - scenario trace coverage = scenarios with a non-empty realization chain /
       all 10 frozen scenarios;
     - chain coverage = one-to-one exact expected component occurrences covered
       anywhere in the realization chain / 24 evaluator occurrences;
     - exact component precision = one-to-one exact expected occurrences
       matched by `PRIMARY` proposals / proposed `PRIMARY` occurrences;
     - exact component recall = the same matches / 24 evaluator occurrences;
     - exact component F1 = `2 * precision * recall / (precision + recall)`;
       if either ratio is undefined or their sum is zero, F1 is undefined.
   - Supporting components, provider-node overlap, source-path similarity,
     containing-type similarity, and bare-symbol similarity are diagnostics
     only and receive zero formal precision/recall/F1 credit.
   - Write evaluator outputs only to
     `validation/software-factory/sf-bl002/hierarchical-evaluation-001.json`
     and `hierarchical-evaluation-evidence-001.json`, with exact input/output
     digests and create-new/collision protection.
   - Verify with `MAVEN_OPTS='-Xmx2g' ./mvnw -q
     -Dtest=SfBl002HierarchicalEvaluationTests test`.
6. **Combined verification and decision package**
   - Run `MAVEN_OPTS='-Xmx2g' ./mvnw -q clean test` and require zero failures.
   - Run every new generator twice into separate temporary roots and require
     byte-identical outputs and identical SHA-256 digests.
   - Require `git diff --exit-code -- validation/pkb001` to prove historical
     evidence was not modified.
   - Update `validation/software-factory/sf-bl002/remediation-evidence.json`
     with production-reference, scenario, chain, exact-component and diagnostic
     before/after metrics, commands, revisions, digests, limitations and an
     independent-review-ready result. This package may recommend closure but
     cannot close `SF-BL-002`.

### Negative acceptance cases

- External library calls and test-helper declarations never become production
  references or formal component credit.
- Unknown direct evidence refs, revision mismatches, duplicate assignments, and
  non-production paths fail closed.
- Scenario output remains `PROPOSAL_ONLY`; no automatic Product publication.
- Evaluator gold/crosswalk data is not an input to generation.
- No file under `validation/pkb001/` is modified.

### Active execution envelope

- Execution ID: `SF-BL-002-PRODUCTION-SCENARIO-001`
- Exact base and Spec revision: `f6ac9d052efe4d7f695c0f941a0e0b7b9c79ba81`
- Requirement bindings: `AUTH-002`, `PK-004`, `EVID-001`, `TECH-001`
- Owned paths: the Java test-behavior extractor/API, scenario-forward mapper and
  their tests, plus new `validation/software-factory/sf-bl002/` artifacts.
- Excluded paths: all `validation/pkb001/` files, evaluator-only gold/crosswalk
  inputs, Graphify Python runtime, SF-BL-001/Azure DevOps inputs, and unrelated
  framework modules.
- Resource bound: Java 17, Spring Boot 3.4.1, Maven heap at most 2 GiB; total
  command memory must remain below the workspace 8 GiB ceiling.
- Required result: combined tests/evidence plus an independent-review-ready
  integration candidate. Only Human Authority may terminally close the parent.

Tasks 3–6 above materially clarify the construction and scoring protocol. The
current execution must stop at its completed production-reference checkpoint;
a successor envelope will bind the exact commit containing this revised Plan
before scenario assignment or evaluation starts.

## Verified delivery ledger

| Delivery | Result | Evidence |
|---|---|---|
| `PKB-001` reusable prototype foundation | Preserved as immutable historical implementation/evidence; compatibility must be checked per active contract before reuse. | Git history and `validation/pkb001/` |
| `PKB-BL-026` | Repository-owned executable framework consumers migrated to Java; remaining Graphify Python is external. | `validation/pkb001/java-migration/python-framework-inventory.json` |
| `PKB-BL-027` | Exact-provenance external provider resolution and bounded Java MCP lifecycle verified. | `validation/pkb001/runtime/pkb-bl027-portable-runtime-evidence.json` |
| `PKB-BL-011` | Hierarchical metrics terminally closed before vNext reconciliation. | Candidate `17b8357e360f6d49dcfda4c80211b88e00b82d00`; Human closure commit `a99206722acbb79e36191e8b21a8b29bd4d439ed` |
| Software Factory vNext reconciliation | Five project-truth controls atomically migrated using archived candidate plus Frozen Delta Spec v2; superseded runtime schemas were not promoted. | `validation/software-factory/vnext-reconciliation-evidence.json` and the Git commit containing this ledger |

## Deferred work

After `SF-BL-002` is independently verified and terminally closed, `SF-BL-001`
remains blocked until an auditable Azure DevOps repository listing is available.
