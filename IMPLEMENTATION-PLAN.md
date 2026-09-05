# PKB-001 Component Mapping Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Java-owned, provider-neutral component contract and a gold-isolated hierarchical evaluator without rewriting the Graphify Python MCP runtime or changing the current bounded `REVISE` decision.

**Architecture record:** Skills generate proposal-only candidates from frozen Product Semantics and exactly bound Graphify evidence. Java validates component role, granularity, normalized identity, and revision consistency. Python evaluation described below is transitional delivery history, not the target architecture.

**Tech Stack:** Java 17, Spring Boot 3.4.1, JUnit 5, Python 3 standard library, pytest, Graphify `graphifyy` MCP stdio runtime.

**Backlog source:** `BACKLOG.md`. This document contains completed delivery
plans and future implementation outlines; backlog status, priority, dependency,
and maturity are authoritative only in `BACKLOG.md`.

**Current selection:** BL-026 first migration slice. Replace the repository-owned
scenario-forward Python gate with a Java API/CLI, cut over its active callers,
then remove that Python consumer. Existing Python plan sections below are
completed or transitional delivery records and do not authorize new Python
framework behavior.

---

## Selected work: BL-026 Java scenario-forward gate migration

**Goal:** Replace `tooling/validation/pkb001_scenario_forward_gate.py` with a
Java 17 API and CLI that preserves its fail-closed v0.3 contract, then remove
the replaced Python module and its direct Python tests.

**Spec binding:** `FRAMEWORK-SPEC.md` at
`891e497968000c32984f26437eab811c063ec4cf`.

**Selected backlog:** `PKB-BL-026`, first bounded consumer only. Completing
this plan advances but does not close BL-026; remaining Python consumers stay
in the same backlog item for later implementation-plan revisions.

**Architecture:** Java owns request parsing, schema and domain validation,
trusted-root file reads, digest/revision binding, Graphify-evidence checks,
run-ID collision checks and the JSON report. It consumes existing Graphify
evidence only; the external Graphify Python MCP runtime stays unchanged. The
public result remains `CONTRACT_VALID` or `BLOCKED`, with empty `mappings` and
no evaluator inputs.

**Tech stack:** Java 17, Spring Boot 3.4.1, Jackson, networknt JSON Schema Draft
2020-12, JUnit 5 and Maven. Python/pytest is used only to characterize the old
consumer before cutover.

### Task 1: Freeze inventory and observable behavior

**Files:**

- Create: `validation/pkb001/java-migration/python-framework-inventory.json`
- Read: `tooling/validation/pkb001_scenario_forward_gate.py`
- Read: `tests/test_pkb001_scenario_forward.py`

- [ ] Record every active `tooling/validation/*.py` file with `path`,
  `responsibility`, `active_callers`, `migration_state`, and `external_runtime`.
  Mark only the scenario-forward gate `SELECTED`; mark other repository Python
  modules `TRANSITIONAL`. Record Graphify as an external MCP runtime rather than
  a repository Python consumer.
- [ ] Record all 36 shared fixture cases and this stable public report boundary
  in the inventory as the characterization target for the later Java tests:

```java
assertThat(fixtures).hasSize(36);
assertThat(valid.keySet()).containsExactlyInAnyOrder(
        "status", "reasons", "mappings", "run_id", "generation_inputs");
assertThat(valid.get("status")).isEqualTo("CONTRACT_VALID");
assertThat(valid.get("mappings")).isEqualTo(List.of());
assertThat(blocked.get("status")).isEqualTo("BLOCKED");
assertThat(blocked.get("generation_inputs")).isEqualTo(List.of());
```

- [ ] Run `python3 -m pytest -q tests/test_pkb001_scenario_forward.py` before
  replacement. Save the exact passing count as `characterization_test_count`;
  never weaken a rejected case for Java parity.
- [ ] Commit the passing characterization inventory as
  `test(fdi): characterize scenario forward migration`.

### Task 2: Implement bounded Java input handling

**Files:**

- Create: `src/main/java/com/featuredeliveryintelligence/fdi/validation/scenarioforward/ScenarioForwardRequestReader.java`
- Create: `src/main/java/com/featuredeliveryintelligence/fdi/validation/scenarioforward/ScenarioForwardRequest.java`
- Create: `src/main/java/com/featuredeliveryintelligence/fdi/validation/scenarioforward/ScenarioForwardReport.java`
- Create: `src/test/java/com/featuredeliveryintelligence/fdi/validation/scenarioforward/ScenarioForwardRequestReaderTests.java`

- [ ] Write failing tests for canonical relative paths, absolute/traversal and
  Windows paths, symlink files/directories, non-regular files, inputs over 8
  MiB, malformed JSON, duplicate keys, non-object roots and post-read mutation.
- [ ] Run `MAVEN_OPTS='-Xmx2g' ./mvnw -q
  -Dtest=ScenarioForwardRequestReaderTests test`; expect compilation failure
  because the reader is absent.
- [ ] Implement this public boundary. Jackson enables strict duplicate-key
detection; path inspection uses `NOFOLLOW_LINKS`; parsed trees are copied.

```java
public record ScenarioForwardRequest(List<BoundInput> inputs, JsonNode proposal) {
    public record BoundInput(String kind, String path, String sha256) {}

    public ScenarioForwardRequest {
        inputs = List.copyOf(inputs);
        proposal = proposal.deepCopy();
    }
}

public final class ScenarioForwardRequestReader {
    public static final long MAX_BYTES = 8L * 1024 * 1024;
    public ScenarioForwardRequest read(Path trustedRoot, Path requestPath);
    public byte[] readBoundFile(Path trustedRoot, String relativePath);
    public static String sha256(byte[] bytes);
    public static boolean canonicalRelative(String value);
}
```

- [ ] Implement an immutable report which sorts/deduplicates reasons, always
  keeps mappings empty, and removes generation inputs whenever blocked:

```java
public record ScenarioForwardReport(
        Status status, List<String> reasons, List<Object> mappings,
        String runId, List<GenerationInput> generationInputs) {
    public enum Status { CONTRACT_VALID, BLOCKED }
    public record GenerationInput(String kind, String path, String sha256) {}
}
```

- [ ] Rerun the focused tests; expect zero failures. Commit as
  `feat(fdi): add bounded scenario forward input reader`.

### Task 3: Port contract and provenance validation

**Files:**

- Create: `src/main/java/com/featuredeliveryintelligence/fdi/validation/scenarioforward/ScenarioForwardGate.java`
- Create: `src/test/java/com/featuredeliveryintelligence/fdi/validation/scenarioforward/ScenarioForwardCharacterizationTests.java`
- Create: `src/test/java/com/featuredeliveryintelligence/fdi/validation/scenarioforward/ScenarioForwardGateTests.java`
- Reuse: `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/ScenarioRealizationProposal.java`
- Reuse: `validation/pkb001/schemas/realization-proposal-v0.3.schema.json`
- Reuse: `validation/pkb001/fixtures/scenario-forward-parity.json`

- [ ] Write failing parameterized tests for every existing reason family:
  request/input shape; digests and selected versions; JSON Schema and Java
  invariants; reviewed-semantics consistency; Graphify exact-revision evidence;
  graph references; and run-ID uniqueness.
- [ ] Implement one fail-closed entry point:

```java
public final class ScenarioForwardGate {
    public static final String SCHEMA_PATH =
            "validation/pkb001/schemas/realization-proposal-v0.3.schema.json";
    public static final String SKILL_PATH =
            "skills/pkb001/pk-s1-product-realization-v0.3/SKILL.md";
    public ScenarioForwardReport validate(Path trustedRoot,
                                          ScenarioForwardRequest request);
}
```

Use networknt Draft 2020-12 validation before deserializing capability results
to `ScenarioRealizationProposal`. Preserve existing public reason codes. Map
unexpected runtime input to `REQUEST_INVALID`; never return stack traces,
evaluator content or partially validated generation inputs.
- [ ] Run `MAVEN_OPTS='-Xmx2g' ./mvnw -q
  -Dtest=ScenarioForwardParityTests,ScenarioForwardGateTests,ScenarioForwardCharacterizationTests
  test`. Expect all Java tests to pass and all 36 fixtures to retain the same
  decision. Commit as `feat(fdi): port scenario forward gate to Java`.

### Task 4: Expose the Java CLI

