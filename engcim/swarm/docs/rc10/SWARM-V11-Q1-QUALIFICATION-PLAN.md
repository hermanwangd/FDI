# Swarm v1.1 Q1 Qualification Plan — Resilience4j CompletionStage<Void>

**Status:** PROPOSED / pre-execution admission evidence and final freeze required

**Case:** Q1 — Resilience4j Issue #480, `CompletionStage<Void>` async resilience behavior

**Scope:** One frozen real-world GitHub replay used to qualify V1.1-D1 → V1.1-D2 → V1.1-D3 without changing the case after results are known

**Non-goals:** No new ENGCIM component, source taxonomy, knowledge store, telemetry framework, production deployment, external-repository mutation, or alternate completion gate

This full plan is operator/evaluator material. It contains case-specific expectations and oracle information and must not be sent to a worker or Phase 1 blind scorer. Use the explicit permitted projections below. The known case and previous results are retained; they are not relabeled as a new blind run. This plan authorizes no execution, new knowledge grant or runtime/context change.

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

Every material mutable provider source must have exact snapshot/revision bytes and a digest, plus evidence establishing that permitted content at or before T. A snapshot hash proves identity, not historical availability. An immutable code commit does not establish the historical body, links, reviews or checks of a mutable work item.

Record the supported provider as-of receipt or the evidentiary basis of a controlled reconstruction separately. A reconstruction is admissible only when its exact allowed view and historical basis are verifiable before admission; otherwise exclude it and retain the unmet admission requirement. A current mutable read or a declared limitation cannot repair that gap. Workers read the frozen allowed view only, with no live post-T fallback.

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
- revision-bound governance/eligibility/selection receipts for both consumed packs;
- evaluator-only withheld refs;
- worker model/config/instruction revision;
- fresh worker/scorer context, prior-exposure quarantine and actual access-boundary receipts;
- S05 contract/profile revision;
- tool permissions and time budget;
- scoring rubric revision;
- hidden-verification revision;
- run-invalidity rules.

Seal the final manifest after Q2/Q3 pack preparation and independent readback, immediately before Q4. It binds the exact allowed worker projections, scorer packet contract, source availability evidence, eligibility decisions, first-SA/SD endpoints, concrete scoring anchors/denominators, mandatory verification commands and expected behavior, deadline and stop rules. Preserve its original bytes/hash. No dependent launch is permitted while these admission receipts are absent or contradictory.

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

Runs are isolated. No arm may read another arm's work or conversation state. Freeze the supported dispatch-order control and private arm assignment/commitment before admission. Record unsupported randomization rather than claiming it occurred.

Require fresh worker conversations/process contexts and a fresh Phase 1 scorer context. Quarantine previous case reasoning, prior solution/oracle exposure, outputs, evaluation material and persisted context from those participants. An exposed operator may prepare attributable frozen evidence under an independently checked allowlist, but cannot supply answer-bearing interpretation to workers or the blind scorer. The full plan, prior case reports and evaluator-only artifacts remain outside their allowed views. A new task name alone does not establish freshness.

Use the existing supported access/context boundary and separate clean Git object stores, checkouts and output scopes; a shared worktree object store can expose withheld refs/objects. A must be unable to read the governed PK/history packs; B must be unable to read C's historical pack; no arm may read other arms or the oracle. Capture actual context/tool/source receipts and check inherited/native context. If exclusion or visibility cannot be demonstrated, stop fresh-blind/causal qualification; a known-case operational rehearsal needs an explicit bounded disposition and must be labeled as such. Do not add services, knowledge stores, permission schemes, credentials or dispatchers to assert isolation.

Model pretraining and stochastic variation cannot be eliminated by equal configuration. The current one-A/B/C triplet supports bounded score, action, outcome and timing observations only. Its causal PK_EFFECT, HISTORICAL_REUSE_EFFECT, FULL_KNOWLEDGE_EFFECT and ACCELERATION_EFFECT remain INCONCLUSIVE, even when a material contrast below is observed. EFFECT_VALIDATED or ACCELERATION_DEMONSTRATED would require a separately authorized, preregistered replicated design with fixed sampling/order controls and an inference rule before any runs; this plan does not prescribe or authorize additional runs. Poor results are not a reason to repeat arms.

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

