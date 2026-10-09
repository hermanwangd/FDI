# ENGCIM Swarm Module

This portable Maven module contains the Java Swarm implementation, contracts,
agent tests, role/Skill authoring and RC10 delivery evidence. Mission remains an
execution/request instance; this module introduces no new ENGCIM component.

## Current development entry

This README is the single documentation entry for the Human-selected Swarm development
work package covering all 19 existing role templates. This local Swarm 1.1 integration
candidate is on `codex/swarm-1-1-ssot-integration`, based on
`37c901fbdbc5dcc0fc858be489f0c9b17e68e83b`; the selected convergence source is
`e87ac10c96db4519264b418b6037aa7f5c86a790`. Each subject below has one
maintained body; other entries link here or to that body. Summaries and links do
not become competing instructions. This checkout is a local development
candidate, not integrated main, provider maintenance switchover or native adoption.

<!-- swarm:document-bodies:start -->
| Subject | Owning body | Applicability |
| --- | --- | --- |
| Development entry | [This README](README.md) | Current layout, navigation and local verification commands |
| Development method | [Swarm Dev Skill](skills/swarm-dev/SKILL.md) | Assigned Swarm Dev responsibility and development procedure |
| Role/profile rules | [ROLE-GUIDANCE.md](instructions/ROLE-GUIDANCE.md) | Selected candidate profile/amendment only; historical adoption has its own exact source |
| Source/version bindings | [SOURCE-TO-EFFECTIVE.json](SOURCE-TO-EFFECTIVE.json) | Source, derivation, provider observations and consumption gaps; not governing authority |
| Agent test use | [Four-role test README](tests/agents/four-role/README.md) | Active suites, checker commands and immutable fixture boundary |
| Ability coverage | [capabilities.json](tests/agents/capabilities.json) | All 19 roles, source/Skill references and historical/current tests; original 65 detailed mappings retained; no native PASS |
| Knowledge design | [RC10-KNOWLEDGE-DESIGN.md](docs/rc10/RC10-KNOWLEDGE-DESIGN.md) | Maintained review design, PROPOSED; not runtime authority |
| Capability design | [Capability DesignSpec addendum](docs/rc10/SWARM-AGENT-CAPABILITY-DESIGN-ADDENDUM.md) | DRAFT_FOR_REVIEW / NOT_ADOPTED / QA_SAMPLE_ONLY; links its proposed detailed QA requirements |
| Qualification design | [Q1 qualification plan](docs/rc10/SWARM-V11-Q1-QUALIFICATION-PLAN.md) | PROPOSED; pre-execution admission and freeze still required |
| Bootstrap use | [Workspace bootstrap runbook](../bootstrap/overlays/claude/engcim/skills/WORKSPACE-BOOTSTRAP-RUNBOOK-v0.1.md) | Existing Supervisor-owned procedure; resolve the selected runtime package separately |
| Workspace learning | [Workspace Learning profile](../bootstrap/overlays/multica/RC10-WORKSPACE-LEARNING.md) | Existing candidate role overlay; scope and authority remain in its body |
| Parent report procedure | [Parent-report procedure](../bootstrap/overlays/multica/RC10-ORCHESTRATOR-PARENT-REPORT.md) | Existing bounded Mission procedure; local source changes are not deployment |
| FDI authority | [Governing-source index](../../governance/GOVERNING-SOURCES.md) | Resolve exact approved authority through CURRENT and its lock; this README grants none |
| File classification | [FILE-CLASSIFICATION.md](../../docs/FILE-CLASSIFICATION.md) | Repository path and change boundaries |
<!-- swarm:document-bodies:end -->

Documentation ownership is distinct from executable ownership: the development
method stays in the Skill, selected runtime role rules stay in their instructions,
and schemas stay in `contracts/`. This table points to their bodies without copying
them. Bootstrap [MAPPING.md](../bootstrap/multica/MAPPING.md) retains workspace/profile
routing and dated deployment lineage; it is not a second Swarm development handbook.

## Source and test layout

```text
engcim/swarm/
  instructions/                maintained role/profile bodies and transformations
  skills/                      maintained Skills and CLI transformations
  src/main/java/               Java 17 runtime
  src/test/java/               Java tests and source/document consistency checks
  tests/agents/four-role/       active definitions, checkers and immutable fixtures
  generated/                   derived Reviewer/CLI delivery bytes
  SOURCE-TO-EFFECTIVE.json      existing source/target/version bindings
  docs/rc10/                   scoped designs, plans and historical reports
  baselines/rc6/               sealed historical baseline
```

