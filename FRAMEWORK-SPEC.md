# PKB-001 Framework Specification

**Status:** Prototype evaluated — `REVISE`

## Objective

PKB-001 validates whether product meaning, source structure, and delivery evidence can support reliable Product Realization discovery without allowing machine observations to become Product truth automatically.

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

Reverse results are proposals. They cannot establish Product semantics or publish Product truth without Product Team review.

## Ownership boundaries

- Product Team owns Product meaning and accepted Capability definitions.
- Graphify supplies structural observations, not Product semantics.
- Git, pull requests, and feature history supply delivery evidence, not Product truth.
- Human/evaluator review accepts, renames, merges, splits, rejects, or identifies missing proposals.

## Architecture

```text
Product Semantics → Capability → Product Realization → Component / Repository

CodeIntelligenceProvider → Graphify adapter → Graphify runtime
Graphify → Structural Intelligence
Git / PR / Feature History → Delivery Intelligence

Structural Intelligence + Delivery Intelligence
→ Capability Hypothesis → Product Team Review
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

A mapping proposal must contain at least one `PRIMARY` component unless its outcome is `UNRESOLVED`. A containing class or file must not replace a more precise method node when direct method evidence is available. Supporting evidence remains separate from formally proposed components.

### Normalized structural identity

The stable comparison identity contains:

```text
source_revision
source_path
symbol_type
qualified_symbol
provider_node_id
```

`source_revision`, repository-relative `source_path`, and `symbol_type` are mandatory. `qualified_symbol` is mandatory for symbol-level components. `provider_node_id` preserves Graphify provenance but is not, by itself, a provider-neutral identity. Normalization must never use evaluator gold or post-generation judgments.

### Hierarchical evaluation

Evaluation reports separate these levels rather than treating them as interchangeable:

1. source-path match;
2. containing type/class match;
3. exact method, entity, template, or configuration-symbol match;
4. expected realization-chain coverage;
5. extra proposed-component precision.

Path overlap and supporting-evidence citation do not count as exact component matches. The existing Petclinic metrics remain descriptive regression evidence. New acceptance thresholds must be registered before the next blind/holdout execution and must not be selected from the observed Petclinic result.

### Data flow and isolation

```text
Frozen Product Semantics + exact-revision Graphify evidence
→ Skill/agent proposal generation without evaluator gold
→ Java contract validation
→ immutable proposal artifact
→ Python blinded evaluation against sealed evaluator truth
→ Product Team review
```

Generation must fail closed when identity, granularity, role, source revision, or provider binding is absent or inconsistent. The Product Team decides capability meaning and boundaries; evaluator comparison measures realization quality but cannot publish Product Semantics.

### Template and UI evidence

Before adding template extraction, the installed Graphify runtime must be queried to prove that it supports the required source type and relationships. If it does not, the prototype must either introduce a separately identified structural-evidence capability behind `CodeIntelligenceProvider` or narrow the capability claim to what the bound evidence can prove. It must not fabricate UI realization from Java-only structure.

### Calibration strategy

- Petclinic remains the regression dataset for detecting improvements and regressions.
- A second exact-revision repository, unseen while selection rules are designed, is required as the blind holdout.
- Product Team semantic review, evaluator truth preparation, proposal generation, and final evaluation remain role-separated.
- Rules may be debugged with Petclinic, but a `GO` decision requires pre-registered thresholds and holdout evidence; improving Petclinic exact-match numbers alone is insufficient.

### Success criteria for the implementation

- Java rejects missing/invalid role, granularity, normalized identity, and revision binding.
- PK-S1 emits separate primary and supporting components or returns `UNRESOLVED`.
- Generation tests prove evaluator gold is not an allowed input.
- Python evaluation reports path, type, exact-symbol, realization-chain, and precision metrics independently.
- Existing `REVISE`, proposal-only, human-authority, and no-publication boundaries remain intact.
- The current Petclinic artifacts are not silently rewritten; a new run uses a new immutable run identifier and manifest.

### Planned project placement and verification

- Java component contract: `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/`
- Java contract tests: `src/test/java/com/featuredeliveryintelligence/fdi/product/realization/`
- Provider integration remains under `structural/api/` and `structural/graphify/`.
- The historical PK-S1 directory `skills/pkb001/pk-s1-product-realization/` remains immutable for completed runs; next-run instructions are in `skills/pkb001/pk-s1-product-realization-v0.2/`.
- Next-run readiness MUST explicitly select PK-S1 v0.2 before generation.
- Python comparison logic remains in `tooling/validation/`; immutable outputs remain under `validation/pkb001/`.

Implementation uses Java records/enums with constructor validation and immutable collections. Python uses deterministic standard-library transformations; no new dependency is introduced.

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

PKB-001 input binding, isolation contracts, and evidence integrity passed. The current Petclinic run remains `REVISE`, not `GO`, because numeric acceptance thresholds were not frozen before generation and judgment. Its metrics are descriptive only. Non-human evaluator review cannot finalize Product meaning; human Product Team review and any semantic publication remain pending.

Task 6 provides deterministic label/order blinding only. `ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT`; no content-level arm-anonymity claim is made.
