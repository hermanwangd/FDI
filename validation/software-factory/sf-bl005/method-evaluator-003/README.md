# METHOD evaluator 003 — local implementation evidence

Supporting evidence for `SF-BL-005-METHOD-EVALUATOR-003`, not project authority.
The user requested direct local implementation, without a new Coordinator task.

## What was implemented

- New Java 17 METHOD-pair scoring engine, separate from the old evaluator.
- Scenario coverage, complete-chain coverage, precision, recall and F1, with
  raw counts and undefined denominators represented as JSON `null`.
- Digest-pinned comparison runner and `method-pair-compare` application command.
- Explicit common source revision, production/test roots, producer-input snapshot
  digest and extractor digest. Both producer artifacts must match the binding.
- Producer snapshots are loaded before evaluator truth and proof-ledger inputs.
- Strict JSON, bounded regular files, symlink/traversal refusal, exact proof
  reference matching, and refusal to overwrite even identical prior output.

This completes the local scorer and comparison-input binding increment. It does
**not** migrate the production generation runner, inspect a real repository,
execute Graphify, recover additional METHOD chains, or establish new experimental
recall/F1. Existing PKB-001/SF-BL-002 artifacts and the in-flight extractor/resolver
remediation paths were not modified.

## Running it

After building the Java application:

```sh
java -Xmx512m -jar target/fdi-0.4.8.3.jar method-pair-compare \
  --manifest /absolute/evaluator-inputs/manifest.json \
  --manifest-sha256 <trusted-64-character-lowercase-sha256> \
  --output /absolute/evaluator-results/new-run.json
```

Use a previously reviewed/pinned manifest digest, not a digest supplied by an
untrusted producer alongside replacement inputs. The output parent must exist;
the output file must not exist. Flags use separate arguments, not `--key=value`.
Exit codes: `0` mechanics computed, `1` comparison refused, `2` invalid usage.
Refusal diagnostics do not echo evaluator contents or untrusted paths.

The application-entrypoint test runs this command in a separate bounded JVM;
it verifies dispatch without falling through to Spring startup.

## Input shape

The exact typed implementation is `MethodPairData.java`. All fields are required;
null, unknown, duplicate JSON properties and scalar coercions are rejected.
Collections reject null elements and are limited to 10,000 entries. Each input
file is at most 2 MiB; nesting is limited to 64. No identity normalization occurs.

The following is type notation, not a literal JSON fixture:

```text
Binding = {
  sourceRevision: full lowercase 40-character revision,
  productionRoots: [canonical repository-relative directory],
  testRoots: [canonical repository-relative directory],
  inputSnapshotSha256: lowercase SHA-256,
  extractorSha256: lowercase SHA-256
}
Artifact = { path: canonical path relative to manifest directory, sha256 }
Manifest = {
  schemaVersion: "SFBL005-METHOD-PAIR-001",
  datasetKind: "SYNTHETIC" | "CALIBRATION",
  binding: Binding,
  baseline: Artifact, improved: Artifact, truth: Artifact, proofs: Artifact
}
Method = { sourceRevision, path, signature }
Pair = { scenarioId, method: Method }
Edge = { scenarioId, from: Method, to: Method }
ProducerArtifact = {
  binding: Binding,
  proposals: {
    methods: [{ pair: Pair, role, evidenceRef }],
    edges: [{ edge: Edge, evidenceRef }],
    unresolvedScenarios: [scenarioId],
    unsupportedTypes: [diagnostic string]
  }
}
Truth = {
  selectedScenarios: [scenarioId],
  expectedPairs: [Pair],
  chains: [{ scenarioId, methods: [Method], edges: [Edge] }]
}
Proofs = {
  proposalsSha256: digest of that exact ProducerArtifact,
  methods: [{ pair: Pair, evidenceRef }],
  edges: [{ edge: Edge, evidenceRef }]
}
Ledger = { baseline: Proofs, improved: Proofs }
```

Method signatures use `declaring.type#method(parameter.types)`, including empty
parentheses for a zero-argument method, for example `demo.Service#find(int)`.
Inputs must already carry canonical identities; the evaluator does not resolve
aliases, infer overloads, or look up source symbols. Method paths must be Java
production paths, not tests. Expected pairs must belong to the bound revision,
selected scenarios and declared production roots.

Expected pairs and chain definitions cannot contain duplicate identities.
Duplicate proposed pairs collapse by exact Pair identity; role is diagnostic.
At least one matching independently sealed proof supports that pair; other
roles or repeated claims cannot create additional TP. A proposed expected pair
without a matching proof counts as FP **and** FN. Unknown but parseable subjects
remain FP. UNRESOLVED cannot also carry a method or edge claim for that scenario.

A missing chain definition makes overall chain coverage unavailable. An explicit
empty-method, empty-edge chain declares that scenario not applicable to the
chain denominator; it does not count as a completed chain. Nonempty chains need
every declared method and directed edge supported. Chain semantics and whether
all necessary edges were sealed are independent-review responsibilities.

## Trust boundary and remaining gates

The proof ledger is evaluator-only adjudication input, not a producer assertion.
Its author must independently check the claimed scenario-to-method/edge evidence
against the frozen source and observation/graph evidence. Matching its digest
and evidence reference **does not** prove that review occurred, that a mock
executed production code, or that the producer never saw gold.

Accordingly every report includes:

```json
{
  "readiness": "SCORING_MECHANICS_ONLY",
  "experimentDecision": "NOT_RUN"
}
```

`HOLDOUT` is rejected. Reports include the manifest digest, full input references,
binding, limitations, and separate baseline/improved metrics; they are evaluator
artifacts and must not be fed back into a blinded producer. No experimental GO,
Product truth, Human approval, independent review, or source-runtime verification
is manufactured from successful arithmetic or matching hashes.

Next dependent implementation remains the generic production runner and verified
Graphify calibration inputs, followed by isolated gold review, producer
improvement and a fresh comparable experiment. Existing remediation acceptance
is required before depending on its changed extractor/resolver behavior.

## Verification

See `verification.json` for the tested source hashes and results. Verification is
the implementer's local check, **not** an independent-review verdict. The full
suite is Java; the unchanged transitional Python control suite was also run.