**Files:**

- Create: `src/main/java/com/featuredeliveryintelligence/fdi/application/ScenarioForwardCli.java`
- Modify: `src/main/java/com/featuredeliveryintelligence/fdi/application/FdiApplication.java`
- Create: `src/test/java/com/featuredeliveryintelligence/fdi/application/ScenarioForwardCliTests.java`
- Create: `validation/pkb001/fixtures/scenario-forward-valid-request.json`

- [ ] Write failing process-level tests for
  `scenario-forward-validate --root <dir> --request <json>`, including missing,
  duplicate and unknown options, valid and blocked reports, deterministic JSON,
  and absence of stack traces.
- [ ] Implement the dispatcher and invoke it before Spring startup:

```java
public final class ScenarioForwardCli {
    public static boolean handles(String[] args) {
        if (args.length == 0 || !"scenario-forward-validate".equals(args[0])) {
            return false;
        }
        if (args.length != 5) throw new IllegalArgumentException(
                "usage: scenario-forward-validate --root <dir> --request <json>");
        Map<String, String> options = new LinkedHashMap<>();
        for (int index = 1; index < args.length; index += 2) {
            if (!Set.of("--root", "--request").contains(args[index])
                    || options.put(args[index], args[index + 1]) != null) {
                throw new IllegalArgumentException("invalid or duplicate option");
            }
        }
        Path root = Path.of(required(options, "--root"));
        Path requestPath = Path.of(required(options, "--request"));
        ScenarioForwardRequest request = new ScenarioForwardRequestReader()
                .read(root, requestPath);
        ScenarioForwardReport report = new ScenarioForwardGate()
                .validate(root, request);
        try {
            System.out.println(new ObjectMapper().writeValueAsString(report));
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("cannot serialize validation report", failure);
        }
        return true;
    }

    private static String required(Map<String, String> options, String key) {
        String value = options.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing " + key);
        }
        return value;
    }
}
```

```java
public static void main(String[] args) {
    if (Dev204Cli.handles(args) || ScenarioForwardCli.handles(args)) return;
    SpringApplication.run(FdiApplication.class, args);
}
```

- [ ] Run the focused CLI test and `MAVEN_OPTS='-Xmx2g' ./mvnw -q package`;
  expect zero failures. Smoke-test the packaged JAR with the frozen valid
  request; expect one `CONTRACT_VALID` JSON object, empty mappings, and exactly
  the three allowed generation-input kinds. Commit as
  `feat(fdi): expose Java scenario forward validation CLI`.

### Task 5: Cut over and remove only the replaced Python consumer

**Files:**

- Modify: `skills/pkb001/pk-s1-product-realization-v0.3/SKILL.md`
- Modify: `validation/pkb001/java-migration/python-framework-inventory.json`
- Delete: `tooling/validation/pkb001_scenario_forward_gate.py`
- Delete: `tests/test_pkb001_scenario_forward.py`
- Modify: `BACKLOG.md`, `STATUS.json`, `IMPLEMENTATION-PLAN.md`

- [ ] Replace every active import/invocation of the Python scenario-forward
  gate with the packaged Java CLI. Search may retain only explicitly historical
  plan text before deletion.
- [ ] Mark the inventory entry `MIGRATED_TO_JAVA`, record the Java API/CLI and
  verification evidence, then delete only the replaced module and direct test.
  Do not delete other Python consumers or modify external Graphify.
- [ ] Run the complete verification set:

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw clean package
python3 -m pytest -q
python3 validation/pkb001/task7-evaluation/public_validate.py .
```

Expected: Java build, remaining transitional Python regression and public
validation all pass; no active caller imports the deleted module.
- [ ] Update BL-026 progress and `STATUS.json` with test counts and commit IDs.
Keep BL-026 active and `PKB-JAVA-001` below M3 because other Python consumers
remain. Commit as `refactor(fdi): cut scenario forward validation over to Java`.

### Plan acceptance boundary

- Java preserves all 36 shared fixture decisions and every security/binding
  family covered by the old gate.
- Active PK-S1 calls Java; the replaced Python gate and its direct test are gone.
- External Graphify remains Python over MCP and is not embedded or rewritten.
- Historical evidence artifacts remain byte-identical.
- BL-026 remains open for later consumers; no child Backlog Items are created.

---

## Existing prototype foundation

## 1. Product Semantics input

Define a small, human-owned Capability set with stable identifiers, descriptions, expected realization boundaries, and evaluator-only expected mappings. Machine output must not overwrite it.

## 2. Graphify exact-revision integration

Inspect the installed Graphify runtime and record its identity, version, transport, and actual supported operations. Keep provider-native operations behind `CodeIntelligenceProvider` and the Graphify adapter. Open a frozen workspace at a full Git commit SHA, prove node and bounded-path queries, and attest that the indexed revision equals the requested revision.

## 3. Delivery History reconstruction

Collect Git commits, pull requests, changed paths, and traceable feature-delivery evidence only up to the calibration cutoff. Preserve source references and uncertainty; do not infer Product truth from history alone.

## 4. Forward experiment

For each known Capability, combine Product Semantics with exact-revision structural observations to propose Capability-to-Component mappings. Score mappings against evaluator-only expected realizations.

A deterministic code implementation may be retained as a baseline, but it must be labeled `CODE_BASELINE` and must not be represented as PK-S1 or PK-S2 Skill execution.

## 5. Reverse experiment

Hide Product Semantics from the reverse arm. Combine structural observations with delivery evidence to produce proposal-only Capability hypotheses with evidence references, confidence, and limitations.

## 6. Human/evaluator comparison

Apply deterministic label/order blinding while keeping ground truth isolated from generation. This removes explicit labels and source identifiers but does not provide content-level arm anonymity: `ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT`. Review proposals using a frozen judgment vocabulary and record evidence validity, usefulness, unsupported claims, precision, and review time.

## 7. GO / REVISE / STOP decision

- **GO:** exact snapshot binding and evidence validity pass, forward mappings are useful, reverse proposals meet the frozen acceptance thresholds, and completed Human Reviewer human review approves the experiment decision. A `GO` decision does not itself publish semantics; formal semantic publication is outside this prototype.
- **REVISE:** execution remains safe and evidence-valid, but quality thresholds are missed or were not pre-registered in time to support `GO`.
- **STOP:** snapshot identity, isolation, evidence integrity, or Product-truth boundaries are violated.

No R1/R2/R3/F1 experiment run begins until all six Phase 0 readiness flags pass: Product Semantics frozen, live Graphify interface verified, PK-S1 ready, PK-S2 ready with cutoff-bounded Delivery History, calibration dataset frozen, and evaluator ground truth sealed and isolated.

**Execution result:** Phase 0 and both Petclinic experiment executions passed their binding and evidence-integrity checks. Two isolated non-human evaluator judgments cover all 15 blind items. The bounded decision is `REVISE` because numeric acceptance thresholds were not pre-registered; observed metrics are not backfit into a `GO` gate. Eleven action/outcome disagreements are listed for independent third review. Human Reviewer review remains pending and no Product Semantics may be published from this run.

**Next experiment:** implement generated scenario proposals and individual review, then freeze user-accepted experiment inputs. Verify UI/template support and preserve evidence gaps. The completed component foundation and Petclinic artifacts remain unchanged.

---

## Selected work: generated scenarios and individual review

This section records completed construction work. The BL-007 section below is a
transitional delivery record; BL-026 above is the current selected plan.

## Selected work: scenario-aware Forward contract

**Goal:** Prepare a versioned contract and input gate for the accepted Capability
001 and Scenarios 001/002, without generating mappings in this plan.
**Spec binding:** `06861c4575c4791f3aa6c262f5f0f4c45c2c2d75`.
**Selected backlog:** `PKB-BL-007`, bounded contract construction only.
**State:** Bounded Tasks A–D implementation and verification complete. No Forward
generation has executed. Full BL-006 and experiment
gates remain incomplete; their status is not cleared by first-slice acceptance.
**Architecture:** Java owns trace and component validation. PK-S1 v0.3 supplies
proposal-only semantic selection instructions. Python verifies frozen inputs,
schema parity and isolation; it does not infer mappings or approve semantics.
**Tech stack:** Existing Java 17/JUnit and Python/pytest/jsonschema; no new runtime.

### Task A: Java scenario trace contract

Files to create under `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/`:
`ScenarioRealizationProposal.java`, with nested immutable records for a component
reference, scenario trace and chain step. Reuse `RealizationComponent` and
`StructuralComponentIdentity`; do not change their historical constructors.
Test: matching `src/test/java/com/featuredeliveryintelligence/fdi/product/realization/ScenarioRealizationProposalTests.java`.

- [x] Write failing tests for ordered variable-length chains, both accepted
  scenario IDs, dangling/duplicate component refs, unused components, cross-parent
  scenarios, revision mismatch, nulls and defensive copying.
- [x] Run `MAVEN_OPTS='-Xmx2g' ./mvnw -Dtest=ScenarioRealizationProposalTests test -q`;
  first expect missing-contract failure, then implement the contract and rerun.
- [x] Define `outcome` independently from `evidence_status`; step states are
  `EVIDENCED`, `EVIDENCE_GAP`, `NOT_APPLICABLE`. Require evidence refs for evidenced
  steps, explicit gap text for gaps and a reason for not-applicable steps.
  Not-applicable assertions remain proposal-only. Mapping requires a PRIMARY;
  unresolved results contain no proposed components. Every proposed component
  must have a local ref, selection reason and actual chain use. Replacing direct
  method evidence with its containing class or file requires an explicit reason.
- [x] Add negative Java tests for each rule. Cross-language parity fixtures were
  subsequently verified in Task B below.

Task A verification (2026-09-05): 16 focused tests and 58 full Java tests pass.
RED was observed for the absent contract, COMPLETE-with-gap inconsistency and
missing FILE replacement guard before implementation. Verification used a clean
git-archive copy at `/tmp/fdi-task-a.afqEac` with exact candidate Java files and
`MAVEN_OPTS='-Xmx2g'`, avoiding workspace report-file stalls.
Java compilation targets release 17; the test JVM was the installed JDK 23.0.2,
so this is not a claim of a separate JDK 17 runtime test. The contract exposes
`BoundScenario`, `ComponentReference`, `ScenarioTrace` and `ChainStep`; it validates
declared membership and references, not actual graph evidence or reviewer approval.
The later frozen-input gate must authenticate those caller-supplied assertions.
Python regression: 306 passed; legacy public validation: 9/9. Two stale tests
from the preceding approved document update were aligned to the active plan and
version-selection wording; no runtime or historical artifact was changed.

### Task B: Versioned schema and skill

Create `validation/pkb001/schemas/realization-proposal-v0.3.schema.json`,
`skills/pkb001/pk-s1-product-realization-v0.3/SKILL.md`, and
`tests/test_pkb001_scenario_forward.py`. Preserve v0.2 bytes.

- [x] Write schema fixtures before implementation: a two-scenario mapping,
  an unresolved result and a partial result with a UI evidence gap.
- [x] Use this envelope shape (fixture values are not an experiment result):

```json
{"schema_version":"pkb001.realization-proposal.v0.3",
 "authority":"PROPOSAL_ONLY", "run_id":"fixture-only",
 "source_revision":"<full Git SHA>", "graph_sha256":"<SHA-256>",
 "semantics_sha256":"<SHA-256>", "capability_results":[]}
