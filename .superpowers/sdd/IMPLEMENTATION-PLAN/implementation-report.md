# BL-005 / BL-023 implementation report

## Scope delivered

- Java 17 Product Semantics contracts for immutable Capability/scenario
  proposals, separate evidence references, uncalibrated confidence labeling,
  exact proposal decision binding, Human Reviewer provenance, DRAFT/FROZEN
  lifecycle, and parent-Capability-before-child Forward eligibility.
- Draft 2020-12 `scenario-proposal.schema.json` for proposal and individual
  review surfaces.
- Bounded `pk-scenario-proposal` generation skill with an exact four-kind input
  allowlist and explicit gold, judgment, and accepted-Forward-semantics
  isolation.
- Deterministic Python proposal validation and bilingual zh-TW/English review
  rendering. Evidence references resolve to one graph node/link or history
  commit/pull request and render the cited label/subject for Human Reviewer
  inspection.
- Exclusive immutable output publication with an atomic run-ID claim, paired
  JSON/Markdown rollback on collision, and no overwrite behavior.

## Authority and lifecycle behavior

- Generated artifacts remain `PROPOSAL_ONLY / UNREVIEWED`; all Capability and
  scenario decision fields are null when rendered.
- ACCEPT/EDIT/REJECT records bind the exact original proposal revision and byte
  digest. The review validator reloads the immutable original and rejects base
  semantic edits outside an explicit EDIT replacement.
- Rejected Capabilities exclude accepted child scenarios. REJECT and
  unconfirmed EDIT never enter the accepted candidate set. Only frozen,
  Human-Reviewer-approved Java lifecycle records are Forward-eligible.
- Rendering and validation do not freeze or publish Product Semantics.

## Verification evidence

- TDD RED observed for missing Java contracts and missing Python module.
- `MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=ScenarioProposalContractsTests test`
  passed on JDK 23.0.2 while compiling with the POM's Java 17 release target.
- `python3 -m pytest -q tests/test_pkb001_scenario_review.py` passed 39 tests.
- The generated Petclinic draft validated with 6 Capability proposals and 48
  resolving evidence references.

## Known boundary

Behavior/evidence separation uses schema plus mirrored Java/Python linting for
obvious code identifiers, qualified names, call notation, and source/template
paths. Natural-language detection is intentionally bounded; Human Reviewer
inspection remains authoritative. The pre-existing untracked duplicate
`GraphifyBindingEvidence 2.java` is preserved at SHA-256
`5091b9d7ae16e330c995945fcee8a9da773a334798d4a5784b5bc80989f6cdb3` and is
temporarily moved only around Maven verification because it otherwise
duplicates a public Java class. The safe command creates a `mktemp -d` staging
directory, installs `EXIT HUP INT TERM` restoration traps before moving the
file, runs Maven with `MAVEN_OPTS='-Xmx2g'`, restores the file explicitly, and
then clears the traps. Full-suite attempts reached passing test report emission
but hung while opening older iCloud-dataless Surefire report files; the focused
new Java suite completed normally.
