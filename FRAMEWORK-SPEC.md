# PKB-001 Framework Specification

**Status:** Prototype evaluated — `REVISE`

## Objective

PKB-001 validates whether product meaning, source structure, and delivery evidence can support reliable Product Realization discovery without allowing machine observations to become Product truth automatically.

## Normative requirement index

This revision supersedes the Stage A/B requirements `PKB-REVIEW-001`,
`PKB-REVIEW-002`, `PKB-STATUS-001`, `PKB-SCENARIO-001`, and
`PKB-SCENARIO-002`. Their replacement IDs below are new because their
semantics changed. Retired IDs must not be reused.


These identifiers are stable traceability anchors. The controlling requirement
is the normative wording in the referenced section, not a restatement in a
Backlog item or Implementation Plan. Changing a requirement's meaning requires
a new Spec revision and reconciliation of every bound Backlog item.

| Requirement ID | Controlling section | Required outcome |
|---|---|---|
| `PKB-COMPONENT-001` | Component roles and granularity / Normalized structural identity | Java enforces provider-neutral component identity, granularity, and canonical revision/path rules. |
| `PKB-PROPOSAL-001` | Component roles and granularity | Java enforces immutable proposal authority, outcome, component roles, and revision consistency. |
| `PKB-ISOLATION-001` | Data flow and isolation | PK-S1 generation is proposal-only and cannot access evaluator gold or post-generation judgments. |
| `PKB-COMPARISON-001` | Hierarchical evaluation | Provider-neutral comparison keeps path, type, bare symbol, exact component, chain, and supporting diagnostics distinct. |
| `PKB-READINESS-001` | Planned project placement and verification | The next-run gate selects the exact skill/input set and fails closed on binding, identity, authority, or run-ID conflicts. |
| `PKB-REVIEW-003` | Scenario authority and isolation | Generate evidence-backed Capability and scenario proposals for individual review. |
| `PKB-STATUS-002` | Scenario authority and isolation | Active status identifies the proposal review surface and actual review state. |
| `PKB-REVIEW-004` | Scenario authority and isolation | The user accepts, edits, or rejects proposals with version-bound decisions. |
| `PKB-EVAL-LEGACY-001` | Current bounded decision | Required evaluator disagreements are adjudicated without gaining Human Reviewer authority. |
| `PKB-SCENARIO-003` | Product Capability behavior scenarios | Behavior scenarios have a provider-neutral, Human Reviewer-owned, immutable lifecycle contract. |
| `PKB-SCENARIO-004` | Scenario authority and isolation | Only frozen approved scenarios enter Forward generation; Reverse hypotheses remain isolated. |
| `PKB-MAPPING-001` | Scenario-grounded realization | Proposals trace scenarios through variable realization chains to justified component roles. |
| `PKB-PROVIDER-001` | Template and UI evidence | UI/template claims use verified provider capability or declare an evidence gap. |
| `PKB-REVERSE-001` | Reverse experiment / Scenario authority and isolation | Reverse quality controls improve proposals without publishing Product truth. |
| `PKB-EVAL-001` | Hierarchical evaluation | Evaluator truth uses provider-neutral normalized component identity and remains isolated. |
| `PKB-EVAL-002` | Hierarchical evaluation | Semantic, scenario, chain, component, and provider-native diagnostic metrics remain distinct. |
| `PKB-CALIBRATION-001` | Calibration strategy | Acceptance thresholds derive from declared error costs and are frozen before generation. |
| `PKB-HOLDOUT-001` | Calibration strategy | An independently proposed exact-revision holdout requires explicit user approval and sealing. |
| `PKB-PROTOCOL-001` | Calibration strategy | The experiment protocol binds exact revisions and digests and fails closed on change. |
| `PKB-REGRESSION-001` | Calibration strategy | Petclinic regression uses a new immutable run and cannot overwrite completed artifacts. |
| `PKB-HOLDOUT-002` | Calibration strategy | The sealed blind holdout executes once under the unchanged frozen protocol. |
| `PKB-DECISION-001` | Current bounded decision | experiment result produces a bounded decision without automatic semantic publication. |

