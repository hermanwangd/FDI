# Swarm v1.1 Q1 Qualification Plan — Resilience4j CompletionStage<Void>

**Status:** PROPOSED / qualification-ready after RC10 integration cleanup  
**Case:** Q1 — Resilience4j Issue #480, `CompletionStage<Void>` async resilience behavior  
**Scope:** One frozen real-world GitHub replay used to qualify V1.1-D1 → V1.1-D2 → V1.1-D3 without changing the case after results are known  
**Non-goals:** No new ENGCIM component, source taxonomy, knowledge store, telemetry framework, production deployment, external-repository mutation, or alternate completion gate

---

## 1. Qualification Objective

Q1 tests the engineering value of governed knowledge, not retrieval success.

The primary causal hypothesis is:

```text
governed Product Knowledge
        ↓
deeper Product / architecture understanding
        ↓
higher-quality System Analysis
        ↓
higher-quality System Design
        ↓
better engineering outcome
        ↓
less avoidable search / rework / Human rescue, when evidence supports it
```

PR-first Engineering History is a separate incremental input:

```text
Product Knowledge
        +
applicable historical/reusable knowledge
        ↓
additional SA/SD uplift, if any
```

The qualification therefore reports three distinct effects:

```text
PK_EFFECT
= Arm B versus Arm A

HISTORICAL_REUSE_EFFECT
= Arm C versus Arm B

FULL_KNOWLEDGE_EFFECT
= Arm C versus Arm A
```

Acceleration is evaluated separately from quality. Retrieval, readback, an `ADOPTED` marker, matching the eventual patch, or producing more documentation does not establish effectiveness.

---

## 2. Frozen Real-World Case

### 2.1 Target

Repository:

```text
resilience4j/resilience4j
```

Target intent:

```text
Issue #480 — Problem with circuit breaker in asynch mode
```

Case cutoff:

```text
T = 2019-06-07T07:01:16Z
```

Pinned target source state already evidenced in the RC10 validation set:

```text
source commit:
30bb4a75d3c9ca09ea22d15bfd88e59b153f4d18

source path:
resilience4j-circuitbreaker/src/main/java/io/github/resilience4j/circuitbreaker/CircuitBreaker.java

source SHA-256:
e8a0fa6b1c297614f5fc5d4f16da195011057f4995857948588edf9b5f05e2cf
```

Known pre-cutoff issue-comment snapshots already referenced by the existing sealed replay:

```text
497675930
497686509
497699789
497757016
499780036
```

The issue root is known to have been edited after the cutoff and is not worker-visible unless an exact pre-cutoff snapshot/revision can be proven.

### 2.2 Evaluator-only oracle

Target solution PR:

```text
PR #484
Issue #480: Fixed a bug that CompletionStage<Void> wasn't handled correctly
```

PR #484 and all answer-bearing post-cutoff artifacts are evaluator-only until all Arm A/B/C submissions and blind SA/SD scores are frozen.

The eventual PR is a reference oracle, not the only acceptable implementation. A materially different solution may be correct if it satisfies the frozen Product behavior/invariants and hidden verification.

---

## 3. Source and Temporal-Freeze Contract

Q1 uses the canonical Source model: documents, issues, code, tests, design material, repository state, PRs, reviews, CI evidence, runtime evidence and Mission artifacts may all be Sources when attributable and authorized.

### 3.1 Worker-visible target Sources

Each arm receives the same frozen target-work Sources at T:

```text
Target work at T
├─ intent / requirement evidence
├─ allowed issue/comment snapshots
├─ current repository state at the pinned commit
├─ current code / API / schema / config relevant to the case
├─ current tests / verification material available at T
├─ permitted product / engineering documentation available at T
└─ other explicitly frozen Mission inputs
```

Every material mutable provider source must be represented by one of:

- exact content snapshot + digest;
- immutable revision/commit + digest;
- provider as-of receipt that establishes the exact content at or before T.

A live mutable provider read after T cannot substitute for the replay input.

