# Software Factory Implementation Plan

## Selected work

`SF-BL-002` remains selected for one bounded effectiveness successor.
`SF-BL-001` and `SF-BL-003` remain `BLOCKED_DEPENDENCY`. This Plan does not
select, clone, index, or modify an SVSPC repository and does not select the
maintainability refactor.

### Goal and construction

Preserve the independently verified production-reference correction, then
improve evaluator-blind scenario assignment without changing frozen Product
Semantics. Generate proposal-only search intents, review and seal them, match
only mechanically resolved production evidence, and allow Graphify to expand
only from accepted `PRIMARY` seeds. Existing PKB-001 and `*-001.json` evidence
remains immutable.

### Selected successor tasks

1. **Scenario search-intent proposals**
   - Create
     `ScenarioSearchIntentProposalGenerator.java` and
     `ScenarioSearchIntentProposalGeneratorTests.java` under the existing
     `product/realization/scenarioforward` source and test packages.
   - Define one proposal per frozen scenario with `capabilityId`, `scenarioId`,
     `action`, `entity`, `conditions`, `aliases`, exact semantics digest,
     source revision, rationale and `PROPOSAL_ONLY` authority. Generation may
     read only the sealed semantics and acceptance manifest; evaluator truth,
     expected components, previous evaluation output and post-run metrics are
     prohibited inputs.
   - Write new collision-protected artifacts
     `validation/software-factory/sf-bl002/scenario-search-intent-proposals-002.json`
     and `scenario-search-intent-proposals-evidence-002.json`.
   - Test one-to-one scenario coverage, deterministic bytes, forbidden
     evaluator vocabulary, changed-output collision, mixed revision and refusal
     to publish Product truth.
   - Verify with `MAVEN_OPTS='-Xmx2g' ./mvnw -q
     -Dtest=ScenarioSearchIntentProposalGeneratorTests test`.
2. **Human review and immutable intent seal**
   - The Execution Plane returns Task 1 artifacts without starting matching.
     The Feature Delivery Plane prepares the 10-record review package; Human
     Authority accepts or rejects the generated retrieval aids without manually
     authoring them.
   - An accepted set is written once as
     `validation/software-factory/sf-bl002/accepted-scenario-search-intents-002.json`
     with an acceptance manifest binding reviewer decision, proposal digest,
     exact source revision and accepted/rejected record IDs.
   - Until this exact artifact and digest exist, Tasks 3–5 are
     `BLOCKED_DEPENDENCY`; no placeholder digest or inferred approval is valid.
3. **Deterministic PRIMARY matching**
   - Create `ScenarioSearchIntentMatcher.java` and
     `ScenarioSearchIntentMatcherTests.java`. The matcher consumes the accepted
     intent set plus exact test-behavior evidence and emits a new immutable
     `scenario-observation-assignments-002.json`.
   - Ranking uses accepted action/entity/condition/alias terms, test identity,
     observed expressions and mechanically resolved production symbols. Every
     selected ref must exist at the exact revision. Unknown, duplicate,
     external-only, test-helper, mixed-revision and non-production selections
     fail closed; insufficient evidence stays `UNRESOLVED`.
   - Test positive mapped and honest unresolved cases, deterministic ranking,
     tie handling, digest/revision mismatch, evaluator blindness and collision
     protection.
   - Verify with `MAVEN_OPTS='-Xmx2g' ./mvnw -q
     -Dtest=ScenarioSearchIntentMatcherTests test`.
4. **Bounded Graphify expansion and immutable run**
   - Create `SfBl002ScenarioEffectivenessRun.java` and its matching test. It
     seals the accepted intent set, new assignments, test evidence, exact graph
     snapshot and Graphify runtime evidence before composing proposals.
   - A mechanically selected production component is `PRIMARY`. Graphify may
     add `SUPPORTING` only through a bounded relationship trace starting from
     that exact seed. An unbound neighbour fails closed and supporting nodes
     receive zero formal PRIMARY precision credit.
   - Write only new `scenario-mapping-proposal-002.json` and its evidence
     manifest. Test a real bound expansion, an unbound neighbour, no-seed
     behavior, deterministic reproduction, evaluator-vocabulary rejection and
     semantic-publication refusal.
   - Verify with `MAVEN_OPTS='-Xmx2g' ./mvnw -q
     -Dtest=SfBl002ScenarioEffectivenessRunTests test`.