## Forward experiment

```text
Product Semantics
+ Graphify Structural Intelligence
→ Capability → Component Mapping
```

Given a Product Capability, the experiment measures whether exact-revision structural evidence identifies its correct technical realization.

## Reverse experiment

```text
Graphify Structural Intelligence
+ Git / PR / Feature Delivery History
→ Capability Hypotheses
→ Human Review
```

Reverse results are proposals. They cannot establish Product semantics or publish Product truth without Human Reviewer review.

## Ownership boundaries

- Human Reviewer owns Product meaning and accepted Capability definitions.
- Graphify supplies structural observations, not Product semantics.
- Git, pull requests, and feature history supply delivery evidence, not Product truth.
- Human/evaluator review accepts, renames, merges, splits, rejects, or identifies missing proposals.

## Product Capability behavior scenarios

A Product Capability MAY contain Human Reviewer-owned behavior scenarios that
describe concrete, externally observable examples of the capability. Scenarios
clarify Product meaning; they do not define technical realization.

Each scenario has a stable `scenario_id`, parent `capability_id`, title,
`given` preconditions, a `when` action or event, observable `then` outcomes,
scope, status, and Human Reviewer approval provenance. Scenario scope is either
`REQUIRED_ACCEPTANCE` or `ILLUSTRATIVE`; status is either `DRAFT` or `FROZEN`.
Capability-level `includes`, `excludes`, and `non_goals` define the shared
semantic boundary. A frozen scenario is immutable; changing its meaning
requires a new Product Semantics revision, and its identifier must never be
reused for different behavior.

Scenarios describe product behavior, not implementation. They MUST NOT contain
source paths, packages, classes, methods, fields, Graphify node identifiers,
provider-native identifiers, evaluator expected mappings, or technical
selection instructions. A scenario is an explicit acceptance example, not a
claim that every valid behavior of the Capability has been enumerated.

Human Reviewer may define required UI or template behavior even when the current
structural provider cannot observe it. Missing provider evidence never
authorizes an agent to weaken or rewrite Product Semantics. The realization
proposal must instead record the evidence gap or return `UNRESOLVED` when the
core behavior cannot be supported.

### Scenario authority and isolation

The user is the sole Human Reviewer / Experiment Owner for this prototype.
No Product Team organization, Stage A/Stage B packet split, or formal semantic
publication workflow is required.

Skills/agents generate Capability hypotheses and behavior scenario proposals
from exact-revision Graphify structural evidence plus cutoff-bounded Git/PR/
feature delivery history. Each proposal records its source revision, graph
digest, history cutoff and evidence references, inference rationale, limitations,
and confidence (0–1, a ranking hint rather than a calibrated probability).
Unavailable evidence is explicitly marked; an empty evidence claim or invented
reference is invalid. Both evidence channels are inspected where available,
but a scenario need not have supporting evidence from both. Unsupported
behavior is identified as a hypothesis, not reported as an observed fact.

Scenario text (title, Given/When/Then and semantic boundaries) remains free of
implementation identifiers. A separate evidence envelope carries structural
identifiers, paths and history references so the user can inspect the basis.

Generated proposals use `HYP-SCENARIO-*`, `authority: PROPOSAL_ONLY` and
`scenario_status: UNREVIEWED`. A single review surface shows the proposed
behavior, evidence, rationale, confidence, limitations and decision fields.
The user chooses `ACCEPT`, `EDIT` or `REJECT`. Each decision binds reviewer
identity, time, reason and exact proposal revision/digest. EDIT records the
replacement text and explicit acceptance of that exact edited version; an
unconfirmed edit remains pending. REJECT never enters accepted semantics.

Only accepted versions may be copied into a new immutable, `FROZEN`
experiment Product Semantics snapshot. Proposal originals remain immutable
and proposal-only; the accepted snapshot links back to proposals and decisions.
The user can approve the accepted set and freeze in one review action.
An agent must never manufacture that approval. Existing snapshots are unchanged.

Forward generation consumes only that reviewed snapshot. Reverse generation
cannot read the accepted snapshot, evaluator gold, or post-generation judgments.
Technical evidence may be visible to the human reviewer; generation must still
remain isolated from evaluator truth. Evaluation cannot silently edit semantics.