```

- [x] Require nonempty capability results; each result contains capability ID,
  outcome, evidence status, components, scenario traces and limitations. Schema
  uses Draft 2020-12, rejects unknown fields, and mirrors Task A. Cross-reference
  checks remain executable, not falsely claimed as JSON Schema guarantees.
- [x] Skill inputs are only frozen accepted semantics and exactly bound graph
  evidence. Prohibit evaluator truth, review evidence envelopes, delivery history
  and judgments from the Forward generation context. Do not invent UI edges;
  unsupported observations produce gaps. Confidence is not a calibrated probability.
- [x] Run `python3 -m pytest -q tests/test_pkb001_scenario_forward.py`; require
  matching Java/Python acceptance for all parity fixtures before committing.

### Task C: Frozen-input gate without generation

Create `tooling/validation/pkb001_scenario_forward_gate.py`; extend the new test
file. Keep `pkb001_next_run_gate.py` and its v0.2 input contract unchanged.

- [x] Test the public entry point before implementation:

```python
def test_unreviewed_input_blocks(valid_request, root):
    valid_request["semantics"]["status"] = "DRAFT"
    result = validate_scenario_forward(root, valid_request)
    assert result["status"] == "BLOCKED"
    assert result["mappings"] == []
```

- [x] Implement `validate_scenario_forward(root, request)` with deterministic
  sorted reasons. Request supplies digest-bound paths for semantics, acceptance
  manifest, decisions, original proposal, graph binding, graph, schema and skill.
  Verification may inspect review provenance; generation receives only a separate
  semantics/graph/skill allowlist, never those review artifacts.
- [x] Verify exact accepted text against decisions and proposal digests, parent
  linkage, frozen status, HUMAN_REVIEWER ownership, snapshot/manifest binding,
  source revision and actual graph bytes. Require exact v0.3 skill/schema digests.
  Reuse safe read/snapshot principles of the existing gate; reject symlink escapes,
  absolute/traversal paths, malformed JSON, hostile shapes and oversized inputs.
- [x] Test tampered text/digest, rejected/unconfirmed edit, pending scenario,
  wrong owner/parent/revision, missing provider binding, duplicate/unknown inputs,
  forbidden paths, unsupported schema validator and existing run ID. Every failure
  returns BLOCKED and no mappings. Contract success is `CONTRACT_VALID`, not
  experiment READY; the gate never executes a skill or writes a proposal.
- [x] Run the focused tests and commit gate/tests only after all mutations pass.

### Task D: Verification and handoff

- [x] Run `python3 -m pytest -q`,
  `MAVEN_OPTS='-Xmx2g' ./mvnw test -q`,
  `python3 validation/pkb001/task7-evaluation/public_validate.py .`, and
  `git diff --check`. Keep total memory below 8 GB and run suites sequentially.
- [x] Verify no diff to existing skills, frozen graph, accepted semantics,
  decisions or prior experiment artifacts. If workspace Maven files stall, use
  a clean temporary source checkout with the same candidate changes and record
  that verification location; do not delete workspace files to unblock it.
- [x] Record implementation commits and actual test counts in this plan. Update
  BL-007 progress only for delivered scope; do not mark full experiment ready.
  Keep REVISE and the other 13 review decisions unchanged.

Tasks B–D verification (2026-09-05), implementation commit
`c12e2f7cf03d23cf5869fb418d7e40f9e6bd7368`:

- 109 focused Python tests; 415 complete Python tests; 94 Java tests including
  36 shared Java/Python parity fixtures; legacy public validation 9/9.
- Actual repository accepted-slice verification returns `CONTRACT_VALID` with
  empty mappings using a synthetic unresolved candidate, not a generated mapping.
- Independent spec and code-quality reviews passed after fixing missing and
  contradictory captured-query evidence, malformed-symbol parity and boolean
  revision confusion. All fixes have regression tests.
- Skill format validation passed. A synthetic boundary exercise refused generation
  with evaluator/history input and without experiment authorization.
- Maven ran sequentially with `MAVEN_OPTS='-Xmx2g'` in
  `/tmp/fdi-task-a.afqEac`, synchronized to the candidate Java test/fixture bytes.
  Compilation targets Java 17; the actual test JVM is JDK 23.0.2.
- No completed skill, frozen evidence, accepted semantics or review decision was
  changed. Next-experiment readiness remains `NOT_READY`; BL-007's full dependency
  scope is not promoted merely because its bounded implementation passed.

The public API is `validate_scenario_forward(root, request)`, with
`request = {"inputs": [...], "proposal": {...}}`. Eight exact input kinds are
declared in `KINDS` in the gate. Contract fixtures and
`test_actual_repository_accepted_slice_is_contract_valid` demonstrate the complete
request. The result exposes only `PRODUCT_SEMANTICS`, `FROZEN_GRAPH`, and
`PKS1_SKILL` to a fresh generation context. It validates trusted review-artifact
consistency, not the authenticity of a human identity string or semantic correctness
of a selected component. It provides no generation writer or experiment permission.

Out of scope: actual mapping generation, evaluator execution, threshold setting,
holdout selection, provider installation, template extraction and semantic publication.

## Completed scenario-generation construction record

**Spec binding:** `FRAMEWORK-SPEC.md` at `eff92e0f7c2e41cd9880c33655ff23df796a5830`.
**Selected backlog:** `PKB-BL-005` and `PKB-BL-023`.
**State:** BL-005/023 implementation verified; BL-024 review pointer verified.
BL-025 is partially reviewed: the user accepted Capability 001 and Scenarios
001/002, now frozen as a separate first-slice snapshot. The other 13 decisions
remain pending. BL-007's bounded construction plan is now recorded above;
its scenario-aware Forward input migration is not yet implemented.
Human acceptance exists only for the three first-slice items recorded in
`STATUS.json` and its acceptance manifest; all other decisions remain pending.

Execution inputs are the existing frozen Petclinic graph (140 nodes, 142
links), exact-revision Graphify binding, and delivery history (1,042 commits,
91 PR records) through `2026-08-26T10:57:54Z`. The scenario generation context
is separate from prior evaluator contexts and reads only these allowed inputs.
This is preparation for human review, not a Forward run or a new GO decision.

1. Add the Java scenario proposal and review decision contracts under
   `src/main/java/com/featuredeliveryintelligence/fdi/product/semantics/`,
   with tests in the matching test directory. Define behavior fields separately
   from evidence refs, confidence/rationale/limitations, revision/digest binding,
   UNREVIEWED proposals and version-bound ACCEPT/EDIT/REJECT decisions.
   Validate that rejected proposals and unconfirmed edits cannot be frozen.
2. Add `validation/pkb001/schemas/scenario-proposal.schema.json` mirroring
   that contract. Cover missing evidence bindings, unsupported confidence,
   semantic/technical field mixing and malformed review records. The existing
   v0.2 readiness gate remains a historical contract; extending it for reviewed
   scenario input belongs to BL-007, not an owner-string replacement.
3. Add `skills/pkb001/pk-scenario-proposal/SKILL.md` for bounded generation.
   Allowed inputs are exact-revision structural evidence and cutoff-bounded
   delivery history plus their manifests. Reject evaluator gold, judgments and
   accepted Forward semantics. Cite evidence per behavioral claim; disclose
   missing channels and unsupported hypotheses; emit proposal-only output.
4. Add `tooling/validation/pkb001_scenario_review.py` to validate proposals and
   deterministically render JSON and Markdown review material. Generation of
   semantic text is performed by the skill/agent; the Python utility does not
   invent semantic conclusions. Render Given/When/Then, evidence, inference
   reason, confidence caveat and editable decision fields. Leave decisions empty.
5. Verify with focused Java tests using `MAVEN_OPTS='-Xmx2g'` and focused
   Python tests under `tests/test_pkb001_scenario_review.py`. Exercise missing/
   forged references, edited-version mismatch, REJECT filtering, gold isolation,
   duplicate run IDs and preservation of existing Petclinic bytes.
6. Produce a fresh Petclinic proposal artifact only after these checks pass;
   mark reconstruction consistency and reviewer exposure. Record evidence in
   BL-005/023. Select BL-024 to point status to the resulting material; BL-025
   waits for the user's actual review. Freezing and Forward execution remain
   dependent work, not implicit effects of rendering the packet.

Acceptance delivered: a reviewable evidence-backed proposal surface, no invented
human decisions, and tested contract enforcement. Implementation commits:
`fc7b304`, `f5d8bc6`. Evidence and command results are recorded in
`validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/verification.json`.
The generated packet contains 6 Capabilities / 10 scenarios / 48 atomic evidence
references. Java full-suite verification uses a clean Git archive in `/tmp`
because existing worktree test reports are iCloud-dataless; the test JVM is
JDK 23.0.2 with Java 17 compilation target and `MAVEN_OPTS='-Xmx2g'`.

## Contract improvement tasks

Tasks 1–5 below are the completed foundation ledger. Their PRODUCT_TEAM owner
and earlier execution notes describe the frozen v0.2 contract, not the new
individual-review workflow. Next-run migration is explicitly planned above.


### Task 1: Java structural component identity

**Files:**

- Create: `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/StructuralComponentIdentity.java`
- Test: `src/test/java/com/featuredeliveryintelligence/fdi/product/realization/StructuralComponentIdentityTests.java`

- [x] **Step 1: Write failing constructor-validation tests**

```java
@Test void acceptsExactMethodIdentity() {
    var identity = new StructuralComponentIdentity(
        REVISION,
        "src/main/java/example/OwnerController.java",
        StructuralComponentIdentity.Granularity.METHOD,
        "example.OwnerController.processFindForm",
        "ownercontroller_ownercontroller_processfindform");
    assertEquals(StructuralComponentIdentity.Granularity.METHOD, identity.granularity());
}

