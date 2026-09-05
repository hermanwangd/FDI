# PKB-001 Implementation Plan

This file contains only the current selection state, verified delivery ledger,
and executable continuation constraints. `FRAMEWORK-SPEC.md` defines what;
`BACKLOG.md` records maturity; this plan defines how selected work is delivered.

## Current selection

**Current selection:** BL-026 third migration slice, selected by the Human
Reviewer on HERM-268 (2026-09-06, options 1/2/3 as sequential slices 3–5) and
dispatched on HERM-269. Replace the repository-owned blind-review Python
consumer with a Java API and packaged CLI, cut over its active callers, then
remove that Python consumer; constructed under "Selected work: BL-026 Java
blind-review migration" below. The first and second BL-026 migration slices are
complete and retained below as their construction and verification records. The
Human Reviewer must select each further bounded Python consumer before this
plan is revised again or another implementation is dispatched. Existing Python
plan sections below are completed or transitional delivery records and do not
authorize new Python framework behavior.

## Completed BL-026 slices

### Java scenario-forward gate migration

- Replaced the bounded scenario-forward Python consumer with Java 17 / Spring Boot.
- Preserved fail-closed validation and public behavior.
- Removed only the replaced direct Python consumer after parity and regression passed.

### Java component-comparator migration

- Base: `c123f24141972b53de66997f7782fcce1fd8cb05`.
- Candidate: `248066754da2210b81504138d974c69711524dd8`.
- Commits: `8545d05`, `6af2b23`, `d81deaa`, `2480667`.
- Replaced `pkb001_component_compare.py` with the Java comparator and retained the
  established CLI/output contract.
- Independent exact-candidate adjudication: PASS; 205 Java tests, 273 Python
  passed with 3 skipped, 40/40 parity fixtures, and public validation 9/9.

## Selected work: BL-026 Java blind-review migration

**Goal:** Replace `tooling/validation/pkb001_blind_review.py` with a
Java 17 API and packaged CLI that preserves its complete observable behavior —
deterministic label/order blinding of PK-S1/PK-S2 review material — then remove
the replaced Python module and its direct Python tests.

**Spec binding:** `FRAMEWORK-SPEC.md` at
`891e497968000c32984f26437eab811c063ec4cf`; requirement `PKB-JAVA-001`;
behavior context: Task 6 deterministic label/order blinding
(`ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT`).

**Selected backlog:** `PKB-BL-026`, third bounded consumer only. Completing
this plan advances but does not close BL-026; remaining Python consumers stay
in the same backlog item for later implementation-plan revisions.

**Architecture:** Java owns sealed-input verification (execution kind, run
status, proposal-only authority attestations, artifact/manifest/witness digest
binding, recorded input digests, forward item counts, reverse hypothesis
counts, shared source/graph binding), deterministic blind ordering
(SHA-256 of a fixed order salt plus the source identifier), the public packet,
the sealed key, the non-recursive manifest, reviewer instructions, judgment
workspace templates, completed-judgment overwrite protection, and the exact
`sorted/indent-2/non-ASCII` JSON rendering and SHA-256 digest semantics of the
Python consumer. The legacy `build_blind_packet` seam is ported unchanged.
Inputs are the sealed read-only Petclinic artifacts; historical packet bytes
under `validation/pkb001/task6-blind-review/` are never regenerated. Rendering
is verified byte-for-byte against the sealed historical outputs.

**Tech stack:** Java 17, Spring Boot 3.4.1, Jackson, JUnit 5 and Maven.
Python/pytest is used only to characterize the old consumer before cutover.

### Task 1: Characterize observable behavior

**Files:**

- Read: `tooling/validation/pkb001_blind_review.py`
- Read: `tests/test_pkb001_blind_review.py`
- Read: `tests/test_pkb001_task6_blind_packet.py`
- Read: `validation/pkb001/java-migration/python-framework-inventory.json`

