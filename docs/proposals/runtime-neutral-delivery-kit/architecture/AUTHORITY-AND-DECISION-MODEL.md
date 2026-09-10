# Authority and decisions

Preserved requirements: AUTH-001–003, FD-T1-001–002, FD-T3-007, PK-005.
Additional profiles below are proposed controls, not a new authority hierarchy.

| Decision/action | Owner | Required proof |
|---|---|---|
| Product meaning, accepted knowledge, intent/criteria change | Human Authority | Authenticated approval of exact revision/digest and scope |
| Material Spec, architecture, authority or scope change | Human Authority approves; FDP applies | Decision bound to proposed change and receiving controls |
| Maintain five controls; issue/revise engineering contracts | Feature Delivery Plane (FDP) | One authorized writer; current control commit; required Human decision where material |
| Scheduling, local decomposition preserving approved contract, tools, retry | Execution Plane (EP) | Exact envelope; dependency and mutation proof; authorized runtime profile |
| Material DAG/objective/input/claim/requirement change | FDP replan | New immutable ExecutionPlan; T2 first for design/requirement changes |
| Independent review | Attributable independent actor in EP or FDP T4 role | Actor not any candidate producer/integrator; distinct run; exact candidate |
| T4 verdict and routing | FDP with independent evaluation | Frozen criteria, exact integrated candidate, evidence coverage |
| Merge/deploy/publication/spending/secrets/destructive or external action | Explicit scoped Human authorization and receiving policy | Destination, action, limits and exact target; no inference from engineering gates |
| Terminal parent closure | Human authorizes; FDP records | Exact closure candidate, valid review/evidence; then control reconciliation |

FDP owns only one active writer per execution. Competing writers produce
CONTEXT_CONFLICT; an agent must not invent last-writer-wins. EP coordinator is
routing-only for the current Multica binding; it does not implement/review via
hidden internal agents. A neutral binding may use other machinery only after its
profile is approved and equivalent attribution/isolation is demonstrated.

## Decision procedure (DEC-01)

1. Verify assignment, control commit, envelope digests and input accessibility.
2. If authority statements conflict, stop the envelope with CONTEXT_CONFLICT.
3. If a required dependency/environment/permission/resource is missing, report
   PLAN_BLOCKED. Continue only independent work already authorized by the DAG.
4. If the plan contradicts active controls or exact code, report PLAN_CONFLICT.
5. If API, scope, acceptance or construction instructions must change, report
   PLAN_CHANGE_REQUIRED; attach evidence and the smallest proposed change.
6. Otherwise execute the authorized work. Choose reversible implementation details
   inside constraints without asking again. Record a material assumption together
   with its test and invalidation trigger. Never use assumptions for authority.

PLAN_* reports are envelope diagnostics, not new WorkItemResult reason codes.
An out-of-claim mutation need is WorkItem BLOCKED/SCOPE_CONFLICT plus
PLAN_CHANGE_REQUIRED. A missing input is BLOCKED/INPUT_UNAVAILABLE plus
PLAN_BLOCKED. Do not encode routing decisions in core results.

## Authorization profile (DEC-02)

Receiving policy binds issuer identity, subject role, allowed actions/resources,
exact contract/candidate, limits, validity interval and revocation reference.
Verify authorization at admission and before each protected side effect. A
still-valid scoped approval persists across retries; do not ask repeatedly.
Unknown identity, revoked approval, changed scope or expired grant fails closed.
Runtime service credentials authenticate transport, not Human decisions.

## Invalidation (DEC-03)

| Change | Required consequence |
|---|---|
| T1 intent/criteria | Stop old cycle; Human approves new IntentSpec/cycle |
| T2 requirement/design/surface | New DeliverySpec and affected plan; revalidate downstream evidence |
| DAG or WorkItem contract/input | New plan identity; no retry label for changed contract |
| Candidate bytes | New candidate identity; previous review cannot authorize it |
| Policy/permission revocation | Suspend affected actions immediately; re-evaluate pending approvals |
| Knowledge superseded | New context cannot silently replace pinned input; FDP assesses applicability |

Historical evidence remains immutable and visible as historical. Applicability
is a separately computed decision over exact dependencies. Evidence for an
unchanged dependency may be reused only with an explicit validity proof.