@Test void rejectsAbbreviatedRevisionAndAbsolutePath() {
    assertThrows(RuntimeContractException.class, () ->
        new StructuralComponentIdentity("818c413", "/tmp/OwnerController.java",
            StructuralComponentIdentity.Granularity.TYPE,
            "example.OwnerController", "ownercontroller"));
}

@Test void requiresQualifiedSymbolForSymbolGranularity() {
    assertThrows(RuntimeContractException.class, () ->
        new StructuralComponentIdentity(REVISION, "src/OwnerController.java",
            StructuralComponentIdentity.Granularity.METHOD, "", "node-1"));
}
```

- [x] **Step 2: Verify RED**

Run: `MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=StructuralComponentIdentityTests test`

Expected: compilation failure because `StructuralComponentIdentity` does not exist.

- [x] **Step 3: Implement the immutable identity contract**

```java
public record StructuralComponentIdentity(
        String sourceRevision,
        String sourcePath,
        Granularity granularity,
        String qualifiedSymbol,
        String providerNodeId) {
    public enum Granularity { REPOSITORY, FILE, TYPE, METHOD, TEMPLATE, CONFIGURATION }

    public StructuralComponentIdentity {
        if (sourceRevision == null || !sourceRevision.matches("[0-9a-f]{40}"))
            throw new RuntimeContractException("sourceRevision must be a full Git SHA");
        if (sourcePath == null || sourcePath.isBlank() || sourcePath.startsWith("/")
                || sourcePath.contains("\\")
                || Arrays.asList(sourcePath.split("/")).contains(".."))
            throw new RuntimeContractException("sourcePath must be repository-relative");
        Objects.requireNonNull(granularity, "granularity");
        if (EnumSet.of(Granularity.TYPE, Granularity.METHOD,
                Granularity.TEMPLATE, Granularity.CONFIGURATION).contains(granularity)
                && (qualifiedSymbol == null || qualifiedSymbol.isBlank()))
            throw new RuntimeContractException("qualifiedSymbol is required");
        if (providerNodeId == null || providerNodeId.isBlank())
            throw new RuntimeContractException("providerNodeId is required");
    }
}
```

- [x] **Step 4: Verify GREEN and regression safety**

Run: `MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=StructuralComponentIdentityTests test`

Expected: all `StructuralComponentIdentityTests` pass.

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/featuredeliveryintelligence/fdi/product/realization/StructuralComponentIdentity.java src/test/java/com/featuredeliveryintelligence/fdi/product/realization/StructuralComponentIdentityTests.java
git commit -m "feat(pkb001): add structural component identity"
```

### Task 2: Java realization proposal contract

**Files:**

- Create: `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/RealizationComponent.java`
- Create: `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/RealizationProposal.java`
- Test: `src/test/java/com/featuredeliveryintelligence/fdi/product/realization/RealizationProposalTests.java`

- [x] **Step 1: Write failing role and proposal-boundary tests**

```java
@Test void mappingRequiresPrimaryComponent() {
    var supporting = new RealizationComponent(Role.SUPPORTING, identity, "Nearby type");
    assertThrows(RuntimeContractException.class, () ->
        new RealizationProposal("PET-CAP-01", Outcome.MAPPING_PROPOSAL,
            REVISION, List.of(supporting), List.of("bounded evidence")));
}

@Test void unresolvedRequiresNoComponents() {
    assertDoesNotThrow(() -> new RealizationProposal(
        "PET-CAP-10", Outcome.UNRESOLVED, REVISION, List.of(),
        List.of("template evidence unavailable")));
}

@Test void rejectsMixedRevisionComponents() {
    assertThrows(RuntimeContractException.class, () ->
        new RealizationProposal("PET-CAP-01", Outcome.MAPPING_PROPOSAL,
            OTHER_REVISION, List.of(primary), List.of("evidence")));
}
```