- [ ] Run `python3 -m pytest -q tests/test_pkb001_blind_review.py tests/test_pkb001_task6_blind_packet.py` before
  replacement. Record the passing count: **14 collected characterization cases**
  (3 legacy `build_blind_packet` cases + 11 Task-6 packet cases); all pass.
  Never weaken a rejected case for Java parity.
- [ ] Confirm the inventory records the consumer as `TRANSITIONAL` with exactly
  the two active callers `tests/test_pkb001_blind_review.py` and
  `tests/test_pkb001_task6_blind_packet.py`; no skill or tool invokes it.
- [ ] Record the observable contract to port:
  - JSON rendering: `json.dumps(indent=2, ensure_ascii=False, sort_keys=True) + '\n'`;
    SHA-256 hex digests over file bytes and over rendered JSON bytes.
  - Blind ordering: ascending SHA-256 hex of `pkb001-task6-blind-order-v1\0` +
    `source_identifier` (Task-6 packet) and of `run_id + '\0' + proposal_id`
    (legacy seam).
  - Packet/key/manifest/workspace shapes and constants: packet
    `pkb001.task6.blind-review-packet.v1` / `pkb001-task6-blind-comparison-v1`
    with 15 `BR-###` items (9 forward mapping proposals, 1 forward unresolved,
    5 reverse hypotheses), sealed key
    `pkb001.task6.sealed-blind-key.v1` / `SEALED_KEY_CUSTODIAN_ONLY`,
    non-recursive manifest `pkb001.task6.blind-review-manifest.v1` with fixed
    input-digest list, blinding, isolation, and decision-boundary blocks;
    `reviewer_instructions()` markdown; `reviewer_template(packet_digest,
    workspace_id)` with `NON_HUMAN` / `EVALUATOR_ONLY` reviewer context,
    isolation flags, empty `judgments`, and a `BR-###` entry template.
  - Verification vocabulary (`BindingError` messages): `forward input is
    missing: <path>`, `forward input digest mismatch: <path>`, `forward
    execution kind is not a skill execution`, `forward run is not completed`,
    `forward run is not proposal-only`, `forward manifest is not
    proposal-only`, `forward artifact digest is not bound`, `forward witness
    artifact digest is not bound`, `forward artifact did not attest non-access`,
    `forward manifest did not attest non-access`, `forward witness did not
    attest non-access`, `forward witness authority differs`, `forward witness
    assurance limit is missing`, `forward <field> differs from manifest`,
    `forward item count is not 10`, `forward mapping count is not 9`, `forward
    unresolved count is not 1`, `forward item authority is not proposal-only`,
    the mirrored `reverse ...` and `reverse input ...` messages, `runs use
    different source commits`, `runs use different graph digests`, `cannot
    safely inspect existing judgment workspace <path>: <error>`, and `refusing
    to overwrite completed judgments; initialize a new version with an explicit
    --output-dir`.
  - CLI contract: `pkb001_blind_review.py --root <dir> --output-dir <dir>`
    (argparse defaults: current directory, `validation/pkb001/task6-blind-review`);
    success prints one line of compact sorted-key JSON
    `{"packet_id": ..., "packet_sha256": ...}` and exits 0; `BindingError`
    prints the message on stderr and exits 2; argparse usage errors exit 2;
    unexpected failures propagate (traceback, exit 1). Overwrite protection
    runs before any file is written.

### Task 2: Implement the Java blind-review API

**Files:**

- Create: `src/main/java/com/featuredeliveryintelligence/fdi/validation/blindreview/BlindReview.java`
- Create: `src/main/java/com/featuredeliveryintelligence/fdi/validation/blindreview/BlindReviewBindingException.java`
- Create: `src/main/java/com/featuredeliveryintelligence/fdi/validation/blindreview/Task6Packet.java`
- Create: `src/main/java/com/featuredeliveryintelligence/fdi/validation/blindreview/LegacyBlindPacket.java`
- Create: `src/test/java/com/featuredeliveryintelligence/fdi/validation/blindreview/BlindReviewCharacterizationTests.java`

