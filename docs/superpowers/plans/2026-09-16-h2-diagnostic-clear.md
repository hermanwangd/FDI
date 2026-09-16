# H2 Diagnostic Calibration Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a deterministic `calibrationReadiness` result that permits bounded engineering calibration only when all seven supplied-evidence dimensions are adequate and contain no match or near-duplicate signal.

**Architecture:** Extend the existing `H2ExposureComparator` aggregation without changing its input contract, matching algorithm, or formal-independence fields. Track adequate dimension coverage while comparisons are evaluated, then derive one of four readiness values using fixed fail-closed precedence. Preserve the work as a new reviewed evidence slice and reconcile active controls without closing H2 or SF-BL-005.

**Tech Stack:** Java 17, Spring Boot 3.4.1, Jackson, JUnit 5, Maven, pytest, Git/GitHub PR workflow.

---

## File map

- Modify `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/contamination/v1/H2ExposureComparator.java`: calculate evidence adequacy and emit readiness.
- Modify `src/test/java/com/featuredeliveryintelligence/fdi/application/H2ExposureCompareCliTests.java`: focused contract and precedence tests.
- Modify `IMPLEMENTATION-PLAN.md`, `BACKLOG.md`, and `STATUS.json`: select, track, and reconcile the bounded SF-BL-005 slice.
- Create `validation/software-factory/sf-bl005/h2-diagnostic-clear-001/`: exact candidate, test, CLI, review, and boundary evidence.
- Do not modify H0/H1 receipts or binaries, the formal holdout protocol, contamination ledger, tokenization, thresholds, input schema, resource limits, or historical H2 evidence.

### Task 1: Select the bounded execution

**Files:**
- Modify: `IMPLEMENTATION-PLAN.md`
- Modify: `BACKLOG.md`
- Modify: `STATUS.json`

- [ ] **Step 1: Record the selected lane**

Add a compact `H2 diagnostic calibration readiness` section to
`IMPLEMENTATION-PLAN.md` bound to design commit `b60717f`, `SF-BL-005`, and the
current full base commit. State the four readiness values, exact owned files,
exclusions, Java 17 verification commands, independent review requirement, and
the prohibition on formal selection, H2 completion, and parent closure.

- [ ] **Step 2: Reconcile project truth**

In `STATUS.json`, record PR65 as integrated, retain
`h2_global_exposure_completeness=INCOMPLETE_PENDING_HUMAN_DISCLOSURE_ATTESTATION`,
and add an active execution with:

```json
{
  "lane_id": "h2-diagnostic-clear",
  "execution_id": "SF-BL-005-H2-DIAGNOSTIC-CLEAR-001",
  "base_commit": "<full current origin/main commit>",
  "selected_backlog_items": ["SF-BL-005"],
  "execution_state": "DESIGN_APPROVED_IMPLEMENTATION_SELECTED",
  "integration_candidate": null
}
```

Update the SF-BL-005 row in `BACKLOG.md` with the selected bounded extension;
do not change its `IN_PROGRESS` state.

- [ ] **Step 3: Validate and commit controls**

Run:

```bash
jq empty STATUS.json
test "$(wc -c < IMPLEMENTATION-PLAN.md)" -le 10240
git diff --check
```

Expected: all commands exit 0.

Commit:

```bash
git add IMPLEMENTATION-PLAN.md BACKLOG.md STATUS.json
git commit -m "docs: select H2 diagnostic calibration readiness"
```

### Task 2: Add failing readiness contract tests

**Files:**
- Modify: `src/test/java/com/featuredeliveryintelligence/fdi/application/H2ExposureCompareCliTests.java`

- [ ] **Step 1: Add a seven-dimension fixture helper**

Add a helper that creates one comparison for every dimension. Use non-empty
different strings for identity and lineage, and at least five policy tokens per
side for the other five dimensions. Each comparison must carry the exact SHA-256
of its UTF-8 content.

- [ ] **Step 2: Add four focused tests**

Add tests with these exact expectations:

```java
assertEquals("DIAGNOSTICALLY_CLEAR", clear.path("calibrationReadiness").asText());
assertEquals("BLOCKED_MATCH", match.path("calibrationReadiness").asText());
assertEquals("REVIEW_REQUIRED", near.path("calibrationReadiness").asText());
assertEquals("INSUFFICIENT_EVIDENCE", missing.path("calibrationReadiness").asText());
```

The tests must also prove:

```java
assertEquals("NOT_PROVEN_INDEPENDENT", clear.path("eligibility").asText());
assertFalse(clear.path("selectionAuthorized").asBoolean(true));
assertEquals("SUPPLIED_EXTRACTS_ONLY", clear.path("applicability").asText());
assertEquals("H2-COMPARISON-OUTPUT-002", clear.path("schemaVersion").asText());
assertEquals(2, clear.at("/policy/version").asInt());
```

Include precedence cases where MATCH wins over near/missing and near wins over
missing. Include empty identity evidence and fewer-than-five-token text evidence
to prove they do not satisfy coverage.

- [ ] **Step 3: Run the focused tests and preserve RED**

Run:

```bash
MAVEN_OPTS='-Xmx2g' mvn -q -DargLine=-Xmx1g -DforkCount=1 \
  -Dtest=H2ExposureCompareCliTests test
```

Expected: FAIL because `calibrationReadiness` is absent and the output schema
and policy are still version 1. Save the complete command output under the new
evidence directory as `targeted-red.log` during Task 4; do not rewrite or label
a compilation mistake as behavioral RED.

- [ ] **Step 4: Commit the failing tests**

