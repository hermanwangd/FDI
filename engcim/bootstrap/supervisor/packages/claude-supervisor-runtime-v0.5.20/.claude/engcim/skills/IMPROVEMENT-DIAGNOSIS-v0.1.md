# Improvement Diagnosis Capability v0.1

**Owner:** Claude Supervisor


# Canonical Component Boundary

Before diagnosing a material finding, read:

```text
../contracts/ENGCIM-SWARM-V1-COMPONENT-CONTRACT.md
../contracts/SUPERVISOR-DIAGNOSIS-CONTRACT-v0.1.yaml
```

Use `owningComponent` for causal ownership and keep it distinct from `improvementDirection`.

Diagnosis is hypothesis-driven: begin with plausible hypotheses and discriminating evidence; do not perform an exhaustive 7+1 audit by default.

## Method
```text
Finding → expected source/behavior → propagation path → actual state at each boundary
→ first proven loss/degradation → candidate directions → reject contradicted candidates
→ rank remaining candidates → Diagnosis + Improvement Direction
```
`HIGH` confidence requires direct evidence on the relevant path; plausibility alone is insufficient.

For each plausible candidate:
```yaml
direction:
evidenceFor: []
evidenceAgainst: []
missingEvidence: []
impact: HIGH | MEDIUM | LOW
confidence: HIGH | MEDIUM | LOW
reusability: HIGH | MEDIUM | LOW
changeScope: SMALL | MEDIUM | LARGE
```
If leaders cannot be distinguished: `INCONCLUSIVE → collect discriminating evidence`.

## Diagnostic Paths

### USER_INPUT
**Evidence path:** `original request → normalized input → downstream requirement`

**Failure modes:** `INPUT_MISSING, INPUT_AMBIGUOUS, INPUT_MISINTERPRETED, INPUT_LOST_DURING_NORMALIZATION, UNSUPPORTED_ASSUMPTION`

**HIGH-confidence proof:** Material information is absent/ambiguous at source, or demonstrably changed/dropped during normalization.

### PM_INTENTION
**Evidence path:** `product request → IntentSpec/delta → Acceptance Criteria → downstream Scenario`

**Failure modes:** `INTENT_DELTA_MISSING, INTENT_AMBIGUOUS, AC_WEAK, INTENT_TRACEABILITY_BROKEN, PRODUCT_MEANING_INVENTED`

**HIGH-confidence proof:** Required product meaning is absent/incorrect in IntentSpec/AC while source request/context supports what should be preserved.

### PRODUCT_KNOWLEDGE
**Evidence path:** `source knowledge → ingestion/extraction → canonical PK → PK revision → resolution`

**Failure modes:** `PK_SOURCE_MISSING, PK_EXTRACTION_GAP, PK_CONTENT_GAP, PK_STALE, PK_INCONSISTENT, PK_REALIZATION_GAP`

**HIGH-confidence proof:** Required product truth is absent/wrong in canonical PK itself.

### PRODUCT_CONTEXT
**Evidence path:** `canonical PK → resolver/selection → resolved context revision/digest → consumer-visible context`

**Failure modes:** `CONTEXT_SELECTION_GAP, CONTEXT_OMISSION, CONTEXT_STALE, CONTEXT_WRONG_REVISION, CONTEXT_NOT_VISIBLE`

**HIGH-confidence proof:** Canonical PK contains required truth but resolved/visible context loses or misselects it.

### ARCHITECTURE
**Evidence path:** `Product Context + ChangeSurface + repo realization → system analysis → As-Is → To-Be`

**Failure modes:** `ARCHITECTURE_MISSING, ARCHITECTURE_INCORRECT, AFFECTED_SURFACE_INCOMPLETE, RELATIONSHIP_MISSING, ASIS_TOBE_DELTA_MISSING`

**HIGH-confidence proof:** Required source/context is available but architecture/system analysis is materially incomplete or incorrect.

### DESIGN
**Evidence path:** `IntentSpec + ChangeSurface + Architecture + Product Context → Technical Design → implementation`

**Failure modes:** `DESIGN_CONTENT_MISSING, DESIGN_SHALLOW, DESIGN_INCONSISTENT, DESIGN_NOT_TRACEABLE, DESIGN_DOWNSTREAM_NOT_USABLE`

**HIGH-confidence proof:** Necessary upstream information is available to the producer but Design fails to turn it into implementation-ready decisions.

### ENGINEERING_SKILL
**Evidence path:** `required capability → Scenario skillRefs → resolved Skill/version → agent/profile → actual load/use → output`

**Failure modes:** `SKILL_MISSING, SKILL_NOT_SELECTED, SKILL_NOT_MATERIALIZED, SKILL_WRONG_VERSION, SKILL_NOT_LOADED, SKILL_CAPABILITY_WEAK, SKILL_NOT_FOLLOWED`

**HIGH-confidence proof:** Correct context and Skill are selected/materialized/loaded and used, yet capability-specific output repeatedly fails.

### ENGINEERING_CONTROL
**Evidence path:** `required governance rule → Control binding → invocation → evaluation inputs → predicate → decision → enforcement/routing`

**Failure modes:** `CONTROL_MISSING, CONTROL_NOT_BOUND, CONTROL_NOT_INVOKED, CONTROL_WRONG_INPUT, CONTROL_PREDICATE_DEFECT, CONTROL_DECISION_WRONG, CONTROL_ENFORCEMENT_MISSING, CONTROL_ROUTING_WRONG`

**HIGH-confidence proof:** Exact Control path is reconstructed and the first incorrect governance/enforcement step is observed.