- [ ] Write failing tests porting every decision of the 14 Python
  characterization cases: the 3 legacy seam cases (arm/proposal identity
  omission, deterministic `BR-001`/`BR-002` ordering independent of input
  order, forward-mapping and reverse-hypothesis acceptance with
  `FORWARD_SKILL`/`REVERSE_SKILL` source kinds) and the 11 Task-6 cases
  (packet accounting without judgments; sealed-key packet binding and absent
  arm labels/identifiers; neutral record shapes not uniquely identifying an
  arm; deterministic label/order blinding claims and disclosed limitation;
  frozen judgment contract and `NON_HUMAN` limit; identical workspace inputs
  and isolation after review; refusal to overwrite completed judgments without
  mutation; explicit new-version initialization; manifest input digests;
  one-current-digest-set in the Task-6 report).
- [ ] Pin byte-level parity: rendering the packet, sealed key, manifest, and
  reviewer instructions built from the sealed inputs must equal the sealed
  historical bytes under `validation/pkb001/task6-blind-review/` exactly.
- [ ] Run `MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=BlindReviewCharacterizationTests test`;
  expect compilation failure because the API is absent (RED).
- [ ] Implement this public boundary. `BindingError` ports as
  `BlindReviewBindingException` (a `RuntimeContractException`); all other
  failures propagate unchanged.

```java
public final class BlindReview {
    public static final String TASK6_DIR = "validation/pkb001/task6-blind-review";
    public static Task6Packet buildTask6Packet(Path root);
    public static ObjectNode writeTask6Artifacts(Path root, Path outputDir);
    public static ObjectNode writeTask6Artifacts(Path root);
    public static LegacyBlindPacket buildBlindPacket(String runId, List<Object> outputs);
    public static String reviewerInstructions();
    public static ObjectNode reviewerTemplate(String packetDigest, String workspaceId);
    public static String sha256(byte[] bytes);
    public static byte[] jsonBytes(Object value);
}
```

- [ ] Rerun the focused tests; expect zero failures (GREEN). Commit as
  `feat(fdi): add Java blind review packet generation`.

### Task 3: Expose the Java CLI

**Files:**

- Create: `src/main/java/com/featuredeliveryintelligence/fdi/application/BlindReviewCli.java`
- Modify: `src/main/java/com/featuredeliveryintelligence/fdi/application/FdiApplication.java`
- Create: `src/test/java/com/featuredeliveryintelligence/fdi/application/BlindReviewCliTests.java`

- [ ] Write failing process-level tests for
  `blind-review-generate --root <dir> --output-dir <dir>` on a copied Task-6
  root: missing, duplicate, unknown, and `--option=value` forms; successful
  generation on a fresh output directory (exit 0, one deterministic JSON line,
  no stack trace); completed-judgment refusal (exit 2, `refusing to overwrite
  completed judgments` on stderr, no mutation, no traceback); `handles`
  dispatch isolation; and stable exit codes through `FdiApplication` without
  starting Spring.
- [ ] Implement the dispatcher and invoke it before Spring startup, mirroring
  `ScenarioForwardCli`: parse `--root` and `--output-dir` (both optional with
  the Python defaults), fail usage errors with exit 2 and no stack trace,
  catch only `BlindReviewBindingException` for the exit-2 contract, and print
  the compact sorted-key `{"packet_id", "packet_sha256"}` line on success.
- [ ] Run the focused CLI tests and `MAVEN_OPTS='-Xmx2g' ./mvnw -q package`;
  expect zero failures. Smoke-test the packaged JAR against a copied Task-6
  root. Commit as `feat(fdi): expose Java blind review generation CLI`.

### Task 4: Cut over and remove only the replaced Python consumer

**Files:**

- Modify: `validation/pkb001/java-migration/python-framework-inventory.json`
- Modify: `tests/test_pkb001_python_framework_inventory.py`
- Delete: `tooling/validation/pkb001_blind_review.py`
- Delete: `tests/test_pkb001_blind_review.py`
- Delete: `tests/test_pkb001_task6_blind_packet.py`
- Modify: `BACKLOG.md`, `STATUS.json`, `IMPLEMENTATION-PLAN.md`