Before handoff, bind the exact PK revision/body digest to its applicable governance decision, authority/policy and current eligibility/selection readback. The existing policy determines required approval; this is not a blanket new Human gate or a new publication authority.

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

For **Arm C consumption**, however, any historical/reusable knowledge selected for worker handoff must pass the existing applicable governance/eligibility rules and be frozen as an exact reusable-knowledge revision before execution. This is a Q1 V1.1-D2 consumer requirement; it does not retroactively make publication a V1.1-D1 reconstruction PASS condition.

The Q3→Q4 gate requires an exact revision/body-digest-bound governance decision and eligibility/selection receipt recording the existing authority, actor/policy, publication/lifecycle status, freshness/validity, target applicability, trust and conflict disposition. Read back the selected eligible revision at admission; pending revisions do not replace an eligible revision. Record raw result and exclusions, not just a citation or ADOPTED marker. Missing, expired, conflicted or unverified eligibility blocks Arm C handoff. Retain these receipts in the evidence package and completion checklist; D1 reconstruction may still be dispositioned independently.

Preserve PA-05's bounded negative protection: demonstrate that an absent/ambiguous link, contradicted/reverted history or post-cutoff source is not promoted into current truth. Manual preparation is an execution mode, not itself a D1 failure.

---

## 7. Required SA Deliverable Contract

All three arms must deliver System Analysis to the same contract.

SA must include:

1. intent/problem interpretation;
2. Product behavior and capability analysis;
3. Level 1 system boundary/context;
4. Level 2 current Product/module realization;
5. Level 3 affected component/responsibility analysis;
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
3. Level 2 To-Be architecture/realization delta;
4. Level 3 To-Be component/responsibility design;
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

## 9. Architecture Hierarchy and C4/Supporting-View Contract

Q1 evaluates architecture depth and model fitness, not diagram count.

The qualification requires a three-level architecture hierarchy for every arm:

```text
Level 1 — System Context
        ↓
Level 2 — Product / Module Realization
        ↓
Level 3 — Component / Responsibility
```

All three levels are mandatory. They must be mutually traceable and consistent with the same frozen evidence.

C4 terminology is used only where its abstraction is valid. In particular, a Maven/JAR/module is not automatically a C4 Container. If a real deployable/runnable C4 Container exists, Level 2 may use the C4 Container view directly. If not, the arm must still provide the mandatory Level 2 Product/Module Realization view and explicitly label it as a qualification architecture view rather than a C4 Container.

Likewise, Level 3 is mandatory even when there is no strict C4 Container scope. In that case use a clearly named Component/Responsibility view rather than falsely claiming a C4 Component diagram.

### 9.1 Mandatory structural levels

#### Level 1 — System Context

Show:

- the software system under analysis;
- relevant users/actors/external systems;
- the behavioral boundary of the target problem;
- material external dependencies relevant to the case.

For Q1, this should establish the Resilience4j system boundary and the application/backend interaction without collapsing into module/code detail.

#### Level 2 — Product / Module Realization

Show the major internal realization units that own the Product capabilities involved in the target behavior.

The worker derives the relevant realization units from the frozen intent and current evidence. The worker-facing contract must not name an expected hidden change surface or direct attention to a target-specific component from the evaluator's answer. Investigate and disposition every material unit supported by that permitted evidence.

Each Level 2 unit must carry responsibility, evidence refs and one of:

```text
CONFIRMED
EXCLUDED
UNRESOLVED
```

A Level 2 unit may correspond to a C4 Container when the runtime abstraction is valid; otherwise it remains an explicit Product/Module realization element.

#### Level 3 — Component / Responsibility

Decompose every material Level 2 unit into the components/responsibilities needed to explain the current behavior and To-Be design.

The worker derives material responsibilities and mechanisms from its allowed evidence. Component names, expected fixes and target-specific decomposition from the oracle are not part of the worker contract.

Level 3 must identify:

- component responsibility;
- relevant interaction/dependency;
- current behavior;
- To-Be responsibility/change;
- evidence source;
- whether the component changes or is explicitly no-change.

### 9.2 Supporting views

For Issue #480, the required supporting architecture evidence is:

