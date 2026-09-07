# PKB-001 Implementation Plan

This file defines how currently selected work is delivered. `FRAMEWORK-SPEC.md`
defines what; `BACKLOG.md` records requirement maturity; `STATUS.json` records
the current execution state and next action.

## Current selection

### PKB-BL-009 Java test behavior extractor

- Backlog / requirement: `PKB-BL-009` / `PKB-REVERSE-002`
- Spec revision: `c396b3cf6e3a32d55c1fb57827f2022e4409df8d`
- Base commit: `1423192773fce7c4f6d1efb8dd3d2abbbebbb3e3`
- Execution ID: `PKB-BL-009-JAVA-TEST-BEHAVIOR-001`
- Accepted discovery evidence: candidate
  `4c1a2bae3850028de25b3fd84cc07ea67640dbb6`, independently reviewed PASS;
  integrated as commits `16bac6d` and `1423192`.
- Discovery result: Graphify indexed 20/20 test files and exposed 76/76 `@Test`
  methods, but only 5/18 `@Test`-bearing files had provider-native production
  links. The verified outcome is `GAP`, so this execution adds a Java-owned,
  provider-neutral mechanical test-behavior extractor without changing
  Graphify or assigning Product meaning.

#### Architecture and fixed dependency

- Add Maven dependency
  `com.github.javaparser:javaparser-symbol-solver-core:3.28.2`; do not use a
  floating version. JavaParser and its symbol-solver artifacts target Java 8
  bytecode and are compatible with the framework's Java 17 runtime.
- Create a separate `TestBehaviorEvidenceProvider` implementation. It emits
  mechanical observations only and does not replace `CodeIntelligenceProvider`
  or the Graphify adapter.
- Configure `CombinedTypeSolver` with `ReflectionTypeSolver`, then
  `JavaParserTypeSolver` for `src/main/java` and `src/test/java`. Restrict
  resolution to those source roots: do not build the target repository, run
  annotation processors, download its dependencies, or infer missing types.
- Run extraction deterministically in a single thread because
  `JavaParserFacade` is not thread-safe. Preserve unresolved symbols and their
  source locations as explicit evidence gaps; external API calls may remain
  syntactic observations.
- Bind every result to Petclinic revision
  `818c4136ea971c21674525f9053de0d9c7ad8cfe` and frozen source digests.

#### Delivery DAG

1. **Slice A — contract foundation (sequential):** add the pinned Maven
   dependency, provider-neutral Java records/interfaces, stable error vocabulary,
   and focused contract tests. Review and accept A before parallel work.
2. **Slice B — Java AST extractor (after A):** own the JavaParser adapter package;
   extract test methods, annotations, source locations, fixtures/setup calls,
   assertions, referenced production types/methods, and unresolved references.
3. **Slice C — schema and validation (after A, parallel with B/D):** own the
   evidence schema and Java validator; enforce revision, path, identity,
   determinism, authority, and unresolved-evidence rules.
4. **Slice D — Petclinic fixtures and golden evidence (after A, parallel with
   B/C):** own test resources and exact-revision expected mechanical cases,
   including same-package references, overloaded calls, helpers, unresolved
   external symbols, nested tests, and negative paths. Do not encode Capability
   or scenario truth in fixtures.
5. **Slice E — combined integration (after B/C/D):** connect the provider and
   Java CLI, generate one immutable Petclinic evidence package, replay twice for
   byte determinism, and run focused plus full regression verification.
6. **Slice F — independent review (after E):** a distinct reviewer who did not
   produce or integrate the candidate checks the exact candidate, source
   identity, countable claims, negative cases, clean-export tests, and scope.
   Remediation returns to the owning slice and requires a fresh exact-candidate
   review.

The Delivery Coordinator receives the entire DAG and may run B, C, and D in
parallel after A is accepted. It performs combined integration and returns one
delivery evidence package. No per-slice Human confirmation is required; Human
Authority is reserved for terminal parent closure or a material Plan/Spec change.

Owned paths are limited to `pom.xml`, the new Java test-behavior contract,
extractor, validator and CLI packages, their JUnit tests/resources, the new
provider-neutral schema, and a new immutable BL009 Java-extractor evidence
directory. The five active controls, existing immutable runs, accepted
semantics, evaluator material, skills, and external Graphify runtime are
read-only to the Execution Plane.

#### Acceptance and negative cases

- The Java extractor accounts for all 18 `@Test`-bearing Petclinic files and all
  76 `@Test` methods; each item has a repository-relative source location.
