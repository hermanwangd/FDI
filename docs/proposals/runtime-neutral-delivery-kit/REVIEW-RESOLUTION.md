# Review resolution

Status: design findings addressed in proposal; implementation/runtime readiness
NOT CLAIMED. This is author self-review, not independent runtime verification.

| Finding | Fix | Verification target |
|---|---|---|
| P1 orchestration overlap | AUTH model: FDP owns engineering/control state; EP runtime facts only | B01/B20; CT-07 |
| P1 vague reliable behavior | DEC-01 + BC-01–07 + six complete skill entrypoints | B15/B19/B21/B27 |
| P1 minimal execution model | Preserve core SF entities; adapter-local transition/cancel/retry semantics | B04–09/B28 |
| P1 untrusted evidence | Exact digests, classifications, trusted attribution and reproduction | B10–13; CT-09–12 |
| P1 undefined portability semantics | P1/P2/P3, strict capability matching, no silent downgrade | B18/B25; G4 |
| P1 skill trust/dependencies | Local catalog, transitive assets, lock, rights status and permission separation | G0; SO-01/02 |
| P2 reviewer independence | Distinct actor/run from all producers/integrators, exact candidate | B10/B11 |
| P2 stale context | DEC-03 invalidation and resume preflight | B14/B19 |
| P2 vague evaluation | Structured scenarios, required/forbidden observations, trace oracle and rubric | G2/G3 |
| Baseline correction | SF authority replaces rc4; T1 and PK skill meanings reconciled | BASELINE-COMPARISON |
| Unnecessary domains | New Factory/task/run domain withdrawn | AUTH-003 preserved |
| Java optionality | Withdrawn; TECH-001 preserved | Adoption step B |
| Unsupported 8 GB guarantee | Proposed enforcement profile plus measured acceptance | B25/G5 |

Architecture summary: two responsibility planes, existing engineering contracts,
portable boundary profiles, six scoped skill entrypoints and independent evidence.

Architecture design review: PASS as a reviewable proposal with explicit boundaries;
FAIL for implementation readiness until ENV-01/02 and non-frozen contract decisions
are approved. Runtime/behavior/P1/P2 validation remains NOT_EXECUTED. Environment
decisions block dependent implementation, not authorship of this proposal.

Critical risks: company runtime may lack durable attribution/fencing; source
rights are not established; semantic validators are not provided as executable
runtime code; behavioral quality varies by model/runtime and must be measured.
Over-engineering avoided: no new lifecycle domain, database, microservice split,
extra T2 helper authority or source-type skill proliferation. Under-engineering
risk remains until failure-injection tests and real feature acceptance run.

Required next decisions: see adoption/ENVIRONMENT-DECISIONS.md. Approved decisions
must be applied by the receiving FDP to its existing controls and selected plan.
No automatic SF-BL-002 status change, company dispatch or terminal closure follows.