### 3.2 Evaluator-only Sources

Withhold until Phase 2 evaluation:

```text
Evaluator-only
├─ PR #484 body/diff/commits/reviews/checks
├─ target solution commit(s)
├─ post-cutoff target code
├─ post-cutoff answer-bearing issue edits/comments
└─ any derived artifact that discloses the target solution
```

### 3.3 Case-freeze manifest

Before execution, create one immutable Q1 freeze manifest containing at least:

- repository/case identity;
- cutoff T;
- target intent snapshot identity;
- every worker-visible Source ref/revision/digest;
- every historical-source ref/revision/digest;
- Product Knowledge pack revision/digest;
- reusable historical knowledge revision/digest;
- evaluator-only withheld refs;
- worker model/config/instruction revision;
- S05 contract/profile revision;
- tool permissions and time budget;
- scoring rubric revision;
- hidden-verification revision;
- run-invalidity rules.

Do not alter the case, oracle, rubric, threshold or worker-visible source set after observing outcomes.

---

## 4. Experimental Design — Three Arms

Q1 uses three isolated arms so Product Knowledge and historical reuse are measured independently.

### Arm A — Baseline

```text
Current Mission Sources
+ Current Engineering Evidence
+ normal S05 Skills / instructions
- governed Product Context
- historical/reusable knowledge pack
```

### Arm B — Product Knowledge

```text
Everything in Arm A
+ governed Product Context / Product Knowledge
- V1.1-D1 historical/reusable knowledge pack
```

### Arm C — Product Knowledge + Historical Reuse

```text
Everything in Arm B
+ V1.1-D1 PR-first Engineering History / applicable reusable knowledge
```

### 4.1 Comparability controls

All arms use the same:

- target intent and acceptance criteria;
- cutoff and frozen current evidence;
- starting repository revision;
- model family/configuration and thinking budget;
- S05 roles/Skills/instructions except for the intended knowledge inputs;
- tools and permissions;
- time/resource budget;
- required SA/SD deliverables;
- implementation endpoint;
- review/verification requirements.

Runs are isolated. No arm may read another arm's work or conversation state. Where feasible, execution order is randomized and evaluation order is anonymized.

Model pretraining cannot be perfectly removed; using the same model/configuration in all arms controls it as a shared background factor rather than attributing it to Product Knowledge.

---

## 5. Product Knowledge Pack for Arm B/C

The PK pack must be built and frozen before Arm A/B/C execution from pre-cutoff, attributable Product Sources. It uses existing Product Knowledge / Product Context governance; Q1 does not create a new store or authority.

The PK pack may contain target-independent Product understanding such as:

- Product capabilities and semantics;
- stable behavior/invariants;
- Product realization and responsibility mapping;
- relationships among resilience capabilities;
- async execution abstractions and applicable contracts;
- stable repository/module navigation;
- known component responsibilities and interfaces.

It must not contain:

- the target solution PR or patch;
- a statement of the hidden #480 answer;
- target-specific instructions such as "change Retry too";
- post-cutoff answer-bearing knowledge;
- evaluator-only conclusions.

The Product Knowledge pack therefore gives Arm B/C a better Product model, not the answer.

### 5.1 PK setup cost

Record PK construction/governance effort separately from Mission execution time. Do not hide one-time knowledge-building cost inside an acceleration claim. Mission-level acceleration and lifecycle/setup cost are reported separately.

---

## 6. V1.1-D1 — PR-first Engineering History

V1.1-D1 qualifies historical-source reconstruction independently from SA/SD effect.

### 6.1 Primary historical delivery H1

Use a pre-cutoff completed delivery with explicit provider-native intent linkage:

```text
PR #394
→ Issue #280
→ CompletionStage exception/completion behavior
→ CircuitBreaker + Retry historical delivery evidence
```

Preserve exact PR/commit/review/test/work-item snapshots and PA-05 correlation method/strength.

### 6.2 Secondary historical delivery H2

