# SF-BL-002 Route-Aware Scenario Effectiveness Correction

## Status and authority

This design is a Feature Delivery Plane proposal for the next bounded
`SF-BL-002` correction. It is governed by `AUTH-002`, `PK-004`, `EVID-001`, and
`TECH-001`. It does not change Product meaning, accepted Behavior Scenarios, or
the Framework Spec.

The proposed execution identity is
`SF-BL-002-ROUTE-EFFECTIVENESS-005`. Implementation is not authorized until
the Feature Delivery Plane selects it in `IMPLEMENTATION-PLAN.md` and
`STATUS.json` from an exact base commit.

## Problem

The accepted `-002` run assigns one PRIMARY production reference to every
scenario using token-overlap count. Any positive overlap qualifies, so weak
entity-only or condition-only matches become proposals:

- create owner selected `Owner#getPet` from the token `owner`;
- create pet selected the same `Owner#getPet` reference from `pet`;
- reject visit selected `Pet#setBirthDate` from `date`.

At the same time, the test evidence already observes HTTP behavior such as
MockMvc and RestTemplate routes, but classifies the framework calls as external
unresolved references. It does not continue from the observed HTTP method and
route to the exact Spring controller method at the same source revision.

The result is honest but ineffective: scenario trace coverage is `7/10`, exact
component precision is `0.2857`, recall is `0.0833`, and F1 is `0.1290`.

## Design decision

Replace token-count-only PRIMARY selection with evidence-strength gating and an
exact route-to-handler bridge. Token similarity remains a retrieval signal; it
cannot establish a proposal by itself.

```text
Exact-revision test source
  -> HTTP behavior observations
     (method + normalized route + test provenance)

Exact-revision production source
  -> Spring route-handler index
     (method + normalized route -> controller method)

HTTP observation + handler index
  -> exact route-handler binding

Accepted scenario search intent
  + exact route-handler binding
  + same-test direct production references
  + bounded Graphify traces
  -> evidence-strength gate
  -> MAPPING_PROPOSAL | UNRESOLVED
```

This approach is preferred over merely increasing the token threshold, which
could raise precision by abstaining but would not recover the missing controller
entry points. It is also preferred over unconstrained reverse Graphify search,
which could introduce inferred candidates without a test-to-handler proof.

## Exact inputs

Generation uses `https://github.com/spring-projects/spring-petclinic.git` at
exact revision `818c4136ea971c21674525f9053de0d9c7ad8cfe`.

The source checkout is a runtime input and is not copied into this repository.
Before parsing it, the implementation must:

1. resolve the revision to the full commit;
2. verify that the checkout is exactly at that revision;
3. verify every consumed production and test file against the existing
   `input_digests` in
   `validation/software-factory/sf-bl002/test-behavior-evidence.json`; and
4. fail closed before artifact generation if the checkout or a digest is
   unavailable or different.

The existing accepted semantics, search intents, acceptance manifest, test
behavior evidence, Graphify snapshot, and Graphify runtime evidence retain their
current exact digest bindings. Evaluator truth is not a generation input.

## Components

### 1. HTTP behavior observation

A Java extractor reads exact-revision tests and emits immutable observations:

```text
HttpBehaviorObservation {
  observationRef
  testSourcePath
  testMethod
  sourceLocation
  httpMethod
  normalizedRouteTemplate
  extractionBasis
}
```

Supported initial forms are bounded to constructs present in the frozen test
set: Spring MockMvc request builders and RestTemplate calls with statically
recoverable HTTP method and route. Query strings are separated from the route;
literal path values and `{variable}` placeholders normalize to one canonical
template. Dynamic route expressions are accepted only when their literal and
placeholder structure can be recovered without executing code.

Framework calls such as `mockMvc.perform` remain external-library diagnostics.
The HTTP method/route extracted from their arguments is a separate recoverable
application observation. Unsupported or ambiguous expressions remain explicit
gaps.

### 2. Spring route-handler index

A JavaParser-based index reads exact-revision production source and combines
class-level and method-level Spring mapping annotations. It emits:

```text
RouteHandler {
  handlerRef
  httpMethods[]
  normalizedRouteTemplate
  productionIdentity
  sourceLocation
  sourceDigest
}
```

The index supports the mapping annotations actually present at the bound
revision. A test observation binds only when HTTP method and normalized route
resolve to exactly one production handler. Zero matches are `UNRESOLVED`; more
than one match is `AMBIGUOUS`. Neither case may fall back to a guessed handler.

### 3. Behavior-aware evidence policy

Scenario retrieval terms are separated into action, entity, condition, and
alias signals. A small provider-neutral action-family table recognizes common
delivery behavior such as find/search, browse/list, create/add/insert, update/edit,
and reject/validate/error. It must not contain Petclinic component names,
evaluator mappings, or expected outputs.

A component qualifies only through one of these proof paths:

- `EXACT_ROUTE_HANDLER`: a unique HTTP observation-to-handler binding whose
  route and test behavior agree with the scenario entity and action family; or
- `DIRECT_PRODUCTION_REFERENCE`: a mechanically resolved production reference
  from a test method assigned to the scenario by at least two independent
  behavior signals, including entity plus action-family or condition evidence.

