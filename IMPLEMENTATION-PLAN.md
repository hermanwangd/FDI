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
3. **New immutable calibration evidence**
   - Generate a new run under `validation/software-factory/sf-bl002/`; never
     overwrite `validation/pkb001/`.
   - Report separate counts for mechanically resolved production references,
     production-resolution gaps, external-dependency diagnostics, and
     test-helper diagnostics. Do not present their sum as mapping failure.
   - Bind exact source/code revisions, inputs, tool versions, commands, outputs,
     SHA-256 digests, limitations, and before/after scenario/chain/component
     metrics.
4. **Combined verification**
   - Run `MAVEN_OPTS='-Xmx2g' ./mvnw -q clean test` and public evidence
     validation.
   - Review evaluator isolation, production-only path enforcement, deterministic
     output, and unchanged historical evidence before preparing closure.

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
- Exact base and Spec revision: `f6ac9d0d389c6857ec0374575bd5af312b704ae9`
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