5. **Evaluator-only scoring and combined decision**
   - Create `SfBl002ScenarioEffectivenessEvaluation.java` and its matching
     tests. Seal all non-evaluator inputs before evaluator access and reuse the
     existing exact one-to-one hierarchical scoring semantics.
   - Produce new `hierarchical-evaluation-002.json`, evaluator evidence and an
     updated remediation decision package. Report scenario trace coverage,
     chain coverage, exact `PRIMARY` precision/recall/F1 and diagnostic-only
     supporting overlap. Without a sealed crosswalk, semantic scenario metrics
     remain `NOT_COMPARABLE`.
   - Acceptance requires scenario trace coverage at least `6/10`, exact chain
     recall greater than `0.0`, and exact `PRIMARY` precision at least `0.70`.
     Failure is `REVISE`; it cannot be repaired by broader guesses or evaluator
     leakage.
   - Run the focused suite, deterministic double-run, `MAVEN_OPTS='-Xmx2g'
     `./mvnw -q clean test`, and prove `validation/pkb001` plus every committed
     `*-001.json` byte is unchanged.

### Exact-input manifest

| Input | Identity | Allowed phase | Evaluator-visible | Mutable |
|---|---|---|---|---|
| accepted semantics | `validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/accepted-semantics-004.json`; SHA-256 `6c854c3d42c348d56720741b573ec88e5d6bd2dc38abb4753540ca23e8aaa9e3` | all generation | no | no |
| acceptance manifest | `validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/acceptance-manifest-004.json`; SHA-256 `1b3fbbfd210c2c0d82d74a2579c1980d5de6b047fff56a08cc2b43097c76e2a9` | all generation | no | no |
| test-behavior evidence | `validation/software-factory/sf-bl002/test-behavior-evidence.json`; SHA-256 `6260f5f3f524256bc276b4715c8560b8f0b674e62c0307d1791d2ec9e3ebc0f2` | matching onward | no | no |
| Graphify snapshot | `validation/pkb001/artifacts/petclinic-graph-818c413.json`; SHA-256 `e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e` | expansion onward | no | no |
| Graphify runtime evidence | `validation/pkb001/runtime/graphify-petclinic-live-evidence.json`; SHA-256 `fd3b6729e720e33c89c87cb987748b17ee6cc4ac1fad2c09ddbf093ab39cd5f8` | expansion onward | no | no |
| evaluator truth | existing sealed `ProviderNeutralEvaluatorTruth` input and seal | evaluation only, after non-evaluator seal | yes | no |

The accepted search-intent artifact is intentionally absent until Task 2. Its
exact path and SHA-256 must be added before Tasks 3–5 dispatch.

### Verified predecessor tasks (read-only)

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

### Superseded execution envelope

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

The execution above stopped at its completed production-reference checkpoint
after Tasks 3–6 materially clarified the construction and scoring protocol.

### Verified predecessor execution envelope

- Execution ID: `SF-BL-002-PRODUCTION-SCENARIO-002`
- Exact base and Spec revision: `13890ccff85fa7b2f79341a2c7439cac02059f87`
- Selected scope: Tasks 3–6 only; Tasks 1–2 and the production-reference
  checkpoint are read-only inputs.
- Owned paths: new Java assignment/mapping/evaluation classes and tests plus
  new or updated files under `validation/software-factory/sf-bl002/`.
- Excluded paths: all five active control files, `AGENTS.md`, every file under
  `validation/pkb001/`, evaluator inputs during generation, Graphify Python,
  SF-BL-001/Azure DevOps inputs, and unrelated framework modules.
- Execution Plane duties: decompose eligible non-overlapping slices, implement
  with TDD, independently review producer changes, integrate on one candidate,
  run combined verification, and return one evidence package. It may not close
  the parent or edit this Plan.
- Resource bound: Maven heap at most 2 GiB and total command memory below 8 GiB.
- Integrated candidate:
  `370166070fa1674658c92ca41717ab2d29d9659d`; independently reproduced
  `1060/1060` passing tests and immutable evidence; integrated onto the Feature
  Delivery branch at `b1c46cd7b70bc01aae594f5f9aa16982705646cc`.

### Active effectiveness execution envelope

- Execution ID: `SF-BL-002-SCENARIO-INTENT-003`
- Exact implementation base and Spec revision:
  `b1c46cd7b70bc01aae594f5f9aa16982705646cc`
- Selected scope: selected successor Task 1 only. Task 2 is the required Human
  review gate; Tasks 3–5 must not execute until the accepted intent artifact and
  its exact digest are added to this Plan.
- Owned paths: the new Java search-intent proposal generator and tests plus only
  new `scenario-search-intent-proposals-*-002.json` files under
  `validation/software-factory/sf-bl002/`.
- Excluded paths: all five active control files, `AGENTS.md`, existing Java
  assignment/mapping/evaluation implementation, every `*-001.json`, every file
  under `validation/pkb001/`, test-behavior evidence, evaluator inputs,
  Graphify runtime and snapshot, `SF-BL-001`, `SF-BL-003`, Azure DevOps inputs,
  and unrelated framework modules.
- Bounded-slice estimate: at most 4 owned paths, at most 500 code/test lines,
  at most 60 tool calls, one primary deliverable. Exceeding a bound requires
  `SLICE_SIZE_EXCEEDED`; it does not authorize scope expansion.
- Execution Plane duties: TDD implementation, focused verification,
  deterministic double-run, immutable-output and evaluator-vocabulary negative
  cases, independent review, and one exact-candidate evidence package. It may
  not proceed through the Human review gate, edit this Plan or close the parent.
- Resource bound: Maven heap at most 2 GiB and total command memory below 8 GiB.

### Future execution parallel-slice planning gate

This gate applies when planning successor execution envelopes. It did not alter
the completed `SF-BL-002-PRODUCTION-SCENARIO-002` evidence chain and governs
every dispatch beginning with `SF-BL-002-SCENARIO-INTENT-003`.

Before a Coordinator chooses a fully sequential DAG, it must classify each
dependency as either:

- a **construction dependency**, where one slice cannot compile or be tested
  until another slice's code or contract exists; or
- a **runtime artifact dependency**, where independently implementable code may
  be built and tested against a frozen fixture, but the final production run
  must wait for an upstream artifact.

Every successor Plan must provide a small slice matrix containing owned paths,
test paths, required inputs, produced outputs, and integration order. Slices
with non-overlapping owned paths and only runtime artifact dependencies should
be dispatched in parallel using frozen fixtures. Their outputs are integrated
once on the Coordinator-owned candidate before combined verification.

A fully sequential DAG is allowed only when the Coordinator records at least
one concrete blocker: overlapping owned paths, a shared mutable contract,
compile-time dependency, unsafe shared state, or unavailable deterministic
fixture. Task numbering or downstream data flow alone is not sufficient reason
to serialize implementation.

## Verified delivery ledger

| Delivery | Result | Evidence |
|---|---|---|
| `PKB-001` reusable prototype foundation | Preserved as immutable historical implementation/evidence; compatibility must be checked per active contract before reuse. | Git history and `validation/pkb001/` |
| `PKB-BL-026` | Repository-owned executable framework consumers migrated to Java; remaining Graphify Python is external. | `validation/pkb001/java-migration/python-framework-inventory.json` |
| `PKB-BL-027` | Exact-provenance external provider resolution and bounded Java MCP lifecycle verified. | `validation/pkb001/runtime/pkb-bl027-portable-runtime-evidence.json` |
| `PKB-BL-011` | Hierarchical metrics terminally closed before vNext reconciliation. | Candidate `17b8357e360f6d49dcfda4c80211b88e00b82d00`; Human closure commit `a99206722acbb79e36191e8b21a8b29bd4d439ed` |
| Software Factory vNext reconciliation | Five project-truth controls atomically migrated using archived candidate plus Frozen Delta Spec v2; superseded runtime schemas were not promoted. | `validation/software-factory/vnext-reconciliation-evidence.json` and the Git commit containing this ledger |
| `SF-BL-002-PRODUCTION-SCENARIO-002` | Engineering-safe production-reference and fail-closed scenario pipeline verified; experimental effectiveness remained `0/10` trace and `0/24` chain coverage, so the parent remains open. | Integrated candidate `370166070fa1674658c92ca41717ab2d29d9659d`, HERM-361 and HERM-363 independent `PASS`, and `validation/software-factory/sf-bl002/remediation-evidence.json` |

## Deferred work

`SF-BL-003` remains blocked until the `SF-BL-002` effectiveness successor is
independently verified. After `SF-BL-002` is terminally closed, `SF-BL-001`
remains blocked until an auditable Azure DevOps repository listing is available.