| View | Q1 expectation |
| --- | --- |
| Level 1 System Context | **Mandatory** |
| Level 2 Product/Module Realization | **Mandatory** |
| Level 3 Component/Responsibility | **Mandatory** |
| C4 Container | Use when a valid runtime/container boundary exists; otherwise do not mislabel Level 2 |
| C4 Component | Use when valid under a C4 Container; otherwise do not mislabel Level 3 |
| C4 Dynamic | **Mandatory** for As-Is and To-Be critical runtime behavior |
| State Diagram | **Mandatory** for the CircuitBreaker state mechanism; non-C4 supporting model |
| C4 System Landscape | Normally N/A unless evidence shows a portfolio/system-of-systems concern |
| C4 Deployment | Normally N/A unless deployment topology materially affects the defect |
| C4 Code | Optional |

Every omitted conditional C4/supporting view needs a one-line applicability rationale.

### 9.3 Cross-level traceability gate

The three mandatory architecture levels must form one consistent reasoning chain:

```text
L1 system behavior/problem boundary
        ↓
L2 owning Product/module realization
        ↓
L3 component/responsibility mechanism
        ↓
Dynamic/state failure mechanism
        ↓
To-Be design / verification implication
```

A material break in this chain is an architecture-depth defect.

Examples:

- L1 identifies behavior whose current-source ownership is omitted from L2;
- L2 puts a unit in scope, but L3 cannot explain its affected responsibility;
- L3 changes a completion handler but Dynamic/State views do not show how the behavior changes;
- implementation changes a component absent from L2/L3 with no justified late evidence.

### 9.4 Architecture-quality dimensions

Evaluate:

- Level 1 correctness;
- Level 2 completeness and responsibility correctness;
- Level 3 component/responsibility depth;
- view fitness;
- abstraction integrity;
- model correctness;
- evidence grounding;
- cross-level/cross-view consistency;
- decision usefulness.

More diagrams do not improve the score by themselves.

A material cross-level or cross-view contradiction is a quality defect.

### 9.5 Absolute conformance and evaluator boundary

For each arm, record absolute conformance with mandatory L1/L2/L3, As-Is/To-Be Dynamic, applicable State and material cross-level traceability. Missing a mandatory level/view or a material traceability break prevents any Q1 V1.1-D2/D3 effect PASS, regardless of relative score. Preserve the score but disposition the affected effectiveness comparison INCONCLUSIVE with the unmet contract; a shared omission cannot cancel out as equal deductions.

The generic SA/SD/view contract may be projected to workers. Case-specific expected units, hidden change surfaces and component decompositions belong only to the withheld evaluator material and are not released to workers or the Phase 1 scorer. They may inform Phase 2 comparison after all first-SA/SD scores are sealed. This plan's case/oracle sections are never a worker input.

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

SA/SD quality is scored before PR #484 or hidden solution evidence is revealed. The primary artifacts are each arm's first complete native SA and first complete native SD, with exact original revision/hash and delivery receipt fixed before review, coding, rescue, feedback or grading. Preserve incomplete/failed outputs; do not select a later best revision. Later repairs and engineering results are separate secondary evidence.

### Phase 1A — Independent architecture/engineering scoring

- Freeze Arm A/B/C submissions.
- Use mandatory technical-only anonymous packets and a frozen anonymous evaluation order; unsupported randomization is reported explicitly.
- Evaluator has the frozen target Sources but not PR #484.
- Score SA/SD quality, depth, evidence grounding and downstream usability.
- Freeze the scores and findings.

Before launch, independently inspect the permitted packet projection and rubric. Before scoring, inspect the actual packets again without exposing the arm map. Allow only frozen technical SA/SD reasoning, diagrams, verification implications and necessary technical evidence excerpts with neutral source aliases. Preserve claim/evidence relationships, legitimate technical citations, wrong assumptions and unknowns; never rewrite the design to improve it.

Exclude arm/condition labels, PK/history retrieval or ADOPTED/MethodUsed/feedback markers, native actor/role/workspace/run identities, private runtime/workspace paths, routing/prior-run history, native Mission clocks/elapsed timing/resource-usage/cost, oracle clues and operator interpretation. Retain rubric-relevant technical behavior timing, synchronization/order constraints, source/code context and evidence through neutral aliases; metadata removal must not hide a technical defect or citation. Never pass a raw native envelope and rely on the scorer to ignore these fields. Keep a private original-to-packet byte/hash correspondence and removal ledger. Substance can still suggest a condition; report that limit rather than claiming perfect blinding.