Because scenarios inferred from the same repository are later mapped back to
it, this experiment measures reconstruction consistency, not independent
validation of intended product requirements. Reports record input provenance,
reviewer exposure and this limitation; confidence and exact-match scores do
not remove it.

### Scenario-grounded realization

Forward generation follows:

```text
Frozen Capability + frozen behavior scenarios + exact-revision evidence
→ scenario traces → realization chain → component proposal
```

A scenario trace links a scenario to an ordered, variable-length realization
chain. Each chain step describes a behavioral function, references zero or more
proposed components, and records `EVIDENCED`, `EVIDENCE_GAP`, or
`NOT_APPLICABLE`. Chains are not forced into a fixed controller, domain,
persistence, or UI layering model. `NOT_APPLICABLE` requires a reason and
remains proposal-only unless Human Reviewer confirms the semantic assertion.
Given, When, and Then clauses do not require one-to-one component mappings.

Component role remains behavioral:

- `PRIMARY`: without this component, the scenario's core behavior cannot be
  realized.
- `SUPPORTING`: the component supplies data, validation, configuration,
  orchestration, or surrounding structure without independently performing the
  core behavior.

Direct method evidence must not be replaced by a containing class without an
explicit reason, but no universal method-first hierarchy is allowed. A type,
method, template, or configuration may be primary when the evidence supports
that role. Every proposed component has a stable proposal-local reference,
selection reason, structural identity, and traceable scenario-chain use.

Mapping outcome and evidence completeness are separate dimensions:

```text
outcome: MAPPING_PROPOSAL | UNRESOLVED
evidence_status: COMPLETE | PARTIAL | INSUFFICIENT
```

Evidence gaps are explicit. They do not authorize fabricated links, automatic
semantic changes, or promotion of supporting evidence to formal component
credit.

## Architecture

```text
Product Semantics → Capability → Product Realization → Component / Repository

CodeIntelligenceProvider → Graphify adapter → Graphify runtime
Graphify → Structural Intelligence
Git / PR / Feature History → Delivery Intelligence

Structural Intelligence + Delivery Intelligence
→ Capability Hypothesis → Human Reviewer Review
```

Graphify operations must be discovered from the installed runtime. Structural evidence must bind the indexed source to an exact Git revision and frozen source snapshot.

Graph node source paths are repository-relative. Runtime/link provenance may retain extraction-time absolute paths and is therefore checkout-specific rather than portable; the frozen graph bytes are not normalized after extraction.

## Component mapping contract design

### Considered approaches

1. **All Java:** rejected because rewriting the installed Graphify Python MCP runtime and Python experiment harness adds risk without improving the provider boundary.
2. **All Python:** rejected because component identity and validation would bypass the Java 17 FDI runtime and create a second product contract.
3. **Hybrid, selected:** Java owns durable contracts and provider validation; the existing Python Graphify runtime and PKB-001 evaluation tooling retain their bounded responsibilities; skills perform proposal-only semantic selection.

### Language and responsibility split

- **Java 17 / Spring Boot 3.4.1** owns the durable FDI runtime contract: component roles, component granularity, normalized structural identity, validation, exact-revision binding, and the provider-neutral `CodeIntelligenceProvider` boundary.
- **Graphify's installed Python runtime** remains an external MCP stdio provider behind the Java Graphify adapter. PKB-001 does not rewrite or assume unsupported Graphify APIs.
- **Skills/agents** perform semantic candidate selection from allowed Product Semantics and exactly bound structural evidence. Their output is always `PROPOSAL_ONLY` and must satisfy the Java-owned contract.
- **Python tooling** owns experiment isolation, blinded evaluation, hierarchical comparison, metrics, deterministic reports, and human-review packet generation. Python evaluation contracts must mirror, not redefine, the Java contract.

### Component roles and granularity

Every proposed realization component has one role:

- `PRIMARY`: directly performs the capability behavior.
- `SUPPORTING`: supplies data, validation, configuration, orchestration, or surrounding structure without independently performing the behavior.

