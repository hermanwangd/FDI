# Workflow portability

WP-01: A portable bundle contains immutable engineering references, a resolved
execution envelope, selected skill bytes, behavioral policy, required capability
profile, output/evidence schemas and acceptance cases. The package lock covers
all transitive local assets. Secrets and environment identities are separate.

| Level | Claim | Mandatory evidence |
|---|---|---|
| P1 definition/execution | Same exact engineering contracts and skills execute on two bindings | Both satisfy dependency, scope, review, retry and result semantics |
| P2 artifacts | Receiving binding verifies and consumes exact output/evidence | Digests, provenance, candidate identity and evidence applicability verified |
| P3 safe-point resume | An unfinished plan resumes only at a proven quiescent boundary | No live writer/unknown side effect, complete immutable result ledger, reauthorized binding |

P1/P2 are initial targets, not achieved claims. P3 is deferred; checkpoint format
is deliberately not frozen. No hot agent memory, live tool connection or model
internal-state portability is promised. A company binding may execute a pending
WorkItem only after predecessors, exact inputs, permissions and capabilities are
rechecked. Revised inputs are a replan, not resume.

WP-02: Required capabilities are matched by exact capability identifier and
major semantic version, including atomicity/isolation requirements. Unknown
required capability fails PLAN_BLOCKED. Optional capability omission must be
declared with its effect; it cannot waive mandatory evidence or parallelism.
Bindings cannot secretly translate to weaker semantics. Core WorkItems have no
runtime selection fields. Profile selection and role/skill mappings are external.

WP-03: Contract major version changes for removed/changed semantics or newly
required fields. Minor releases may add separately negotiated optional profiles;
strict v1 readers still reject unknown core fields. Patch changes cannot change
accepted behavior. Pin exact package version and lock digest per execution.
Upgrades require conformance replay and FDP assessment; never mutate in-flight
contracts or substitute newer skills. Retain previous immutable artifacts and
profile for rollback; rollback itself must reconcile outstanding effects.

WP-04: Full IntentSpec, ProductContext, DeliverySpec and ChangeSurface field-level
schemas are not frozen by Software-Factory. This kit treats them as opaque typed,
digest-bound references and lists required semantic validators. Its proposed
envelope schema does not freeze or invent their contents. Import mapping must
detect semantic mismatch, not merely resolve a similarly named field.

WP-05: PORT-001 change-reference export and P1–P3 workflow portability are separate.
In a company repository with no common baseline, source control examples and
skills are reviewed adaptation inputs. Rebind local controls, ownership and
acceptance under company authority. Do not cherry-pick, overwrite controls or
present the source execution's verdict as company verification.