PR #173 ("Fixing blinking tests") may provide bounded async callback/test-synchronization precedent.

If no explicit provider-native work-item link exists, retain that limitation. Do not invent an Issue association. PR-native rationale may be usable as attributed historical evidence without pretending it is an explicit work-item correlation.

### 6.3 Historical knowledge output

Historical material may produce bounded reusable precedent/hypotheses such as:

- cross-capability investigation cues;
- async callback verification discipline;
- known historical change surfaces;
- historical rationale/constraints.

It must not establish current truth.

The allowed reasoning chain is:

```text
Historical knowledge
        ↓
current hypothesis / investigation priority
        ↓
current frozen evidence
        ↓
CONFIRMED | EXCLUDED | UNRESOLVED
```

V1.1-D1 PASS requires reproducible change-first reconstruction, temporal isolation, provenance/correlation fidelity, honest incompleteness and safe handoff. It does not require Product/Workspace Knowledge publication and does not depend on whether V1.1-D2/D3 later show uplift.

---

## 7. Required SA Deliverable Contract

All three arms must deliver System Analysis to the same contract.

SA must include:

1. intent/problem interpretation;
2. Product behavior and capability analysis;
3. system boundary/context;
4. current Product/module realization;
5. affected responsibility/component analysis;
6. As-Is critical dynamic behavior;
7. state/behavior model where state is material;
8. explicit behavior/invariant model;
9. root-cause causal chain;
10. change-surface analysis with `CONFIRMED | EXCLUDED | UNRESOLVED`;
11. dependency/interaction analysis;
12. historical/reusable knowledge applicability, when available;
13. risk/compatibility analysis;
14. material uncertainty and evidence gaps.

A short correct analysis may outscore a longer shallow one.

---

## 8. Required SD Deliverable Contract

All three arms must deliver System Design to the same contract.

SD must include:

1. design intent/principles;
2. To-Be behavior;
3. architecture/realization delta;
4. To-Be component/responsibility design;
5. To-Be critical dynamic behavior;
6. explicit invariants;
7. interface/interaction implications;
8. state/failure behavior where applicable;
9. compatibility strategy;
10. verification architecture;
11. implementation plan and change traceability;
12. explicit no-change/excluded surfaces where material.

The design must be implementation-ready: a Coder should not have to rediscover the architecture or infer critical behavior omitted by SD.

---

## 9. C4 and Supporting-View Selection Contract

Q1 evaluates architecture-model fitness, not diagram count.

C4 abstractions must not be falsified merely to satisfy a checklist. In particular, a Maven/JAR/module is not automatically a C4 Container. Use a C4 Container only when the modeled element is a deployable/runnable application or data store consistent with the C4 abstraction being claimed.

### 9.1 Q1 expected view set

For Issue #480, the expected architecture evidence is:

| View | Q1 expectation |
| --- | --- |
| C4 System Context | Applicable; concise system boundary/context |
| C4 Container | Conditional; do not relabel library modules as Containers without a valid runtime/container boundary |
| C4 Component | Conditional on a valid Container scope; otherwise use a clearly named responsibility/component view |
| C4 Code | Optional |
| C4 System Landscape | Normally N/A unless evidence shows a portfolio/system-of-systems concern |
| C4 Dynamic | Mandatory for As-Is and To-Be critical runtime behavior |
| C4 Deployment | Normally N/A unless deployment topology materially affects the defect |
| Product/Module Realization View | Mandatory; non-C4 if necessary |
| State Diagram | Mandatory for the CircuitBreaker state mechanism; non-C4 supporting model |

Every omitted/conditional view needs a one-line applicability rationale.

### 9.2 Architecture-quality dimensions

Evaluate:

- view fitness;
- abstraction integrity;
- model correctness;
- model completeness;
- responsibility/boundary correctness;
- evidence grounding;
- cross-view consistency;
- decision usefulness.

More diagrams do not improve the score by themselves.