The fresh scorer receives only validated packets, the frozen generic rubric/conformance contract and permitted pre-cutoff technical references. Seal original score/report bytes and hashes before revealing arm/knowledge identity or oracle material. On exposure, stop that scorer, preserve its partial work and record the disclosure boundary; no blind claim survives that exposure. A replacement scorer requires a pre-frozen integrity rule and unchanged artifacts, not a solver rerun or post-hoc packet tuning.

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

Also trace direct Sources through their attributed observations/correlation or synthesis to the specific SA/SD decision, action and observed outcome. Specifications, design/implementation plans, code, tests and runtime/Mission evidence remain Sources; they need not become PK/WK. Record retrieval, selection, declared adoption, actual decision use and observed effect separately. PR→WK alone is not the engineering-value trace.

---

## 12. SA Quality Rubric — 40 Points

| SA dimension | Points |
| --- | ---: |
| Intent / Product behavior | 5 |
| Capability understanding | 5 |
| Level 2 architecture / realization | 5 |
| Level 3 component/responsibility depth + view correctness | 4 |
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
| Level 2/3 architecture and component design | 6 |
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

Proposed material-contrast thresholds for Q1, to be adopted and frozen before launch. They classify bounded observed contrasts; they do not overcome §4.1's single-triplet causal limitation. Every rule requires §9.5 absolute architecture conformance and valid admission/blinding evidence.

### PK_EFFECT — Arm B vs Arm A

Require all of:

- Arm B SA score >= Arm A SA score;
- Arm B SD score >= Arm A SD score;
- Arm B SA+SD >= Arm A SA+SD + 8 points out of 80;
- at least three material reasoning layers in the Coverage Matrix improve by one grade without a critical-layer regression;
- no critical architecture/evidence regression.

### HISTORICAL_REUSE_EFFECT — Arm C vs Arm B

A bounded historical-reuse contrast is material only when all of the following hold:

- Arm C SA score >= Arm B SA score;
- Arm C SD score >= Arm B SD score;
- Arm C SA+SD >= Arm B SA+SD + 4 points out of 80;
- no critical-layer or architecture/evidence regression;
- at least one exact history→reasoning→decision→action trace supports a material improvement.

Examples of a qualifying decision improvement are:

- a current change surface that B missed;
- a dependency/constraint/risk B missed;
- a materially stronger verification design;
- removal of an unsupported assumption;
- a meaningful SA/SD score increase.

The +4/80 threshold is mandatory, not optional corroboration. A smaller contrast is NO_MEASURABLE_UPLIFT as a bounded observation; a worse score/critical regression is a bounded REGRESSION. Missing trace, contract failure, exposure or invalid admission is INCONCLUSIVE. None authorizes a causal EFFECT_VALIDATED verdict for one triplet.

### FULL_KNOWLEDGE_EFFECT — Arm C vs Arm A

Require:

- no critical regression;
- Arm C SA+SD >= Arm A SA+SD + 10/80;
- material uplift in architecture/change-surface or behavior/verification depth;
- causal evidence showing which Product Knowledge and/or historical knowledge contributed.

Freeze concrete full/partial/no-credit item anchors, applicability/denominators and critical defects with these rules before execution. Do not lower thresholds or remove unassessable items after seeing results.

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

All eight listed hidden behaviors are mandatory. Before launch freeze the exact fixtures/commands, expected pre-existing error policy, required baseline/regression gates and any diagnostic-equivalence rule. Any missing/failed mandatory behavior or critical regression fails functional qualification; nominal completion or a later ad-hoc compensating check cannot replace that gate. Preserve raw failures and attempts.

### 15.2 Outcome rubric — 20 points

| Outcome dimension | Points |
| --- | ---: |
| Patch/behavior correctness | 8 |
| Hidden verification | 5 |
| Minimality / regression control | 3 |
| Reviewer usability / avoidable rework | 4 |
| **Outcome total** | **20** |

Exact textual or file-for-file agreement with PR #484 is not required.

Per-arm functional qualification requires full 8/8 patch/behavior-correctness credit, full 5/5 hidden-verification credit, every frozen mandatory verification gate passing and zero critical regressions. The correctness floor is therefore 13/20; the other seven points report minimality and review/rework quality and cannot compensate for incorrect behavior. Freeze concrete point anchors and critical failures before execution.