- [x] **Step 2: Verify RED**

Run: `MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=RealizationProposalTests test`

Expected: compilation failure because the proposal types do not exist.

- [x] **Step 3: Implement minimal records and enums**

```java
public record RealizationComponent(
        Role role, StructuralComponentIdentity identity, String selectionReason) {
    public enum Role { PRIMARY, SUPPORTING }
    public RealizationComponent {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(identity, "identity");
        if (selectionReason == null || selectionReason.isBlank())
            throw new RuntimeContractException("selectionReason is required");
    }
}

public record RealizationProposal(
        String capabilityId, Outcome outcome, String sourceRevision,
        List<RealizationComponent> components, List<String> limitations) {
    public enum Outcome { MAPPING_PROPOSAL, UNRESOLVED }
    public RealizationProposal {
        components = List.copyOf(components);
        limitations = List.copyOf(limitations);
        boolean hasPrimary = components.stream().anyMatch(
            item -> item.role() == RealizationComponent.Role.PRIMARY);
        if (outcome == Outcome.MAPPING_PROPOSAL && !hasPrimary)
            throw new RuntimeContractException("mapping proposal requires PRIMARY component");
        if (outcome == Outcome.UNRESOLVED && !components.isEmpty())
            throw new RuntimeContractException("unresolved proposal cannot contain components");
        if (components.stream().anyMatch(item ->
                !sourceRevision.equals(item.identity().sourceRevision())))
            throw new RuntimeContractException("component revision mismatch");
    }
}
```

- [x] **Step 4: Verify GREEN and full Java tests**

Run: `MAVEN_OPTS='-Xmx2g' ./mvnw test -q`

Expected: proposal tests and all existing Java tests pass.

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/featuredeliveryintelligence/fdi/product/realization src/test/java/com/featuredeliveryintelligence/fdi/product/realization
git commit -m "feat(pkb001): enforce realization proposal boundaries"
```

### Task 3: PK-S1 output contract and gold isolation

**Files:**

- Preserve unchanged: `skills/pkb001/pk-s1-product-realization/SKILL.md` (immutable historical Petclinic input)
- Create: `skills/pkb001/pk-s1-product-realization-v0.2/SKILL.md`
- Modify: `tests/test_pkb001_execution_classification.py`

The original PK-S1 skill is a byte-bound input to the completed Petclinic run and must remain unchanged. The component contract applies to the versioned v0.2 skill for the next run.

- [x] **Step 1: Add a failing skill-contract test**

```python
def test_pk_s1_requires_typed_primary_and_supporting_components_without_gold():
    text = (ROOT/'skills/pkb001/pk-s1-product-realization-v0.2/SKILL.md').read_text()
    assert '`PRIMARY`' in text and '`SUPPORTING`' in text
    assert all(f'`{value}`' in text for value in
               ('REPOSITORY', 'FILE', 'TYPE', 'METHOD', 'TEMPLATE', 'CONFIGURATION'))
    assert 'A containing class or file must not replace a direct method node' in text
    assert 'evaluator gold' in text and 'MUST NOT' in text
```

- [x] **Step 2: Verify RED**

Run: `python3 -m pytest -q tests/test_pkb001_execution_classification.py::test_pk_s1_requires_typed_primary_and_supporting_components_without_gold`

Expected: failure because the versioned v0.2 skill does not exist yet.

- [x] **Step 3: Create versioned PK-S1 v0.2 with the exact output requirements**

```markdown
## Component output contract

Each component MUST include `role`, `granularity`, `source_revision`,
repository-relative `source_path`, `qualified_symbol`, `provider_node_id`,
and `selection_reason`. A mapping has at least one `PRIMARY` component.
`SUPPORTING` evidence cannot substitute for a directly evidenced method.
A containing class or file must not replace a direct method node.
PK-S1 MUST NOT read evaluator gold, sealed expected mappings, judgments, or
post-generation comparison results.
```

- [x] **Step 4: Verify GREEN and governance regressions**

Run: `python3 -m pytest -q tests/test_pkb001_execution_classification.py tests/test_pkb001_phase0.py`

Expected: all selected tests pass.

- [x] **Step 5: Commit**

```bash
git add skills/pkb001/pk-s1-product-realization-v0.2/SKILL.md tests/test_pkb001_execution_classification.py IMPLEMENTATION-PLAN.md
git commit -m "fix(pkb001): version the next-run PK-S1 contract"
```

### Task 4: Gold-isolated hierarchical comparison utility

**Files:**

- Create: `tooling/validation/pkb001_component_compare.py`
- Create: `tests/test_pkb001_component_compare.py`

- [x] **Step 1: Write failing independent metric and channel tests**

```python
def test_comparison_keeps_all_hierarchical_levels_separate():
    proposed = [{'source_path': 'OwnerController.java',
                 'containing_type': 'OwnerController',
                 'qualified_symbol': 'OwnerController', 'role': 'PRIMARY'}]
    expected = [{'source_path': 'OwnerController.java',
                 'containing_type': 'OwnerController',
                 'qualified_symbol': 'OwnerController.processFindForm',
                 'role': 'PRIMARY'}]
    result = compare_components(proposed, expected)
    assert result == {
        'path': {'matched': 1, 'expected': 1, 'proposed': 1},
        'type': {'matched': 1, 'expected': 1, 'proposed': 1},
        'symbol_name': {'matched': 0, 'expected': 1, 'proposed': 1},
        'exact_component': {'matched': 0, 'expected': 1, 'proposed': 1},
        'expected_realization_chain_coverage': 0.0,
        'extra_proposed_components': [{
            'source_path': 'OwnerController.java',
            'containing_type': 'OwnerController',
            'qualified_symbol': 'OwnerController',
        }],
        'missing_expected_components': [{
            'source_path': 'OwnerController.java',
            'containing_type': 'OwnerController',
            'qualified_symbol': 'OwnerController.processFindForm',
        }],
        'supporting_expected_citations': {
            'symbol_name': {'count': 0, 'symbols': []},
            'exact_component': {'count': 0, 'components': []},
        },
    }

def test_comparison_does_not_promote_supporting_evidence_to_exact_component():
    expected_method = {'source_path': 'OwnerController.java',
                       'containing_type': 'OwnerController',
                       'qualified_symbol': 'OwnerController.processFindForm'}
    result = compare_components([], [expected_method], supporting=[expected_method])
    assert result['exact_component']['matched'] == 0
    assert result['supporting_expected_citations']['symbol_name']['count'] == 1
    assert result['supporting_expected_citations']['exact_component']['count'] == 1
```

Tests also prove that symbol-name overlap across different paths/types is diagnostic only; realization-chain coverage and missing/extra use exact component identity. Proposed rows with explicit `SUPPORTING` and supporting rows with explicit `PRIMARY` fail closed. Expected roles may be present but never grant proposal credit. Drive-relative paths such as `C:src/a.py`, hostile dictionary subclasses, and iterables exceeding 10,000 components fail closed. Finite generators and one-shot iterables within the bound are accepted and consumed once.

- [x] **Step 2: Verify RED**

Run: `python3 -m pytest -q tests/test_pkb001_component_compare.py`

Expected: import failure because the comparison utility does not exist.

- [x] **Step 3: Implement bounded deterministic set-based comparison**

```python
def compare_components(proposed, expected, supporting=()):
    proposed_rows = snapshot_and_validate(proposed, channel='proposed', limit=10_000)
    expected_rows = snapshot_and_validate(expected, channel='expected', limit=10_000)
    supporting_rows = snapshot_and_validate(supporting, channel='supporting', limit=10_000)
    proposed_components = composite_identities(proposed_rows)
    expected_components = composite_identities(expected_rows)
    exact_components = proposed_components & expected_components
    return {
        'path': metric(paths(proposed_rows), paths(expected_rows)),
        'type': metric(types(proposed_rows), types(expected_rows)),
        'symbol_name': metric(symbols(proposed_rows), symbols(expected_rows)),
        'exact_component': metric(proposed_components, expected_components),
        'expected_realization_chain_coverage': ratio(
            len(exact_components), len(expected_components)),
        'extra_proposed_components': sorted_components(
            proposed_components - expected_components),
        'missing_expected_components': sorted_components(
            expected_components - proposed_components),
        'supporting_expected_citations': supporting_diagnostics(
            supporting_rows, expected_rows),
    }