A material cross-view contradiction is a quality defect. Example: SA says Retry is in scope, Dynamic omits it without explanation, SD excludes it, while implementation changes it.

---

## 10. Reasoning Depth — Coverage Matrix

Depth is the number and quality of correctly connected, evidence-backed reasoning layers, not prose length.

For each arm, record:

| Reasoning layer | 0 | 1 | 2 |
| --- | --- | --- | --- |
| Intent | absent/wrong | partial/weakly supported | correct and evidence-backed |
| Product behavior | absent/wrong | partial | correct and connected |
| Capability | absent/wrong | partial | correct and connected |
| System boundary | absent/wrong | partial | correct and connected |
| Product realization | absent/wrong | partial | correct and connected |
| Component responsibility | absent/wrong | partial | correct and connected |
| Dynamic interaction | absent/wrong | partial | correct and connected |
| State/behavior | absent/wrong | partial | correct and connected |
| Failure mechanism | absent/wrong | partial | correct causal chain |
| Change surface | absent/wrong | partial | complete/materially correct |
| To-Be invariants | absent/wrong | partial | explicit and correct |
| Verification implications | absent/wrong | partial | derived and traceable |

This matrix supports the depth verdict; it is not added again to the 100-point total.

---

## 11. Blind SA/SD Evaluation — Before Oracle Reveal

SA/SD quality is scored before PR #484 or hidden solution evidence is revealed.

### Phase 1A — Independent architecture/engineering scoring

- Freeze Arm A/B/C submissions.
- Present them in randomized/anonymized order where practical.
- Evaluator has the frozen target Sources but not PR #484.
- Score SA/SD quality, depth, evidence grounding and downstream usability.
- Freeze the scores and findings.

### Phase 1B — Knowledge causal attribution

After quality scores are frozen, resolve arm identity and exact knowledge inputs.

For every claimed material knowledge use, record:

```text
knowledge revision
→ reasoning step
→ SA/SD decision
→ resulting artifact/change implication
→ supporting current evidence
```

Current evidence remains the authority for current applicability.

---

## 12. SA Quality Rubric — 40 Points

| SA dimension | Points |
| --- | ---: |
| Intent / Product behavior | 5 |
| Capability understanding | 5 |
| Architecture / realization | 5 |
| Architecture view selection & correctness | 4 |
| Dynamic / state understanding | 5 |
| Root-cause causal chain | 5 |
| Change-surface completeness | 5 |
| Dependency / interaction reasoning | 3 |
| Risk / compatibility / uncertainty | 3 |
| **SA total** | **40** |

A critical unsupported current-truth claim, material solution leakage, fabricated source linkage or incorrect system boundary prevents an SA effectiveness PASS regardless of raw score.

---

## 13. SD Quality Rubric — 40 Points

| SD dimension | Points |
| --- | ---: |
| Design intent / principles | 4 |
| To-Be behavior | 5 |
| Architecture / component design | 6 |
| To-Be Dynamic | 4 |
| Explicit invariants | 5 |
| Interface / interaction implications | 4 |
| Failure / compatibility behavior | 4 |
| Verification architecture | 4 |
| Implementation readiness / traceability | 4 |
| **SD total** | **40** |

A design that reaches the eventual patch by unsupported guessing does not receive full design-quality credit.

---

## 14. Architecture-Depth Gate for V1.1-D2

Proposed frozen material-uplift thresholds for Q1:

### PK_EFFECT — Arm B vs Arm A

Require all of:

- Arm B SA score >= Arm A SA score;
- Arm B SD score >= Arm A SD score;
- Arm B SA+SD >= Arm A SA+SD + 8 points out of 80;
- at least three material reasoning layers in the Coverage Matrix improve by one grade without a critical-layer regression;
- no critical architecture/evidence regression.

### HISTORICAL_REUSE_EFFECT — Arm C vs Arm B

A historical-reuse uplift is material when it causes an attributable new or improved engineering decision, such as:

- a current change surface that B missed;
- a dependency/constraint/risk B missed;
- a materially stronger verification design;
- removal of an unsupported assumption;
- a meaningful SA/SD score increase.

For Q1, use +4/80 SA+SD points as the proposed quantitative corroboration threshold, but causal traceability is mandatory; a numeric increase without a history→reasoning→decision chain is not validated reuse.

### FULL_KNOWLEDGE_EFFECT — Arm C vs Arm A

Require:

- no critical regression;
- Arm C SA+SD >= Arm A SA+SD + 10/80;
- material uplift in architecture/change-surface or behavior/verification depth;
- causal evidence showing which Product Knowledge and/or historical knowledge contributed.

These thresholds are frozen before execution. Do not lower them after seeing results.

---

## 15. Phase 2 Outcome Oracle — After SA/SD Scores Freeze

Only after blind SA/SD scoring is frozen may the evaluator reveal:

- PR #484;
- final solution diff;
- final changed files;
- solution reviews/checks;
- hidden verification.

### 15.1 Hidden verification minimum

The hidden oracle should cover at least:

1. CircuitBreaker `CompletionStage<Void>` successful completion;
2. success accounting exactly once;
3. HALF_OPEN → CLOSED after sufficient successful probes;
4. normal non-null success unchanged;
5. Exception path behavior;
6. Error behavior/policy preservation;
7. Retry `CompletionStage<Void>` successful completion does not retry;
8. Retry non-null behavior remains correct.

Worker-authored tests are scored separately from hidden tests.

### 15.2 Outcome rubric — 20 points

| Outcome dimension | Points |
| --- | ---: |
| Patch/behavior correctness | 8 |
| Hidden verification | 5 |
| Minimality / regression control | 3 |
| Reviewer usability / avoidable rework | 4 |
| **Outcome total** | **20** |

Exact textual or file-for-file agreement with PR #484 is not required.

---

## 16. V1.1-D3 — Engineering Effectiveness

Report quality and acceleration separately.

### 16.1 Quality conclusion

Use:

```text
SA 40
+ SD 40
+ Outcome 20
= 100
```

V1.1-D3 engineering effectiveness requires the relevant V1.1-D2 depth/quality gate plus no critical downstream regression.

Allowed dispositions:

- `EFFECT_VALIDATED`
- `NO_MEASURABLE_UPLIFT`
- `REGRESSION`
- `INCONCLUSIVE`

An unfavorable result is retained. Do not swap the case or change the oracle to manufacture uplift.

### 16.2 Acceleration

Record at least:

```text
T0  Mission-ready / frozen input available
T1  first complete SA
T2  first complete SD
T3  implementation ready
T4  first independent review verdict
T5  quality-qualified endpoint
```

Also record:

- Human interventions;
- clarification requests;
- external-agent rescue;
- repeated source lookup;
- failed verification cycles;
- rework cycles.

Report separately:

1. **Reasoning elapsed:** context-ready → quality-qualified SA/SD.
2. **Mission end-to-end elapsed:** Mission-ready → quality-qualified engineering endpoint.
3. **Knowledge setup cost:** PK/history construction/governance outside the Mission.

Do not infer acceleration from fewer retrieval calls, lower token count, or a single fast run.

Allowed acceleration dispositions:

- `ACCELERATION_DEMONSTRATED`
- `NO_MEASURABLE_UPLIFT`
- `SLOWER_WITH_QUALITY_UPLIFT`
- `INCONCLUSIVE`

---

## 17. Required Causal Verdicts

Final Q1 report must independently disposition:

| Verdict | Comparison | Primary question |
| --- | --- | --- |
| `PK_EFFECT` | B vs A | Did governed Product Knowledge materially improve SA/SD quality/depth? |
| `HISTORICAL_REUSE_EFFECT` | C vs B | Did PR-first reusable history add engineering value beyond PK? |
| `FULL_KNOWLEDGE_EFFECT` | C vs A | Did the complete knowledge-enabled Swarm materially outperform baseline? |
| `ENGINEERING_OUTCOME_EFFECT` | A/B/C + hidden oracle | Did deeper SA/SD translate to a correct/no-regression engineering outcome? |
| `ACCELERATION_EFFECT` | comparable T0–T5 | Was quality-qualified delivery faster or less rescue/rework intensive? |