- [ ] Verify `tests/test_pkb001_blind_review.py` and
  `tests/test_pkb001_task6_blind_packet.py` are the only active callers; search
  may retain only explicitly historical plan text before deletion.
- [ ] Mark the inventory entry `MIGRATED_TO_JAVA`, record the Java API, CLI,
  and verification evidence, extend the inventory test's migrated-list
  assertion, and add the blind-review cutover assertion. Delete only the
  replaced module and its two direct tests. Do not delete other Python
  consumers or modify external Graphify.
- [ ] Run the complete verification set:

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw test -q
MAVEN_OPTS='-Xmx2g' ./mvnw -q package
python3 -m pytest -q
python3 validation/pkb001/task7-evaluation/public_validate.py .
git diff --check
```

Expected: the Java suite grows beyond the 205-test second-slice baseline and
never shrinks; remaining transitional Python regression passes at 267 tests
with 3 skips (273 minus the 14 removed characterization cases plus the new
blind-review cutover inventory test); public validation stays 9/9; no active
caller imports the deleted module; sealed artifacts under
`validation/pkb001/` are byte-identical.
- [ ] Update BL-026 progress and `STATUS.json` with test counts and commit
  IDs. Keep BL-026 active and `PKB-JAVA-001` below M3 because other Python
  consumers remain. Commit the cutover as
  `refactor(fdi): cut blind review generation over to Java` and the control
  reconciliation as `docs(fdi): record blind review Java cutover`.

### Plan acceptance boundary

- Java preserves every one of the 14 characterization decisions, the blind
  ordering and digest semantics, byte-identical sorted JSON/Markdown rendering,
  the fail-closed sealed-input verification and overwrite protection, and the
  CLI exit-code/stdout contract.
- The replaced Python module and its two direct tests are gone; no other
  Python consumer is touched; external Graphify remains unchanged.
- Historical evidence artifacts under `validation/pkb001/` remain byte-identical.
- BL-026 remains open for later consumers; no child Backlog Items are created.

---

These are completion records, not authority to select the next consumer.

## Verified foundation ledger

| Backlog | Delivered behavior | Verification boundary |
|---|---|---|
| `PKB-BL-018` | Durable `StructuralComponentIdentity` | Java identity and immutability tests. |
| `PKB-BL-019` | Immutable `RealizationProposal` contract | Authority, role, revision, and outcome tests. |
| `PKB-BL-020` | PK-S1 proposal-only and gold isolation | Forbidden-input and no-publication tests. |
| `PKB-BL-021` | Hierarchical comparison | Deterministic metric and regression tests. |
| `PKB-BL-022` | Next-run readiness gate | Schema, identity, digest, mutation, CLI, and clean-copy tests. |
| `PKB-BL-005`, `PKB-BL-023` | Scenario proposal lifecycle and individual review surface | Validator and artifact tests. |
| `PKB-BL-024` | Active review pointers | Baseline contract tests. |

### Task 5: Next-run schema and readiness gate

This completed foundation contract is retained because later work consumes it.
The implementation lives at `tooling/validation/pkb001_next_run_gate.py`; its
tests live at `tests/test_pkb001_next_run_gate.py`. Its stable API is
`validate_next_run(root, request) -> report dict`.

The gate defaults fail closed and does not execute generation. It performs
schema validation plus cross-field and runtime checks and returns a deterministic
`READY` or `BLOCKED` report. A blocked request is `BLOCKED` with no mappings.
The output is a deterministic `READY` or `BLOCKED` report.

Required inputs use kinds `'PRODUCT_SEMANTICS'`, `'GRAPHIFY_BINDING_EVIDENCE'`,
`'FROZEN_GRAPH'`, and `'PKS1_SKILL'`, with exactly one input of each required kind.
Product Semantics requires `status=FROZEN` and `owner=PRODUCT_TEAM`. Graphify
binding evidence requires `result=EXACTLY_BOUND` with nonempty query bounds.
The gate checks requested revision, indexed revision, applicable revision, and proposal `source_revision`.
Every component `source_revision` equals the proposal
top-level `source_revision`.
Each component `source_revision` equals the proposal top-level `source_revision`.

The frozen graph records and verifies its SHA-256. The binding `graph_sha256`
equals the verified frozen-graph SHA-256. The PK-S1 request uses an explicit generation-input allowlist
and must not collide with any existing immutable
`run_id`. The downstream writer MUST atomically create a non-existing output path
and run ID.
The binding `graph_sha256` equals the verified frozen-graph SHA-256.
The downstream writer MUST atomically create a non-existing output path and run ID.

The proposal schema preserves these mandatory shapes:

```json
"required": ["schema_version", "run_id", "authority", "source_revision", "graph_sha256", "capability_results"]
```

```json
"required": ["capability_id", "outcome", "components", "evidence_refs", "confidence", "limitations"]
```

```json
"confidence": {"type": "number", "minimum": 0, "maximum": 1}
```

```json
"required": ["provider_node_id", "source_path", "source_location"]
```

Mutation coverage includes v1 selection, skill digest mismatch, forbidden input,
revision mismatch, duplicate run_id, malformed schema, empty inputs, missing
required kind, duplicate required kind, unfrozen semantics, wrong semantics owner,
unbound Graphify evidence, missing query bounds, applicable revision mismatch,
requested revision mismatch, indexed revision mismatch, frozen graph digest
mismatch, binding graph digest mismatch, missing graph_sha256, missing evidence_refs,
missing confidence, and missing limitations.

The run ID must not collide with any existing immutable `run_id`.
Required mutation labels are: empty inputs; missing required kind; duplicate required kind; unfrozen semantics; wrong semantics owner; unbound Graphify evidence; missing query bounds; applicable revision mismatch; requested revision mismatch; indexed revision mismatch; frozen graph digest mismatch; binding graph digest mismatch.

## Next experiment construction sequence

The previous Petclinic run is immutable evidence with decision `REVISE`. It used
deterministic label/order blinding, but its evidence content leaves
`ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT`. It had no preregistered numeric
acceptance gate, so its metrics are diagnostic rather than a reusable threshold.

Before another experiment:

1. Finish the 13 pending scenario decisions and 11 disagreement adjudications.
   Only completed Human Reviewer human review may establish Product meaning.
2. Create a new frozen scenario-bearing semantics revision; do not overwrite the
   Petclinic input.
3. Reconcile scenario-grounded PK-S1 with the Java-only framework target.
4. Verify actual Graphify runtime capability without assuming APIs.
5. Improve reverse proposal controls and provider-neutral evaluator identity.
6. Define separate scenario, chain, exact-component, and diagnostic metrics.
7. Preregister justified numeric thresholds and approve one exact-revision holdout.
8. Freeze all inputs in the readiness protocol, regress Petclinic, then run the
   sealed holdout once.
9. Review evidence and issue GO / REVISE / STOP; formal semantic publication is outside this prototype.

## Selection template

Before implementing any new slice, update this file and `STATUS.json` with:

- one backlog ID and one normative requirement;
- exact base commit and owned files;
- in-scope and explicitly excluded behavior;
- observable acceptance criteria and negative cases;
- focused tests, full regression commands, and independent review expectation;
- removal boundary for any replaced Python framework consumer.

An agent must stop with `CONTEXT_CONFLICT` if the five active files disagree.
No plan may silently broaden into Graphify replacement, holdout execution,
automatic Product semantics, or governance redesign.

## Default verification

Run within the 8 GB system limit:

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw clean package -q
python3 -m pytest -q
python3 validation/pkb001/task7-evaluation/public_validate.py .
python3 -m json.tool STATUS.json
git diff --check
```