### SCENARIO
**Evidence path:** `goal → inputRequirements → contextRequirements → skillRefs → controlRefs → outputRequirements → completionCriteria → actual plan`

**Failure modes:** `SCENARIO_INPUT_GAP, SCENARIO_CONTEXT_GAP, SCENARIO_SKILL_GAP, SCENARIO_CONTROL_GAP, SCENARIO_OUTPUT_GAP, SCENARIO_COMPLETION_GAP, SCENARIO_PROPAGATION_GAP`

**HIGH-confidence proof:** Required reusable capability/Control/context exists but Scenario fails to require/compose/propagate it.

### SWARM_CORE
**Evidence path:** `valid Scenario + context + Skill/Control composition → shared planning/execution → handoff/aggregation`

**Failure modes:** `CORE_PLANNING_DEFECT, CORE_CONTEXT_PROPAGATION_DEFECT, CORE_AGENT_BINDING_DEFECT, CORE_AGGREGATION_DEFECT, CORE_SHARED_BEHAVIOR_DEFECT`

**HIGH-confidence proof:** Upstream composition is valid and evidence localizes repeated shared loss/degradation inside Core.

### RUNTIME_BINDING
**Evidence path:** `ENGCIM work/contract → Runtime Binding → Multica representation → execution → returned result/evidence → ENGCIM artifact/state`

**Failure modes:** `BINDING_TRANSLATION_DEFECT, BINDING_CONTEXT_DROP, BINDING_EVIDENCE_DROP, BINDING_STATE_MAPPING_DEFECT, BINDING_RESULT_MAPPING_DEFECT`

**HIGH-confidence proof:** ENGCIM supplies correct semantics and Multica can support them, but binding demonstrably loses/mistranslates them.

### MULTICA_RUNTIME
**Evidence path:** `valid binding request → Multica generic mechanics → dispatch/fan-out/fan-in/retry/re-entry/state → result`

**Failure modes:** `RUNTIME_DISPATCH_DEFECT, RUNTIME_HANDOFF_DEFECT, RUNTIME_FANIN_DEFECT, RUNTIME_RETRY_DEFECT, RUNTIME_REENTRY_DEFECT, RUNTIME_STATE_DEFECT, RUNTIME_CAPABILITY_UNAVAILABLE`

**HIGH-confidence proof:** Binding request is valid and failure is observed in generic Multica runtime mechanics.

### FIXTURE_TEST_ASSET
**Evidence path:** `verification requirement → fixture/test data/asset → digest/revision → execution → expected/actual`

**Failure modes:** `FIXTURE_MISSING, FIXTURE_STALE, FIXTURE_INVALID, EXPECTED_RESULT_WRONG, TEST_ASSET_INCOMPLETE, TEST_DATA_INTEGRITY_DEFECT`

**HIGH-confidence proof:** Exact asset identity is known and evidence shows the asset itself is defective/inadequate.

### ENVIRONMENT
**Evidence path:** `required tool/repo/auth/workspace/service → availability/config/version → operation`

**Failure modes:** `REPO_UNAVAILABLE, TOOL_MISSING, TOOL_VERSION_MISMATCH, AUTH_BLOCKED, WORKSPACE_ACCESS_BLOCKED, CONFIGURATION_DEFECT, EXTERNAL_DEPENDENCY_UNAVAILABLE`

**HIGH-confidence proof:** Direct operational evidence proves the environment prerequisite is defective/unavailable.

### PROCESS
**Evidence path:** `human/engineering operating procedure → handoff/review/release execution`

**Failure modes:** `SOP_MISSING, SOP_AMBIGUOUS, HANDOFF_PROCESS_GAP, REVIEW_PROCESS_GAP, RELEASE_PROCESS_GAP`

**HIGH-confidence proof:** Evidence localizes the gap to human/engineering procedure rather than missing executable capability.

### HUMAN_DECISION
**Evidence path:** `decision requiring authority → authorized decision`

**Failure modes:** `PRODUCT_MEANING_DECISION, AC_DECISION, RISK_ACCEPTANCE, PROTECTED_SCOPE_AUTHORIZATION, RELEASE_APPROVAL`

**HIGH-confidence proof:** Evidence shows an authority decision is required rather than an engineering correction.

## Cross-Layer Discrimination
```text
truth absent/wrong in canonical PK        → PRODUCT_KNOWLEDGE
truth exists in PK but not selected       → PRODUCT_CONTEXT
system structure not established          → ARCHITECTURE
architecture available, decisions weak    → DESIGN
capability exists but Scenario omits it   → SCENARIO
correct Skill loaded but capability weak  → ENGINEERING_SKILL
reasoning ability weak                    → ENGINEERING_SKILL
governance/enforcement wrong              → ENGINEERING_CONTROL
one Scenario miscomposes requirement      → SCENARIO
valid composition lost by shared behavior → SWARM_CORE
shared ENGCIM behavior wrong              → SWARM_CORE
ENGCIM↔Multica translation wrong          → RUNTIME_BINDING
generic runtime mechanics wrong           → MULTICA_RUNTIME
```

## Output
```yaml
problem:
observedFindingRefs: []
diagnosis:
  explanation:
  firstProvenLossOrDegradation:
  confidence:
  evidenceRefs: []
  unresolvedQuestions: []
candidates:
  - direction:
    evidenceFor: []
    evidenceAgainst: []
    missingEvidence: []
    impact:
    confidence:
    reusability:
    changeScope:
    rationale:
recommendedDirection:
  selected:
  why:
  alternatives: []
  discriminatingEvidenceNeeded: []
```
Only then create an Improvement Mission.