```

The comparison identity is the canonical tuple `(source_path, containing_type, qualified_symbol)`. `symbol_name` is intentionally only a bare-name diagnostic. Supporting diagnostics expose both bare-name and exact-component citations independently and never alter `exact_component` or realization-chain coverage. `snapshot_and_validate` consumes at most 10,001 entries so unbounded iterables cannot cause unbounded memory growth.

- [x] **Step 4: Verify GREEN without touching the completed Task 7 report**

Run: `python3 -m pytest -q tests/test_pkb001_component_compare.py tests/test_pkb001_task7_evaluation.py`

Expected: all tests pass and `validation/pkb001/task7-evaluation/evaluation-report.json` remains byte-identical.

- [x] **Step 5: Commit**

```bash
git add tooling/validation/pkb001_component_compare.py tests/test_pkb001_component_compare.py
git commit -m "feat(pkb001): add hierarchical component comparison"
```

### Task 5: Next-run schema and readiness gate

Task 5 MUST enforce the component contract and input/authority boundaries structurally through schema, readiness, and execution-isolation validation, not only with prose sentinel tests.

**Files:**

- Create: `validation/pkb001/schemas/realization-proposal-v0.2.schema.json`
- Create: `tooling/validation/pkb001_next_run_gate.py`
- Create: `tests/test_pkb001_next_run_gate.py` (schema and executable-gate coverage)
- Modify: `IMPLEMENTATION-PLAN.md` only for completion tracking

- [x] **Step 1: Write failing schema and executable-gate tests**

```python
from copy import deepcopy

import pytest

from tooling.validation.pkb001_next_run_gate import validate_next_run


def test_v02_readiness_is_ready(valid_request, root):
    report = validate_next_run(root, valid_request)
    skill = next(item for item in valid_request['generation_inputs']
                 if item['kind'] == 'PKS1_SKILL')
    assert report == {
        'status': 'READY', 'reasons': [], 'mappings': [],
        'run_id': valid_request['proposal']['run_id'],
        'skill_path': 'skills/pkb001/pk-s1-product-realization-v0.2/SKILL.md',
        'skill_sha256': skill['sha256'],
    }


@pytest.mark.parametrize(('mutation', 'reason'), [
    ('v1 selection', 'SKILL_VERSION_NOT_SELECTED'),
    ('skill digest mismatch', 'SKILL_DIGEST_MISMATCH'),
    ('forbidden input', 'FORBIDDEN_GENERATION_INPUT'),
    ('revision mismatch', 'COMPONENT_REVISION_MISMATCH'),
    ('duplicate run_id', 'RUN_ID_ALREADY_EXISTS'),
    ('malformed schema', 'SCHEMA_INVALID'),
    ('empty inputs', 'REQUIRED_INPUT_SET_INVALID'),
    ('missing required kind', 'REQUIRED_INPUT_SET_INVALID'),
    ('duplicate required kind', 'REQUIRED_INPUT_SET_INVALID'),
    ('unfrozen semantics', 'PRODUCT_SEMANTICS_NOT_FROZEN'),
    ('wrong semantics owner', 'PRODUCT_SEMANTICS_OWNER_INVALID'),
    ('unbound Graphify evidence', 'GRAPHIFY_BINDING_INVALID'),
    ('missing query bounds', 'GRAPHIFY_QUERY_BOUNDS_MISSING'),
    ('applicable revision mismatch', 'REVISION_BINDING_MISMATCH'),
    ('requested revision mismatch', 'REVISION_BINDING_MISMATCH'),
    ('indexed revision mismatch', 'REVISION_BINDING_MISMATCH'),
    ('frozen graph digest mismatch', 'FROZEN_GRAPH_DIGEST_MISMATCH'),
    ('binding graph digest mismatch', 'GRAPH_BINDING_DIGEST_MISMATCH'),
    ('missing graph_sha256', 'SCHEMA_INVALID'),
    ('missing evidence_refs', 'SCHEMA_INVALID'),
    ('missing confidence', 'SCHEMA_INVALID'),
    ('missing limitations', 'SCHEMA_INVALID'),
])
def test_mutations_are_blocked_without_mappings(valid_request, root, mutation, reason):
    request = mutate(deepcopy(valid_request), mutation)
    report = validate_next_run(root, request)
    assert report['status'] == 'BLOCKED'
    assert reason in report['reasons']
    assert report['mappings'] == []
```

The positive fixture supplies exactly one input of each required kind: `PRODUCT_SEMANTICS`, `GRAPHIFY_BINDING_EVIDENCE`, `FROZEN_GRAPH`, and `PKS1_SKILL`. It selects exactly `skills/pkb001/pk-s1-product-realization-v0.2/SKILL.md`, records its actual SHA-256, uses a new nonblank `run_id`, and contains a complete conforming v0.2 output whose component revisions equal its top-level revision. Mutation helpers make literal fixture changes, not mocks.

- [x] **Step 2: Verify RED**

Run: `python3 -m pytest -q tests/test_pkb001_next_run_gate.py`

Expected: import failure because the executable next-run gate does not exist.

- [x] **Step 3: Add the provider-neutral JSON Schema and executable gate**

Use this provider-neutral schema; it intentionally contains no Petclinic identifiers or evaluator paths:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "realization-proposal-v0.2.schema.json",
  "type": "object",
  "additionalProperties": false,
  "required": ["schema_version", "run_id", "authority", "source_revision", "graph_sha256", "capability_results"],
  "properties": {
    "schema_version": {"const": "pkb001.realization-proposal.v0.2"},
    "run_id": {"type": "string", "minLength": 1},
    "authority": {"const": "PROPOSAL_ONLY"},
    "source_revision": {"type": "string", "pattern": "^[0-9a-f]{40}$"},
    "graph_sha256": {"type": "string", "pattern": "^[0-9a-f]{64}$"},
    "capability_results": {
      "type": "array",
      "minItems": 1,
      "items": {"$ref": "#/$defs/result"}
    }
  },
  "$defs": {
    "evidence_ref": {
      "type": "object",
      "additionalProperties": false,
      "required": ["provider_node_id", "source_path", "source_location"],
      "properties": {
        "provider_node_id": {"type": "string", "minLength": 1},
        "source_path": {"type": "string", "minLength": 1, "pattern": "^(?!/)(?!.*(?:^|/)\\.\\.(?:/|$)).+$"},
        "source_location": {"type": "string", "minLength": 1}
      }
    },
    "component": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "role", "granularity", "source_revision", "source_path",
        "qualified_symbol", "provider_node_id", "selection_reason"
      ],
      "properties": {
        "role": {"enum": ["PRIMARY", "SUPPORTING"]},
        "granularity": {
          "enum": ["REPOSITORY", "FILE", "TYPE", "METHOD", "TEMPLATE", "CONFIGURATION"]
        },
        "source_revision": {"type": "string", "pattern": "^[0-9a-f]{40}$"},
        "source_path": {
          "type": "string",
          "minLength": 1,
          "pattern": "^(?!/)(?!.*(?:^|/)\\.\\.(?:/|$)).+$"
        },
        "qualified_symbol": {"type": "string"},
        "provider_node_id": {"type": "string", "minLength": 1},
        "selection_reason": {"type": "string", "minLength": 1}
      }
    },
    "result": {
      "type": "object",
      "additionalProperties": false,
      "required": ["capability_id", "outcome", "components", "evidence_refs", "confidence", "limitations"],
      "properties": {
        "capability_id": {"type": "string", "minLength": 1},
        "outcome": {"enum": ["MAPPING_PROPOSAL", "UNRESOLVED"]},
        "components": {"type": "array", "items": {"$ref": "#/$defs/component"}},
        "evidence_refs": {
          "type": "array",
          "minItems": 1,
          "items": {"$ref": "#/$defs/evidence_ref"}
        },
        "confidence": {"type": "number", "minimum": 0, "maximum": 1},
        "limitations": {
          "type": "array",
          "minItems": 1,
          "items": {"type": "string", "minLength": 1}
        }
      },
      "allOf": [
        {
          "if": {"properties": {"outcome": {"const": "MAPPING_PROPOSAL"}}},
          "then": {
            "properties": {
              "components": {
                "minItems": 1,
                "contains": {
                  "type": "object",
                  "properties": {"role": {"const": "PRIMARY"}},
                  "required": ["role"]
                },
                "minContains": 1
              }
            }
          },
          "else": {"properties": {"components": {"maxItems": 0}}}
        }
      ]
    }
  }
}
```