The 19 role instruction objects have these single editing entries. Reviewer's
complete body remains derived from its existing transformation source.

| Existing role | Instruction editing entry | Assigned Skills | Test-reference status |
| --- | --- | ---: | --- |
| Swarm Orchestrator | [owning source](instructions/orchestrator/instructions.md) | 4 | Existing four-role definitions + historical six cases |
| Swarm Product Manager | [owning source](instructions/product-manager/instructions.md) | 4 | Six historical draft cases; current admission missing |
| Swarm Knowledge Curator | [owning source](instructions/knowledge-curator/instructions.md) | 8 | Six historical draft cases; current admission missing |
| Swarm Architect | [owning source](instructions/architect/instructions.md) | 6 | Existing four-role definitions + historical six cases |
| Swarm Researcher | [owning source](instructions/researcher/instructions.md) | 1 | Six historical draft cases; current admission missing |
| Swarm Coder | [owning source](instructions/coder/instructions.md) | 4 | Six historical draft cases; current admission missing |
| Swarm Writer | [owning source](instructions/writer/instructions.md) | 1 | Six historical draft cases; current admission missing |
| Swarm Data Analyst | [owning source](instructions/data-analyst/instructions.md) | 2 | Six historical draft cases; current admission missing |
| Swarm Security Auditor | [owning source](instructions/security-auditor/instructions.md) | 2 | Six historical draft cases; current admission missing |
| Swarm QA Tester | [owning source](instructions/qa/instructions.md) | 6 | Existing four-role definitions + historical six cases |
| Swarm DevOps | [owning source](instructions/devops/instructions.md) | 6 | Six historical draft cases; current admission missing |
| Swarm Reviewer | [owning source](instructions/reviewer/source.json) | 4 | Existing four-role definitions + historical six cases |
| Swarm Verifier | [owning source](instructions/verifier/instructions.md) | 6 | Six historical draft cases; current admission missing |
| Swarm Backend Dev | [owning source](instructions/backend-dev/instructions.md) | 4 | Six historical draft cases; current admission missing |
| Swarm Frontend Dev | [owning source](instructions/frontend-dev/instructions.md) | 4 | Six historical draft cases; current admission missing |
| Swarm DBA | [owning source](instructions/dba/instructions.md) | 4 | Six historical draft cases; current admission missing |
| Swarm SRE | [owning source](instructions/sre/instructions.md) | 7 | Six historical draft cases; current admission missing |
| Swarm Release Manager | [owning source](instructions/release-manager/instructions.md) | 4 | Six historical draft cases; current admission missing |
| Swarm Performance Engineer | [owning source](instructions/performance-engineer/instructions.md) | 3 | Six historical draft cases; current admission missing |

The existing [source map](SOURCE-TO-EFFECTIVE.json) binds each role to its exact
agent/workspace, observed model/runtime, enabled Skill set and source version.
The shared [capability inventory](tests/agents/capabilities.json) now covers all 19
roles: it retains 65 four-role mappings and references the original 114 draft
cases without copying suites or transferring old PASS results. The section index
retains original responsibilities, prohibitions, exceptions and repeated overlays;
clause-level capability completeness and current test coverage remain unfinished.

All 19 current bodies and 31 distinct assigned Skill bodies were read through the
supported provider CLI on 2026-10-09. Fifteen previously missing role bodies and
29 Skill bodies are now maintained here byte-for-byte. [skills/](skills/) owns the
Skill bodies; multica-cli remains derived from its existing [source.json](skills/multica-cli/source.json).
This is **new local maintenance convergence**; global upstream and automatic sync
remain UNKNOWN. Eighty-two attached Skill files are retained and individually
pinned in readback evidence; their editable maintenance has not been converged.
Body source ownership does not establish complete Skill-package maintenance,
render/loading, runtime behavior or regression acceptance.

Edit owning sources and version them with their compatible tests in Git. The two
transformation records retain hashed immutable provenance under
`validation/rc10/current-work-package-candidate/supplemental-current-source-r13/`;
regenerate `generated/` from those records instead of editing payloads. Fixtures,
provider observations and derived bytes are separate from authoring. Publication
uses the applicable scoped procedure in the Swarm Dev Skill and source map; this
README neither grants publication nor authorizes whole-bundle bootstrap.