```bash
git add src/test/java/com/featuredeliveryintelligence/fdi/application/H2ExposureCompareCliTests.java
git commit -m "test: define H2 diagnostic calibration readiness"
```

### Task 3: Implement the readiness derivation

**Files:**
- Modify: `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/contamination/v1/H2ExposureComparator.java`

- [ ] **Step 1: Track comparison facts**

Add sets/flags while processing comparisons:

```java
Set<String> adequateDimensions = new HashSet<>();
boolean reviewRequired = false;
```

Identity and lineage are adequate only when both decoded strings are non-empty.
For the other dimensions, tokenize each side once and treat the comparison as
adequate only when each side has at least five tokens. Preserve the existing
shingle intersection/union and threshold behavior.

- [ ] **Step 2: Derive readiness with fixed precedence**

After all comparisons, compute:

```java
String calibrationReadiness;
if (!matches.isEmpty()) {
    calibrationReadiness = "BLOCKED_MATCH";
} else if (reviewRequired) {
    calibrationReadiness = "REVIEW_REQUIRED";
} else if (!adequateDimensions.containsAll(DIMENSIONS)) {
    calibrationReadiness = "INSUFFICIENT_EVIDENCE";
} else {
    calibrationReadiness = "DIAGNOSTICALLY_CLEAR";
}
```

Emit it at the output root, change `schemaVersion` to
`H2-COMPARISON-OUTPUT-002`, and change only the embedded policy version to `2`.
Keep `eligibility`, `selectionAuthorized`, and `applicability` unchanged.

- [ ] **Step 3: Run focused tests**

Run the Task 2 Maven command.

Expected: all `H2ExposureCompareCliTests` pass with zero failure/error.

- [ ] **Step 4: Inspect the behavioral diff and commit**

```bash
git diff --check
git diff -- src/main src/test
git add src/main/java/com/featuredeliveryintelligence/fdi/product/realization/contamination/v1/H2ExposureComparator.java
git commit -m "feat: classify H2 diagnostic calibration readiness"
```

### Task 4: Verify the packaged behavior and assemble evidence

**Files:**
- Create: `validation/software-factory/sf-bl005/h2-diagnostic-clear-001/summary.json`
- Create: `validation/software-factory/sf-bl005/h2-diagnostic-clear-001/runtime-identity.json`
- Create: `validation/software-factory/sf-bl005/h2-diagnostic-clear-001/cli-runs.json`
- Create: `validation/software-factory/sf-bl005/h2-diagnostic-clear-001/artifact-manifest.json`
- Create: bounded input/output/log files in the same directory.

- [ ] **Step 1: Run complete verification once**

```bash
MAVEN_OPTS='-Xmx2g' mvn -q clean package -DargLine=-Xmx1g -DforkCount=1
python3 -m pytest -q
```

Expected: Java and Python suites pass with zero failure/error. Record exact
counts, command arguments, candidate commit, start/end times, and SHA-256 log
digests. One heavy JVM may run at a time; aggregate memory must remain below
8 GB.

- [ ] **Step 2: Run packaged CLI fixtures**

Create synthetic fixtures for all four readiness values. Run each twice from
the packaged JAR into absent output paths. Verify byte-identical repeated output,
all seven dimension records, the expected readiness, and unchanged safety fields.

- [ ] **Step 3: Bind the runtime and evidence**

Store the exact JAR under the existing content-addressed `.fdi-artifacts/sha256`
convention as read-only. Record source commit, source-file digests, JAR digest,
policy digest, and `frozenH0H1Replacement=false`. Build a manifest containing
every evidence artifact except the manifest itself.

- [ ] **Step 4: Commit the evidence package**

```bash
git add -f validation/software-factory/sf-bl005/h2-diagnostic-clear-001
git commit -m "test: verify H2 diagnostic calibration readiness"
```

### Task 5: Independent review, reconciliation, and PR integration

**Files:**
- Modify: `IMPLEMENTATION-PLAN.md`
- Modify: `BACKLOG.md`
- Modify: `STATUS.json`
- Create: `validation/software-factory/sf-bl005/h2-diagnostic-clear-001/code-review.json`
- Create: `validation/software-factory/sf-bl005/h2-diagnostic-clear-001/evidence-review.json`

- [ ] **Step 1: Obtain independent review**

The reviewer must be distinct from implementation/integration and bind the
exact candidate. Review contract correctness, precedence, evidence adequacy,
schema/versioning, unchanged safety fields, test quality, artifact hashes, and
the prohibition on interpreting clear as independence. Resolve every P0/P1/P2
finding and obtain a fresh review before continuing.

- [ ] **Step 2: Reconcile active controls**

Record the reviewed candidate and evidence path. Keep SF-BL-005 `IN_PROGRESS`,
H2 incomplete, global disclosure pending, formal selection unauthorized, and
production readiness `NOT_READY`. Do not mark the parent item `VERIFIED`.

- [ ] **Step 3: Verify final source/evidence identity**

Run focused tests only if source changed after the full verification. Always
verify manifest hashes, `jq empty` for JSON, `git diff --check`, clean worktree,
and exact source parity with the reviewed candidate.

- [ ] **Step 4: Create and merge through PR**

Push the short-lived feature branch, create a PR describing the engineering-only
readiness result and formal limitations, verify a clean merge state, and squash
merge with exact head matching. Never push directly to `main` or bypass branch
protection.

- [ ] **Step 5: Post-merge readback**

Fetch `origin/main`; verify the merged tree equals the reviewed PR head and the
tested executable sources. Record the merge commit in the final status report.