JSON Schema alone cannot enforce revision equality, filesystem digests, input isolation, or committed run-ID uniqueness. Task 5 therefore requires schema validation plus cross-field and runtime checks through this public API:

```python
validate_next_run(root, request) -> report dict
```

The request supplies `generation_inputs` and `proposal`. The proposal contains `schema_version`, a new nonblank `run_id`, immutable `authority=PROPOSAL_ONLY`, top-level `source_revision`, `graph_sha256`, and `capability_results`. Each result contains `evidence_refs`, numeric `confidence` from 0 through 1, and nonempty `limitations`; the top-level binding is authoritative, so no redundant per-result source revision is added.

The executable gate uses these constants and cross-field checks:

```python
from collections import Counter

SKILL_PATH = 'skills/pkb001/pk-s1-product-realization-v0.2/SKILL.md'
REQUIRED_INPUT_KINDS = (
    'PRODUCT_SEMANTICS', 'GRAPHIFY_BINDING_EVIDENCE',
    'FROZEN_GRAPH', 'PKS1_SKILL',
)
ALLOWED_INPUT_KINDS = frozenset(REQUIRED_INPUT_KINDS)
FORBIDDEN_PATH_PARTS = frozenset({
    'evaluator', 'task6', 'task7', 'human-review', 'gold', 'judgments',
    'post-generation', 'comparison', 'evaluation',
})

def validate_next_run(root, request):
    reasons = set()
    inputs = request.get('generation_inputs', [])
    counts = Counter(item.get('kind') for item in inputs if isinstance(item, dict))
    if set(counts) != ALLOWED_INPUT_KINDS or any(
            counts[kind] != 1 for kind in REQUIRED_INPUT_KINDS):
        reasons.add('REQUIRED_INPUT_SET_INVALID')
    if any(not isinstance(item, dict) or
           item.get('kind') not in ALLOWED_INPUT_KINDS for item in inputs):
        reasons.add('GENERATION_INPUT_NOT_ALLOWLISTED')
    if any(not isinstance(item, dict) or forbidden_path(item.get('path', ''))
           for item in inputs):
        reasons.add('FORBIDDEN_GENERATION_INPUT')
    if any(not isinstance(item, dict) or not digest_matches(root, item)
           for item in inputs):
        reasons.add('INPUT_DIGEST_MISMATCH')
    by_kind = {item['kind']: item for item in inputs
               if isinstance(item, dict) and counts[item.get('kind')] == 1}
    skill = by_kind.get('PKS1_SKILL', {})
    if skill.get('path') != SKILL_PATH:
        reasons.add('SKILL_VERSION_NOT_SELECTED')
    elif not digest_matches(root, skill):
        reasons.add('SKILL_DIGEST_MISMATCH')
    semantics = load_json_input(root, by_kind.get('PRODUCT_SEMANTICS'), reasons)
    binding = load_json_input(root, by_kind.get('GRAPHIFY_BINDING_EVIDENCE'), reasons)
    graph = by_kind.get('FROZEN_GRAPH', {})
    if semantics.get('status') != 'FROZEN':
        reasons.add('PRODUCT_SEMANTICS_NOT_FROZEN')
    if semantics.get('owner') != 'PRODUCT_TEAM':
        reasons.add('PRODUCT_SEMANTICS_OWNER_INVALID')
    if binding.get('result') != 'EXACTLY_BOUND':
        reasons.add('GRAPHIFY_BINDING_INVALID')
    if not binding.get('query_bounds'):
        reasons.add('GRAPHIFY_QUERY_BOUNDS_MISSING')
    proposal = request.get('proposal', {})
    reasons.update(schema_errors(proposal))
    revision = proposal.get('source_revision')
    if any(value != revision for value in (
            semantics.get('applicable_source_commit_sha'),
            binding.get('requested_revision'), binding.get('indexed_revision'))):
        reasons.add('REVISION_BINDING_MISMATCH')
    if any(component.get('source_revision') != proposal.get('source_revision')
           for result in proposal.get('capability_results', [])
           for component in result.get('components', [])):
        reasons.add('COMPONENT_REVISION_MISMATCH')
    frozen_graph_sha256 = verified_sha256(root, graph, reasons)
    if graph.get('sha256') != frozen_graph_sha256:
        reasons.add('FROZEN_GRAPH_DIGEST_MISMATCH')
    if binding.get('graph_sha256') != frozen_graph_sha256:
        reasons.add('GRAPH_BINDING_DIGEST_MISMATCH')
    if proposal.get('graph_sha256') != frozen_graph_sha256:
        reasons.add('GRAPH_BINDING_DIGEST_MISMATCH')
    run_id = proposal.get('run_id')
    if not isinstance(run_id, str) or not run_id.strip():
        reasons.add('RUN_ID_INVALID')
    elif run_id in committed_pkb001_run_ids(root):
        reasons.add('RUN_ID_ALREADY_EXISTS')
    status = 'BLOCKED' if reasons else 'READY'
    return {
        'status': status, 'reasons': sorted(reasons), 'mappings': [],
        'run_id': run_id, 'skill_path': skill.get('path'),
        'skill_sha256': skill.get('sha256'),
    }
```

`schema_errors` validates with the checked-in Draft 2020-12 schema and returns `SCHEMA_INVALID` for any violation. Each component `source_revision` equals the proposal top-level `source_revision`; this is enforced after schema validation. `committed_pkb001_run_ids` reads JSON returned by `git ls-files validation/pkb001` and collects every nonblank `run_id` in committed PKB-001 artifacts and manifests. A requested ID must not collide with any existing immutable `run_id`, and the gate never overwrites a run or artifact.

The explicit generation-input allowlist requires exactly one input of each required kind and rejects empty inputs, a missing required kind, a duplicate required kind, and any unknown kind. All four inputs require repository-relative paths and verified SHA-256 values. `PRODUCT_SEMANTICS` must have `status=FROZEN` and `owner=PRODUCT_TEAM`. `GRAPHIFY_BINDING_EVIDENCE` must have `result=EXACTLY_BOUND` with nonempty query bounds. The requested revision, indexed revision, applicable revision, and proposal `source_revision` must all be identical, as must every component revision. The binding `graph_sha256` equals the verified frozen-graph SHA-256 and the proposal `graph_sha256`; `PKS1_SKILL` selects exact v0.2 and records and verifies its SHA-256.

The allowlist also rejects `evaluator/`, task6, task7, human-review, gold, judgments, and post-generation comparison/evaluation inputs. Any missing, duplicate, unbound, mismatched, or forbidden input produces `BLOCKED` with no mappings.

The gate emits a deterministic `READY` or `BLOCKED` report with sorted reasons, defaults fail closed and does not execute generation. Missing keys, missing files, path-normalization failures, malformed JSON, schema errors, and digest/read failures are converted to stable `BLOCKED` reason codes rather than escaping as exceptions. The CLI only validates and writes a new report:

```bash
python3 tooling/validation/pkb001_next_run_gate.py --root . --request next-run-request.json --report next-run-readiness.json
```

The CLI exits `0` for `READY` and `1` for `BLOCKED`, serializes sorted-key JSON with a trailing newline, refuses to overwrite an existing report, never invokes PK-S1, and never emits proposal mappings.

After a `READY` report, any downstream writer MUST atomically create a non-existing output path and run ID in one exclusive operation and abort on collision. This closes the gate-to-write TOCTOU window; the gate itself writes no generation output.

- [x] **Step 4: Verify schema and complete repository regression**

Run:

```bash
python3 -m pytest -q
MAVEN_OPTS='-Xmx2g' ./mvnw test -q
python3 validation/pkb001/task7-evaluation/public_validate.py .
git diff --check
```

Expected: positive v0.2 readiness passes. Mutations for v1 selection, skill digest mismatch, forbidden input, revision mismatch, duplicate run_id, and malformed schema each assert `BLOCKED` with no mappings. All Python and Java tests pass, Task 7 remains 9/9, the decision remains `REVISE`, and current immutable Petclinic artifacts have no diff.