Every component has one explicit granularity:

- `REPOSITORY`, `FILE`, `TYPE`, `METHOD`, `TEMPLATE`, or `CONFIGURATION`.

A mapping proposal must contain at least one `PRIMARY` component unless its outcome is `UNRESOLVED`. A containing class or file must not replace a more precise method node when direct method evidence is available unless the proposal records an explicit, evidence-backed reason that the broader component is the behavioral realization. Supporting evidence remains separate from formally proposed components.

### Normalized structural identity

The stable comparison identity contains:

```text
source_revision
source_path
granularity
qualified_symbol
provider_node_id
```

`source_revision`, repository-relative `source_path`, and `granularity` are mandatory. `qualified_symbol` is mandatory for symbol-level components. `provider_node_id` preserves Graphify provenance but is not, by itself, a provider-neutral identity. Normalization must never use evaluator gold or post-generation judgments.

### Hierarchical evaluation

Evaluation reports separate these levels rather than treating them as interchangeable:

1. source-path match;
2. containing type/class match;
3. qualified-symbol-name overlap as a diagnostic independent of path and type;
4. exact component match using `(source_path, containing_type, qualified_symbol)`;
5. expected realization-chain coverage based only on exact component matches;
6. missing and extra components based only on exact component identity.

Path, type, or symbol-name overlap and supporting-evidence citation do not count as exact component matches. Supporting citations report symbol-name and exact-component overlap separately, but neither grants proposal credit. Rows in the proposed channel may explicitly declare only `PRIMARY`; rows in the supporting channel may explicitly declare only `SUPPORTING`; evaluator-expected roles do not grant proposal credit. Comparison inputs are snapshotted once with a 10,000-component-per-channel safety bound. The existing Petclinic metrics remain descriptive regression evidence. New acceptance thresholds must be registered before the next blind/holdout execution and must not be selected from the observed Petclinic result.

Next-run evaluation separates Product semantic quality from technical
realization quality. Semantic measures include Human Reviewer acceptance,
rename/merge/split/reject decisions, duplicate or composite hypotheses, and
unsupported behavior claims. Technical measures include scenario evidence
coverage, complete-chain coverage, provider-neutral exact-component precision,
recall and F1, missing and extra components, unresolved rate, and UI/template
evidence gaps. Macro per-Capability results are reported so large mappings do
not dominate the aggregate.

The formal provider-neutral exact-component identity is
`(source_revision, source_path, granularity, qualified_symbol)`. Graphify node-ID
exact match remains a provider-native provenance diagnostic. Path, containing
type, bare-symbol overlap, and supporting citations remain separate diagnostics.
These metrics must not be given the same name or substituted for one another.
Next-run evaluator truth therefore records normalized component identity rather
than relying only on provider node IDs.

### Data flow and isolation

```text
Frozen Product Semantics + frozen Human Reviewer behavior scenarios
+ exact-revision Graphify evidence
→ scenario-grounded Skill/agent proposal generation without evaluator gold
→ Java contract validation
→ immutable proposal artifact
→ Python blinded evaluation against sealed evaluator truth
→ Human Reviewer experiment result realization review
```

Generation must fail closed when identity, granularity, role, source revision, or provider binding is absent or inconsistent. The Human Reviewer decides capability meaning and boundaries; evaluator comparison measures realization quality but cannot publish Product Semantics.

### Template and UI evidence

Before adding template extraction, the installed Graphify runtime must be queried to prove that it supports the required source type and relationships. If it does not, the prototype must either introduce a separately identified structural-evidence capability behind `CodeIntelligenceProvider` or report an explicit evidence gap. It must not fabricate UI realization from Java-only structure. Only Human Reviewer may decide that the Capability itself should be narrowed or renamed.

### Calibration strategy

- Petclinic remains the regression dataset for detecting improvements and regressions.
- A second exact-revision repository, unseen while selection rules are designed, is required as the blind holdout.
- The user performs human review; automated generation and evaluator truth remain input-isolated. Reports disclose reviewer access to technical comparisons.
- Rules may be debugged with Petclinic, but a `GO` decision requires pre-registered thresholds and holdout evidence; improving Petclinic exact-match numbers alone is insufficient.
- An independent role proposes the holdout repository and exact revision; the
  user must approve them before the holdout is sealed. The holdout remains
  inaccessible while selection rules and thresholds are completed.