A single aggregate PASS is insufficient.

---

## 18. Run Validity and Rerun Rules

A run may be declared invalid only for a pre-frozen execution failure such as:

- wrong frozen input set;
- oracle leakage;
- infrastructure/tool failure preventing comparable completion;
- model/config mismatch;
- corruption of required artifacts.

Invalid runs are preserved with the reason. A rerun, if necessary, uses the same frozen inputs/rubric. Poor engineering quality is not a valid reason to rerun.

---

## 19. Required Q1 Evidence Package

Produce one evidence package:

```text
validation/rc10/swarm-v11-q1/
├─ case-freeze.json
├─ source-snapshot-manifest.json
├─ product-knowledge/
│  ├─ source-manifest.json
│  ├─ governed-pk-revision.json
│  └─ product-context-receipt.json
├─ historical/
│  ├─ H1-pr394/
│  ├─ H2-pr173/
│  ├─ d1-reconstruction.json
│  └─ reusable-knowledge-revision.json
├─ arm-a-baseline/
│  ├─ sa/
│  ├─ sd/
│  ├─ implementation/
│  └─ timing.json
├─ arm-b-pk/
│  ├─ sa/
│  ├─ sd/
│  ├─ implementation/
│  └─ timing.json
├─ arm-c-pk-history/
│  ├─ sa/
│  ├─ sd/
│  ├─ implementation/
│  └─ timing.json
├─ blind-evaluation/
│  ├─ sa-sd-scores.json
│  ├─ reasoning-coverage-matrix.json
│  └─ review-findings.md
├─ oracle/
│  ├─ withheld-manifest.json
│  ├─ solution-comparison.json
│  └─ hidden-verification.json
└─ final-q1-report.md
```

Exact formats may reuse existing RC10 evidence schemas where available. Do not create a second telemetry or runtime framework merely to match this directory illustration.

---

## 20. Execution Sequence

```text
Q0  preflight current RC10/integration identities
 ↓
Q1  freeze target Sources / cutoff / oracle / rubric
 ↓
Q2  build + govern target-independent Product Knowledge
 ↓
Q3  V1.1-D1 PR-first historical reconstruction + reusable knowledge freeze
 ↓
Q4  execute isolated Arm A / B / C to the same endpoint
 ↓
Q5  blind SA/SD scoring and Reasoning Coverage Matrix
 ↓
Q6  freeze scores; perform knowledge causal attribution
 ↓
Q7  reveal PR #484 + run hidden verification
 ↓
Q8  score engineering outcome
 ↓
Q9  compare timing/rework/rescue
 ↓
Q10 disposition PK_EFFECT / HISTORICAL_REUSE_EFFECT /
    FULL_KNOWLEDGE_EFFECT / ENGINEERING_OUTCOME_EFFECT / ACCELERATION_EFFECT
```

The entire Q1 sequence is one qualification slice. V1.1-D1, D2 and D3 are not separate projects.

---

## 21. Completion Boundary

Q1 is complete when:

- the case and all mutable replay Sources are frozen reproducibly;
- V1.1-D1 is dispositioned from real historical evidence;
- all A/B/C arms reach the same predeclared endpoint or are honestly invalidated;
- blind SA/SD scoring is frozen before oracle reveal;
- PK and historical knowledge influence is traceable to material reasoning/decisions;
- hidden outcome verification is complete;
- quality and acceleration conclusions are separate;
- unfavorable/no-uplift results are preserved;
- no target-solution leakage, fabricated linkage, Product-truth elevation from history, or post-hoc rubric/case changes occurred.

Q1 completion does not establish company/Azure qualification, production deployment, Human DONE, or universal Swarm effectiveness across all engineering domains.