- [x] **Step 5: Mark implementation readiness without selecting the holdout**

Update this plan with completed commit IDs and record these remaining authorized decisions:

1. Historical v0.2 prerequisite: Product Team completes the current 15-item review (superseded for the next run by the selected individual-review plan).
2. User approves a second exact-revision holdout repository.
3. Numeric thresholds are frozen before generating either next-run proposal.

Implementation record (2026-09-05): executable schema/readiness gate committed as
`7eb1c88`, with checked-in-schema validation hardened in `1c5b879`. Focused gate tests passed 32/32, the complete Python suite passed
206/206, Task 7 public validation remained 9/9, and the isolated Maven suite
passed with `MAVEN_OPTS='-Xmx2g'`. No holdout was selected and no proposal was
generated. The three authorized decisions above remain open.

Review hardening record (2026-09-05): `934946b` adds fail-closed hostile-shape
handling, compound forbidden-path classification, Java identity parity checks,
HEAD-blob run-ID collision checks, single-read digest/JSON verification,
`Draft202012Validator` schema checking, and dir-fd/no-follow exclusive CLI
report creation. Focused gate tests passed 66/66, the complete Python suite
passed 240/240, Task 7 remained 9/9, and isolated Maven tests passed with
`MAVEN_OPTS='-Xmx2g'`. The repository has no Python dependency manifest;
`jsonschema` is present in the Graphify runtime (4.26.0) and verification
interpreter (4.25.1). A runtime without `jsonschema` blocks with
`SCHEMA_DEFINITION_INVALID` and never falls back to a partial validator.

Final parity record (2026-09-05): `73bd933` rejects whitespace-only component
paths, provider node IDs, selection reasons, and evidence identities while
matching the Java symbol rule for `TYPE`, `METHOD`, `TEMPLATE`, and
`CONFIGURATION`. Java permits any `qualifiedSymbol` value for `REPOSITORY` and
`FILE`; the v0.2 JSON contract accepts any string there, including blank or
nonblank. Unlike the Java nullable record field, the active PK-S1 v0.2 envelope
still requires the JSON field and its schema type remains string. Focused gate
tests passed 75/75, the full Python suite passed 249/249, Task 7 remained 9/9,
and isolated Maven tests passed with `MAVEN_OPTS='-Xmx2g'`.

Completion ledger:

- Task 1: `d483c39d`, `b634d0fb`
- Task 2: `40adc0c`, `383cac7`
- Task 3: `1b4cb7b`, `9ce4a58`, `67be4a7`, `300ce7c`
- Task 4: `37d4aa6`, `d402e916`, `7fb1e796`, `27d3b45`, `e89f77c`
- Task 5: `7eb1c88`, `1c5b879`, `934946b`, `73bd933`, `54042d3`, `5f3c86b`

Public-boundary record (2026-09-05): `54042d3` snapshots exact built-in JSON
containers before any request access and rejects hostile subclasses, non-finite
numbers, excessive nesting, and oversized input with deterministic `BLOCKED`
and no mappings. It also aligns whitespace handling for `capability_id` and
every limitation with the Java proposal contract. This is readiness validation
only; no next-run generation was executed. Commit `5f3c86b` also rejects a
whitespace-only evidence `source_location`. Final focused gate tests passed
82/82, the whole Python suite passed 259/259, Task 7 remained 9/9, and the
isolated Maven suite passed with `MAVEN_OPTS='-Xmx2g'`.

- [x] **Step 6: Commit**

```bash
git add validation/pkb001/schemas/realization-proposal-v0.2.schema.json tooling/validation/pkb001_next_run_gate.py tests/test_pkb001_next_run_gate.py IMPLEMENTATION-PLAN.md
git commit -m "feat(pkb001): gate typed realization proposal runs"
```

## Next experiment improvement phase

This phase is specified but not authorized for execution by the completion of
Tasks 1–5. It applies only to a new Product Semantics revision, new skill
version, and new immutable run. The completed Petclinic artifacts, PK-S1 v0.2,
and bounded `REVISE` decision remain unchanged.

### Task 6: Human-reviewed behavior-scenario contract

- [x] Define a provider-neutral scenario schema with stable scenario and
  Capability identifiers, Given/When/Then behavior, scope, boundaries, status,
  Human Reviewer ownership, approval provenance, and immutable revision binding.
- [x] Reject implementation identifiers, Graphify nodes, evaluator mappings,
  and technical selection instructions from Product Semantics scenarios.
- [x] Keep reverse-generated `HYP-SCENARIO-*` proposals isolated as
  `PROPOSAL_ONLY / UNREVIEWED`.
- [ ] Add validation proving only frozen Human Reviewer scenarios can enter
  Forward generation (Java lifecycle guard is verified; full next-run input
  migration remains BL-007).

### Task 7: Generated proposals and individual review

- [x] Generate Capability and scenario proposals from Graphify and delivery history.
- [x] Produce one review surface with behavior, separate evidence, rationale,
  confidence, limitations and ACCEPT / EDIT / REJECT decision fields.
- [ ] Record user decisions against exact proposal versions; EDIT requires
  explicit acceptance of the replacement; REJECT never enters accepted inputs.
- [ ] Freeze only the accepted set; preserve the original proposal artifacts.
- [x] Record reviewer exposure and reconstruction-consistency limitations.

### Task 8: Scenario-grounded proposal contract

- [ ] Create a new versioned PK-S1 contract; do not modify the completed v0.2
  skill or reuse its run identifier.
- [ ] Add proposal-local component references and scenario traces containing
  variable-length behavioral chain steps.
- [ ] Keep `outcome: MAPPING_PROPOSAL | UNRESOLVED` separate from
  `evidence_status: COMPLETE | PARTIAL | INSUFFICIENT`.
- [ ] Require explicit evidence gaps and justified `NOT_APPLICABLE` steps.
- [ ] Apply behavioral PRIMARY/SUPPORTING rules without a universal
  method-first hierarchy; require an explicit reason when direct method
  evidence is replaced by a containing class.

### Task 9: Evidence and Reverse quality controls

- [ ] Verify the actual installed Graphify runtime for template, view,
  navigation, form-binding, and relationship support before adding any provider
  operation.
- [ ] If required evidence is unsupported, add a separately identified
  capability behind `CodeIntelligenceProvider` or report an evidence gap; never
  weaken Product Semantics automatically.
- [ ] Add proposal-only duplicate, composite, rename, merge/split, claim-to-
  evidence, confidence, and limitation checks for Reverse output.
- [ ] Prevent these checks from publishing or mutating Product Semantics.

### Task 10: Provider-neutral metrics and preregistration

- [ ] Add scenario evidence and complete-chain coverage plus provider-neutral
  exact-component precision, recall, F1, missing/extra counts, unresolved rate,
  and UI/template evidence-gap rate.
- [ ] Report provider-native Graphify node-ID match, path, type, bare-symbol,
  and supporting-citation measures only as separately named diagnostics.
- [ ] Extend evaluator truth with normalized component identity
  `(source_revision, source_path, granularity, qualified_symbol)`.
- [ ] Derive acceptance thresholds from declared error costs and independent
  calibration evidence. Until Human Reviewer approval, every numeric proposal is
  `PROPOSED_NOT_FROZEN`; do not reuse observed Petclinic values as a gate.

### Task 11: User-approved sealed holdout and execution

- [ ] Have an independent role propose a repository and exact revision, obtain
  explicit user approval, then seal the holdout before rule completion.
- [ ] Freeze reviewed experiment semantics, scenarios, metrics, thresholds, skill, schema,
  comparator, provider version, and Graphify query-bound digests before blind
  generation.
- [ ] Run Petclinic regression without overwriting existing artifacts.
- [ ] If any frozen input changes after regression, create a new protocol
  revision, re-freeze all digests, and restart regression while the holdout
  remains sealed.
- [ ] Execute the blind holdout once, conduct individual result review, and issue a bounded
  `GO / REVISE / STOP` decision without automatic semantic publication.

## Explicitly deferred

- Do not change or reinstall the Graphify runtime in this plan.
- Do not add template extraction until live Graphify capability is verified and separately approved.
- Do not select the holdout repository without user approval.
- Do not regenerate the completed Petclinic run under its existing run ID.
- Do not define acceptance thresholds from the observed Petclinic metrics.
- Do not publish Product Semantics from evaluator or agent output.