- reviewed experiment semantics, scenarios, metrics, thresholds, skill digest, schema
  digest, comparator digest, and Graphify query bounds are frozen before blind
  holdout generation.
- If a Petclinic regression causes any frozen scenario, rule, threshold, skill,
  schema, comparator, or query bound to change, a new protocol revision and new
  digest set are required. Petclinic regression restarts while the holdout
  remains sealed.
- The current Petclinic semantics, artifacts, and `REVISE` decision remain
  immutable. Scenario-grounded execution uses a new semantics revision and a
  new run identifier.

### Success criteria for the implementation

- Java rejects missing/invalid role, granularity, normalized identity, and revision binding.
- PK-S1 emits separate primary and supporting components or returns `UNRESOLVED`.
- Generation tests prove evaluator gold is not an allowed input.
- Python evaluation reports path, type, exact-symbol, realization-chain, and precision metrics independently.
- Existing `REVISE`, proposal-only, human-authority, and no-publication boundaries remain intact.
- The current Petclinic artifacts are not silently rewritten; a new run uses a new immutable run identifier and manifest.
- Frozen Forward scenarios are Human Reviewer-owned and contain no implementation
  identifiers.
- Every proposed component is traceable through a scenario realization chain.
- Missing UI/template evidence produces an explicit evidence gap or
  `UNRESOLVED`, not modified Product Semantics.
- Reverse-generated scenarios remain `UNREVIEWED` and `PROPOSAL_ONLY`.
- Human decisions bind exact proposal versions; rejected and unreviewed scenarios
  cannot enter frozen Forward inputs.

### Planned project placement and verification

- Java component contract: `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/`
- Java contract tests: `src/test/java/com/featuredeliveryintelligence/fdi/product/realization/`
- Provider integration remains under `structural/api/` and `structural/graphify/`.
- The historical PK-S1 directory and `skills/pkb001/pk-s1-product-realization-v0.2/` remain immutable for their existing experiments and contracts.
- Scenario-grounded Forward work uses a separately versioned PK-S1 skill and proposal contract. Its readiness gate MUST explicitly select and verify that version and its schema digest, bind the reviewed frozen semantics and exact-revision evidence, and reject incompatible versions. This does not authorize experiment execution or bypass its remaining gates.
- Python comparison logic remains in `tooling/validation/`; immutable outputs remain under `validation/pkb001/`.

Implementation uses Java records/enums with constructor validation and immutable collections. Python uses deterministic transformations. Checked-in JSON Schema validation requires a Draft 2020-12-compatible `jsonschema` runtime and fails closed when that validator is unavailable.

Required verification commands are:

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw test -q
python3 -m pytest -q
python3 validation/pkb001/task7-evaluation/public_validate.py .
git diff --check
```

Always preserve exact-revision binding, input isolation, deterministic output, and proposal-only authority. Ask before changing the installed Graphify runtime or selecting the holdout repository. Never expose evaluator gold to generation, overwrite an existing immutable run, or permit automatic semantic publication.

## Prototype boundary

In scope: Product Semantics input, exact-revision Graphify evidence, Delivery History reconstruction, forward and reverse experiments, human/evaluator comparison, and a GO / REVISE / STOP decision.

Out of scope: full T1–T4, DEV-204, F001, full Product Knowledge governance, automatic semantic publication, a maintenance engine, a knowledge graph database, and a new governance framework.

## Current bounded decision

PKB-001 input binding, isolation contracts, and evidence integrity passed. The current Petclinic run remains `REVISE`, not `GO`, because numeric acceptance thresholds were not frozen before generation and judgment. Its metrics are descriptive only. Non-human evaluator review cannot finalize Product meaning; human review remains pending; formal semantic publication is outside this prototype.

Task 6 provides deterministic label/order blinding only. `ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT`; no content-level arm-anonymity claim is made.
