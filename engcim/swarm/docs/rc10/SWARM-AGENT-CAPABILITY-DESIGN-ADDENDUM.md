# Swarm agent capability DesignSpec addendum — A1

**Status: DRAFT_FOR_REVIEW / NOT_ADOPTED / QA_SAMPLE_ONLY.** This docs/bootstrap rescue prepares a capability/test contract; it does not replace native Swarm reasoning, tests, QA/Verifier or adoption. No role/model/Skill assignment, source policy or runtime behavior changes. Parent review of this exact sample is required before broader role specification. Current affected QA accounting analysis remains separately authorized and unblocked.

## Design ownership and scope

The domain owner is the sealed [Swarm Execution Model — Part Z](../../baselines/rc6/runtime/package/engcim-swarm-package-RC6/docs/swarm-execution-model.md), SHA-256 `c27256883132f49ecba4d75d4c9029a465b9638f3cc9dc5df9dfb38cad864119`: Z1 role/execution mapping, Z9 revision freshness, Z10 Reviewer/Verifier separation, Z14 capability-based specialist selection, Z20/Z21 actual native behavior proof. The sealed [QA role](../../baselines/rc6/runtime/package/engcim-swarm-package-RC6/agents/qa-tester.md), SHA `54ca880fe0dacec1b0c9d818fb22f51a76b4db58bae98aa274536bed4455280f`, supplies QA responsibility and AC→case requirements. [Selected current-work-package guidance](../../instructions/ROLE-GUIDANCE.md) remains its existing scoped candidate, unchanged. Scenario S06 and test-design/test-architecture/runtime-qa retain their existing responsibilities.

This is a clearly scoped authoring addendum to that design, not a replacement execution architecture or a second deployed policy. Sealed bytes remain historical/source authority as applicable; this addendum and its L2 are proposed until exact review/adoption. No adoption is inferred from writing a document or matching metadata. The installer `SWARM-V11-RELEASE-PACKAGE-DESIGN.md` v4 is **DRAFT_FOR_REVIEW**, primarily packaging/installation; its agent/Skill asset references do not make it the role-behavior design owner. Its exact bytes SHA `706880d18f2d759dbc7fb7c6cd3b7ea67e38ce1184e5abe0c1ed5ef56476cfb9` are retained as a [packaging reference](../../../../validation/rc10/agent-capability-tests/qa/retained/SWARM-V11-RELEASE-PACKAGE-DESIGN-v4.md), external authoring worktree untouched.

## Two-level specification and references

- **L1, this DesignSpec addendum:** role/responsibility, capability summary, input/output and collaboration; link detailed L2. Summaries are navigational, not competing AC definitions.
- **L2, [QA capability spec](agent-capabilities/qa.md):** stable capability IDs; versioned behavior/input/output contract and constraints; normal/error/edge/recovery behavior; measurable AC and owned test references. This is the one proposed detailed requirement authoring source for the sample.
- **[Owned test specification](../../../../validation/rc10/agent-capability-tests/qa/TEST-SPEC.md):** links stable capability/AC IDs and defines stimuli, fixture/evidence needs and execution bindings. It does not copy normative acceptance into a second requirement source.
- **[Instance bindings](agent-capabilities/instance-bindings.json):** instance→role-template/revision/applicability plus each instance's owned suite/evidence gaps. No per-instance duplicate specification files.
- **[Source pins](agent-capabilities/SOURCE-PINS.json):** exact lineage, retained findings and limits. Method guidance stays in the canonical Swarm Dev Skill, not requirement tables.

After scoped adoption, materialized instructions/tests should reference the accepted L2 IDs/version/hash under the existing generation/selection paths; divergence must be reconciled, never maintained as two current detailed definitions. That future adoption/materialization is not performed here. Existing reviewed r11 and original QA-freeze bindings/denominators remain unchanged.

## QA role summary

| Role / responsibility | Capability summaries | Inputs | Outputs | Collaboration / detailed owner |
|---|---|---|---|---|
| Swarm QA Tester: systematic coverage, meaningful test design/execution and scope-specific assessment | `SWARM-QA-CAP-001`: executable independent method; `SWARM-QA-CAP-002`: coverage/evidence applicability; `SWARM-QA-CAP-003`: scoped operation accounting and stored-output binding | Assigned goal/AC/non-goals; candidate/contracts; selected procedures; permitted acquisition; dependency/approval status; versioned evidence | Prospective method/coverage; actual ledger/report only for executed work; findings/unknowns/gaps; exact artifacts | Existing Reviewer assesses current method/result; Verifier independently reproduces evidence; developer corrects product defects. No role substitution. Detailed criteria: [L2 QA](agent-capabilities/qa.md) |

The sample covers three QA capabilities, not the entire role or all 19 role templates. Full role completeness must be separately reviewed using source requirements. Normal/negative cases, boundary/recovery variants and independent actual operations/output are designed at capability level. Each compatible instance owns its own runnable suite; one instance's native result never establishes another's PASS. Model/assignment readback and actual consumption are different evidence fields.

## Draft instance coverage and test status

Supplied controller inventory pins: 19 draft roles / 114 proposed cases (`2c732ea058f1b009ca84445295f32697ca5a054cf96cbf8877690547aff57703`); 203-instance reconciliation (`ecbc5ae8526713e4850c9abf12ed7a17205ae1e8db8209d82acb0c8fc92da2e5`). The latter reports 133 standard role instances (73 metadata matches, 60 differences), 70 unverified mappings, 202 untested instances and **zero current unit PASS**. These are retained draft metadata, not fresh provider discovery, frozen fixtures, accepted capability assignments or a new execution claim.

Only seven supplied QA bindings are projected here; applicability to this new A1 template and consumed Skill content remains unresolved. Native QA method v1/v2 evaluations both remain **REVISE**; they motivate source-backed regressions but did not consume this L2 revision. New sample: three capability IDs, nine AC IDs, ten proposed test definitions; every definition **NOT_FROZEN / NOT_RUN**. This docs sample changes no execution denominator or acceptance result.

Next: parent independent review of L1 scope/ownership and complete QA mapping, then disposition sample changes before any broader role documentation. Concrete fixtures, exact instance applicability/admission, same-version native suites and independent grading require their own bounded reviewed operations. Maintain coverage gaps without making all-203 readiness a prerequisite for an otherwise authorized affected-case check.