Entity agreement requires the normalized route, controller type, referenced
production type, or test identity to contain the accepted entity or an accepted
alias. Action-family agreement requires compatible test-method, handler-method,
route-literal, or assertion evidence; HTTP verb alone is not sufficient to
distinguish create, update, and reject behavior. A reject scenario additionally
requires same-test negative evidence such as a validation error, error response,
exception, rejected persistence result, or explicit guard assertion.

A single overlapping token never qualifies. Graphify traversal may add bounded
relationship traces from a qualified component, but `GRAPH_TRACE_SUPPORT` stays
diagnostic and receives no formal component credit unless the target also has
an independent direct proof.

Each scenario emits an ordered set of component proposals rather than forcing a
single component to represent the entire realization chain:

```text
ScenarioComponentProposal {
  scenarioId
  outcome
  components[] {
    role
    evidenceStrength
    productionIdentity
    evidenceRefs[]
    relationshipTrace?
  }
  gaps[]
}
```

`outcome` is `MAPPING_PROPOSAL` only when at least one component passes the
policy. Otherwise it is `UNRESOLVED`. All output remains `PROPOSAL_ONLY` and
`semantic_publication_allowed=false`.

### 4. Evaluator-only scoring and enforced decision

Generation seals all non-evaluator inputs and outputs before evaluator truth is
opened. The evaluator independently validates every formally scored component's
proof path instead of trusting a producer-supplied credit flag.

The new report must show per-scenario and aggregate:

- mapping versus unresolved counts;
- scenario trace coverage;
- exact component matched, proposed, and expected counts;
- exact component precision, recall, and F1;
- route resolved, unresolved, and ambiguous counts;
- direct-reference and Graphify-diagnostic counts; and
- every failed evidence-strength rule.

The decision is computed, not narrated. `GO` requires all of:

- scenario trace coverage `>= 6/10`;
- exact component precision `>= 0.70`;
- exact component recall `> 0.0833333333`; and
- exact component F1 `> 0.1290322581`.

Otherwise the result is `REVISE`. A computed `GO` is experiment evidence only;
it does not publish Product truth or terminally close `SF-BL-002`.

Capability alignment remains
`NOT_COMPARABLE_NO_SEALED_CROSSWALK`. This correction does not create a
crosswalk after observing evaluator truth.

## Immutable artifact boundary

All existing `validation/pkb001/` files and every current `*-001` and `*-002`
SF-BL-002 artifact remain byte-identical. The new run writes only new `-003`
artifacts and manifests under `validation/software-factory/sf-bl002/`.

Every new artifact binds the execution ID, exact source revision, all input and
output digests, generation method, Java/runtime identity, and evaluator-access
order. A pre-existing output path with different bytes is a hard failure; the
run never overwrites it.

## Failure behavior

- Missing or wrong exact-revision source: `PLAN_BLOCKED` before execution or a
  fail-closed runtime result after dispatch.
- Input digest mismatch: no new artifact is published.
- Unsupported dynamic route: preserve an explicit unresolved observation.
- Ambiguous route-to-handler match: preserve ambiguity; do not rank handlers.
- Weak token-only candidate: emit a diagnostic and keep the scenario
  `UNRESOLVED` unless another proof path qualifies.
- Missing Graphify edge: keep the proven component and omit the unsupported
  relationship; never invent an edge.
- Evaluator input accessed before generation sealing: invalidate the run.
- Any attempt to publish semantics: fail validation.

## Delivery decomposition

The future Implementation Plan should use this dependency shape:

```text
Task 1: v0.3 contracts and synthetic fixtures
        |
        +--> Task 2A: HTTP behavior observation extractor ----+
        |                                                      |
        +--> Task 2B: Spring route-handler index --------------+--> Task 3
        |                                                          policy +
        |                                                          proposals
        |                                                             |
        +--> Task 2C: evaluator threshold enforcement ----------------+
                                                                      |
                                                                      v
Task 4: exact-revision -003 run, deterministic replay, evaluation, evidence
                                                                      |
                                                                      v
Task 5: combined integration, independent review, full regression, KPI return
```

Tasks 2A, 2B, and 2C may run in parallel after Task 1 because their mutation
surfaces are disjoint. Task 3 joins 2A and 2B; Task 4 requires Task 3 and the
evaluator work from 2C. The Execution Plane owns routing, integration, review,
and remediation but cannot edit active controls.

## Verification strategy

Tests must cover:

- MockMvc and RestTemplate method/route extraction;
- query removal, path-variable normalization, unsupported dynamic paths, and
  byte-stable ordering;
- class-plus-method Spring annotation composition;
- zero, unique, and ambiguous handler resolution;
- weak one-token rejection and honest abstention;
- exact route and direct-reference qualification;
- evaluator-blind generation and post-seal evaluator access;
- enforced threshold boundary cases;
- immutable `-001`/`-002` artifacts and collision refusal;
- two independent `-003` runs producing byte-identical outputs; and
- full Java 17 regression with Maven heap bounded to 2 GiB.

Independent review must evaluate the exact integrated candidate for evaluator
leakage, route-resolution correctness, evidence-credit correctness, immutable
artifact handling, determinism, and compliance with proposal-only authority.

## Explicit exclusions

This correction does not change accepted Product Semantics, add a Product
Knowledge crosswalk, use evaluator truth during generation, modify the external
Graphify runtime, generalize route extraction beyond the frozen Java/Spring
experiment, alter historical evidence, close `SF-BL-002`, or start `SF-BL-001`.