Report B−A, C−B and C−A outcome contrasts separately. A bounded better-outcome observation requires the treatment arm to meet that correctness floor, a strictly positive outcome-score difference, no critical regression and an exact decision→action→outcome trace. Equal qualified scores are NO_MEASURABLE_UPLIFT; a worse score or critical regression is a bounded REGRESSION; missing gates/trace or invalid evidence is INCONCLUSIVE. Qualification of one arm is a direct verification fact, not causal validation. The current single triplet's causal ENGINEERING_OUTCOME_EFFECT remains INCONCLUSIVE under §4.1 even when these bounded observations are present.

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

Any Q1 V1.1-D3 effect candidate requires the relevant V1.1-D2 material-contrast rule, absolute architecture conformance, §15's functional floor/mandatory gates and no critical downstream regression. Better SA/SD alone does not establish a better engineering outcome; report the separate outcome contrasts and trace. The one-triplet causal effectiveness verdict remains INCONCLUSIVE.

Allowed dispositions:

- `EFFECT_VALIDATED`
- `NO_MEASURABLE_UPLIFT`
- `REGRESSION`
- `INCONCLUSIVE`

An unfavorable result is retained. Do not swap the case or change the oracle to manufacture uplift.

For this single triplet, report the actual scores, conformance, functional results and bounded contrast dispositions alongside INCONCLUSIVE causal verdicts. EFFECT_VALIDATED is unavailable without the separately preregistered replicated evidence described in §4.1.

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

Freeze T0–T5 event definitions and identical absolute qualification gates before launch. Keep creation, dispatch, start, artifact delivery, run completion and readback distinct; missing required gates mean that endpoint was not achieved. Report queue/active/wall time, PK/history setup, review/rework/rescue, packet/grading and feedback overhead separately. The single triplet permits bounded timing observations only; causal ACCELERATION_EFFECT is INCONCLUSIVE and ACCELERATION_DEMONSTRATED is unavailable.

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
- prior-exposure/context or scoring-packet leakage violating the frozen admission/blinding contract.

Invalid runs are preserved with the reason. A rerun, if necessary, uses the same frozen inputs/rubric. Poor engineering quality is not a valid reason to rerun.

Freeze any permitted bounded correction/replacement rule and deadline before admission. A critical source, eligibility, context, isolation or blinding gap stops dependent work; an operational-only downgrade must be explicit and cannot be reported as fresh blind causal qualification. Previously completed case results remain unchanged.

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
│  ├─ product-context-receipt.json
│  └─ revision-eligibility-selection-receipt.json
├─ historical/
│  ├─ H1-pr394/
│  ├─ H2-pr173/
│  ├─ d1-reconstruction.json
│  ├─ reusable-knowledge-revision.json
│  └─ revision-eligibility-selection-receipt.json
├─ admission/
│  ├─ fresh-context-and-prior-exposure-receipts.json
│  ├─ allowed-view-and-access-boundary-receipts.json
│  └─ final-freeze-readback.json
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
│  ├─ technical-packet-contract-and-inspection.json
│  ├─ score-seal-and-disclosure-receipt.json
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
Q3a exact PK/history eligibility + fresh-context/access/packet checks;
    final manifest seal and independent readback; stop on unmet admission
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
- the Q3a final manifest/readback, prior-exposure/context/access and packet checks are satisfied, or the case is explicitly stopped/downgraded;
- V1.1-D1 is dispositioned from real historical evidence;
- exact consumed PK/history governance and current revision-eligibility/selection receipts are retained;
- all A/B/C arms reach the same predeclared endpoint or are honestly invalidated;
- first-SA/SD artifacts, absolute architecture conformance and mandatory engineering gates are dispositioned independently of relative scores;
- blind SA/SD scoring is frozen before oracle reveal;
- PK and historical knowledge influence is traceable to material reasoning/decisions;
- hidden outcome verification is complete;
- quality and acceleration conclusions are separate;
- unfavorable/no-uplift results are preserved;
- single-triplet causal effectiveness/acceleration verdicts remain INCONCLUSIVE; bounded observations are not relabeled as validated effects;
- no target-solution leakage, fabricated linkage, Product-truth elevation from history, or post-hoc rubric/case changes occurred.

Q1 completion does not establish company/Azure qualification, production deployment, Human DONE, or universal Swarm effectiveness across all engineering domains.