The separately published `codex/swarm-dev-lineage-20261009` Skill branch remains
**not integrated** here, as recorded in the source map. Its old bootstrap path is
not a second editing entry. Earlier layout versions are available through Git;
use `git log --follow -- <path>` rather than adding version-suffixed active copies.
For the relocated and amended role-guidance body, `development_ssot.integration.movedSources`
retains the exact earlier path/commit/digest; automatic rename detection alone may not follow it.

## Plans, historical evidence and generated indexes

The files below keep their own subjects and recorded revisions. A heading saying
“current” inside an older report means current at that report's checkpoint, not
current acceptance of this Swarm improvement work package.

| Material | Entry and retained scope |
| --- | --- |
| RC10 implementation backlog | [Implementation plan](docs/rc10/RC10-IMPLEMENTATION-PLAN.md): bounded RC10 plan with dated checkpoints; not this repair's execution approval |
| RC10 implementation evidence | [Implementation report](docs/rc10/RC10-IMPLEMENTATION-REPORT.md) and [evidence manifest](docs/rc10/RC10-EVIDENCE-MANIFEST.md): recorded 2026-09-27 candidate identities |
| RC10 test evidence | [Integration report](docs/rc10/RC10-INTEGRATION-TEST-REPORT.md) and [regression report](docs/rc10/RC10-REGRESSION-REPORT.md): results for their original candidates |
| RC10 gaps and deployment evidence | [Gap report](docs/rc10/RC6-TO-RC10-GAP-REPORT.md), [source/runtime matrix](docs/rc10/RC6-TO-RC10-SOURCE-TO-RUNTIME-GAP-MATRIX.md), [remainder report](docs/rc10/RC10-REMAINDER-GAP-REPORT.md) and [runtime materialization record](docs/rc10/RC10-RUNTIME-MATERIALIZATION.md): revision-bound evidence, not fresh readback |
| Company import proposal | [Folder map](docs/rc10/RC10-COMPANY-REPO-IMPORT-FOLDER-MAP.md) and [import manifest](docs/rc10/RC10-COMPANY-REPO-IMPORT-MANIFEST.json): proposed import boundary and pinned checkpoint; no company adoption |
| v1.1 review/evidence units | [Three-PR split](docs/rc10/SWARM-V11-THREE-PR-SPLIT.md) and [Mac integration evidence](docs/rc10/SWARM-V11-MAC-INTEGRATION.md): their recorded subjects and limits |
| Candidate package index | [Release README](release/RC10-CANDIDATE-PACKAGE/README.md): retained package/checkpoint navigation, not current role acceptance |

Historical runs and publication receipts under `validation/rc10/`, dated plans
under `docs/superpowers/`, sealed RC6 and versioned Supervisor packages retain their
original bytes and identities. They are not alternate editing entries. Root
`release/PROJECT-TREE.txt`, `MARKDOWN-INVENTORY.txt` and `MANIFEST.json` are generated
repository indexes, never document authority. Update current navigation when a
source moves; do not rewrite old run receipts to match new paths.

## Local verification

Run from the repository root with Java 17, one fork and bounded heaps:

```sh
MAVEN_OPTS=-Xmx512m ./mvnw -pl engcim/swarm -Dtest=SwarmSourceLayoutTests,CurrentWorkPackageAuditTests,JavaOnlySourcePolicyTests -DargLine=-Xmx512m -DforkCount=1 test
```

`SwarmSourceLayoutTests` verifies owning pins, exact derivation, fixture integrity,
unique document subjects, valid local navigation and source-map agreement. The
existing [Node checker self-tests](tests/agents/four-role/README.md#existing-local-checks)
remain independently runnable. These checks do not dispatch, publish or prove
native role behavior.

For the module suite or company import check, respectively:

```sh
MAVEN_OPTS=-Xmx512m ./mvnw -pl engcim/swarm -DargLine=-Xmx512m -DforkCount=1 test
MAVEN_OPTS=-Xmx512m ./mvnw -pl engcim/swarm -Dtest=CompanyImportManifestTests -DargLine=-Xmx512m -DforkCount=1 test
```

The company import baseline already fails on the OrchestratorReportAudit digest;
source/document checks do not clear that failure. Full role render/loading,
normal/negative native behavior and independent behavioral acceptance remain
unfinished. The [runtime composition script](tooling/verification/verify_swarm_runtime.sh)
requires the active workspace overlay and its applicable authorization; it is a
separate consumer action, not part of these offline checks.