- Same-package production references and method/type evidence are recovered
  where source-root resolution permits. Unresolved or ambiguous references are
  retained and never converted into invented links.
- Output is provider-neutral, deterministic across two clean runs, and bound to
  the exact source revision and input digests.
- Malformed paths, revision/digest mismatch, duplicate identities, unsupported
  schema versions, and escaped source roots fail closed.
- Extraction produces behavior evidence only. It must not generate Capability
  names, scenario wording, Product truth, evaluator labels, or semantic
  publication decisions.

#### Verification and handoff

Run within the 8 GB limit:

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw clean package -q
python3 -m pytest -q
python3 -m json.tool STATUS.json
git diff --check
```

Return one exact combined candidate containing accepted slice identities,
changed paths, generated evidence and digests, limitations, review/remediation
history, verification results, and token/cycle-time/first-pass KPIs. The Feature
Delivery Plane reconciles it before changing Backlog maturity. This execution
does not generate or evaluate Reverse Capability/scenario proposals and cannot
close `PKB-BL-009`.

## Verified delivery ledger

| Backlog | Delivered behavior | Evidence |
|---|---|---|
| `PKB-BL-004` | Deterministic evaluator-only third review of exactly 11 frozen disagreements | Candidate `45b4ba3def00d7b8adfd55153a497788b531a38a`; `validation/pkb001/task7-evaluation/third-review-adjudication-evidence.json`; independent review PASS; Java 731, Python 62, public validation 9/9 |
| `PKB-BL-005` | Machine-verifiable scenario proposal and review lifecycle | Contract and validator tests |
| `PKB-BL-008` | Frozen Graphify capability and live MCP contract | `validation/pkb001/runtime/bl008-stage1-integration-evidence.json` |
| `PKB-BL-009` discovery | Graphify test indexing supported; test-to-production relationship coverage classified `GAP` | Candidate `4c1a2bae3850028de25b3fd84cc07ea67640dbb6`; `validation/pkb001/reverse-pkb-bl009-petclinic-001/provider-discovery/discovery-evidence.json`; fresh independent review PASS |
| `PKB-BL-018` | Durable structural component identity | Java identity tests |
| `PKB-BL-019` | Immutable realization proposal contract | Java authority and revision tests |
| `PKB-BL-020` | Proposal-only generation and evaluator-gold isolation | Isolation tests |
| `PKB-BL-021` | Hierarchical component comparison | Deterministic comparator tests |
| `PKB-BL-022` | Fail-closed next-run readiness gate | Gate and clean-copy tests |
| `PKB-BL-023` | Evidence-backed scenario proposal generation | Review artifacts and validator tests |
| `PKB-BL-024` | Active review pointers | Control-file tests |
| `PKB-BL-026` | 15/15 repository-owned Python consumers migrated to Java | `validation/pkb001/java-migration/python-framework-inventory.json` |
| `PKB-BL-027` | Portable Graphify runtime and bounded Java stdio-MCP lifecycle | `validation/pkb001/runtime/pkb-bl027-portable-runtime-evidence.json` |

Commit-level history, slice handoffs, test counts, and superseded plans remain in
Git history and immutable evidence; they are not duplicated here.

## Next experiment construction sequence

Before another experiment:

1. Complete the remaining scenario decisions and evaluator disagreement review.
2. Freeze a new scenario-bearing semantics revision.
3. Reconcile scenario-grounded PK-S1 under the Java framework target.
4. Improve Reverse proposal controls and provider-neutral evaluator identity.
5. Define separate scenario, chain, exact-component, and diagnostic metrics.
6. Preregister justified thresholds and approve one exact-revision holdout.
7. Freeze all protocol inputs, regress Petclinic, execute the sealed holdout once.
8. Review the evidence and issue `GO`, `REVISE`, or `STOP`.

The exact current counts, blocker, selected Backlog, and next action are read only
from `STATUS.json`.

## Selection template

Before implementation begins, bind this file and `STATUS.json` to:

- one Backlog ID and normative requirement;
- exact base commit and owned files;
- in-scope and excluded behavior;
- observable acceptance criteria and negative cases;
- focused tests and full regression commands;
- independent review expectations where required.

On completion, replace construction detail with one short ledger row and clear
the active selection. An agent stops with `CONTEXT_CONFLICT` if the five active
files disagree.

## Default verification

Run within the 8 GB system limit:

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw clean package -q
python3 -m pytest -q
python3 validation/pkb001/task7-evaluation/public_validate.py .
python3 -m json.tool STATUS.json
git diff --check
```
