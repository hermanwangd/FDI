# Java Coding Guidelines

## Purpose and precedence

These are engineering defaults for new or materially changed FDI Java code.
They are supporting guidance, not Product truth, a Framework requirement, a
Backlog, or an execution authorization. The five active control files and
`AGENTS.md` must both be satisfied and take precedence. Domain behavior,
security, evidence authority,
isolation, and acceptance come from their bound Spec and Plan requirement IDs;
this file does not restate or replace them.

The guidelines apply only to executions whose assigned control commit contains
this file and its `AGENTS.md` reference. Existing code is grandfathered unless
the selected Plan changes it. A default becomes a hard completion gate only
when the selected Plan explicitly says so.

## Design defaults

### Cohesion and change coupling

A class should have a coherent reason to change. Review a class for separation
when it independently owns several of parsing, domain decisions, ranking,
validation, orchestration, persistence, serialization, or transport.

Line count is a review signal, not a pass/fail rule:

- above 300 production lines: check cohesion and name the reason to keep it
  together in the handoff when the diff materially grows the class;
- above 500 production lines: prefer separation unless it would increase
  coupling, weaken an atomic invariant, or split a cohesive parser/adapter.

Do not create micro-classes merely to meet a line target. A separation is useful
only when the new boundary has a clear contract and can change or be tested
without coordinated edits across unrelated responsibilities.

### Duplication and trust boundaries

Centralize the same policy implementation within one trust boundary when copies
could drift. Independent validation at different trust boundaries is allowed
and often required. When similar checks are intentionally retained as
defense-in-depth, identify the boundaries and cover their shared semantics with
contract tests.

Prefer shared mechanisms for byte hashing, sealed-input verification,
collision-safe artifact writing, and canonical identity parsing. Do not make
unrelated domains depend on one utility solely to remove textual duplication.

### Public API surface

Keep public surface minimal. A new public API needs a named production contract,
such as a current consumer, provider SPI, framework extension point,
serialization/Spring boundary, cross-repository contract, or migration-first
interface selected by the Plan.

Do not expose an unsealed parsing or validation bypass solely for tests. Prefer
package-local tests, dependency injection, or a narrow validated interface.
Visibility alone does not prove design quality; reviewers check the actual
contract and bypass risk.

### Domain identities

Introduce a typed identity only when it enforces a real boundary. A useful type
defines validation, canonical representation, equality, serialization behavior,
and rejection of cross-domain values. Do not wrap every string in a record when
the wrapper adds no invariant or clarity.

### Errors and determinism

Reject invalid or ambiguous inputs at the boundary that has enough information
to explain the failure. Preserve the error vocabulary required by the selected
contract. Deterministic generators must define ordering explicitly and bind
every output to its exact inputs; do not rely on incidental map, filesystem, or
reflection order.

## Test defaults

Use TDD for new behavior and a characterization test before behavior-preserving
refactoring. Test observable contracts and trust-boundary failures rather than
private implementation details.

An implementation change must not make a failing test pass by silently:

- deleting or weakening an assertion;
- changing a fixture or expected value to mirror the implementation;
- disabling, ignoring, or filtering the test;
- replacing an exact check with a broader snapshot or predicate;
- removing a negative or mutation case.

A legitimate expectation change must bind to the applicable Spec/Plan
requirement or an exact independent-review finding and be identified in the
handoff. Generated-artifact tests should verify exact identity, deterministic
reproduction, collision behavior, and applicable historical-byte preservation.

## Slice and review defaults

The slice-size and context limits in
`validation/pkb001/operations/MULTICA-SLICE-OPTIMIZATION.md` are planning
signals. They do not authorize incomplete migrations, reduced tests, fragmented
design, or skipped review.

The producer performs a compact self-check of the changed code. Use the existing
`code-simplifier` skill only when the diff presents a concrete clarity,
duplication, public-API, or complexity risk; do not start a separate style-only
agent run by default. Apply only findings that preserve the selected behavior
and evidence contract.

The already-required independent reviewer checks both Spec conformance and the
applicable guidelines in one review run. No additional style-review run is
required. A review should cite concrete paths and consequences rather than fail
a candidate solely for a line count or subjective preference.

## Deviation record

When a selected change intentionally departs from a default, record in the Plan
or delivery handoff:

```text
Guideline / affected paths / technical reason / risk
Compensating test or review evidence / disposition / follow-up Backlog or N/A
Expiry condition or permanent rationale
```

An undocumented “special case” is not a reviewable deviation. A documented
deviation is evidence for a decision, not automatic approval.

## Automation adoption

Do not add a Maven quality plugin merely because it can measure an easy proxy.
After at least three comparable Java slices, use their independent findings to
select at most one pilot gate. Record its false positives, runtime cost,
bypassability, and effect on first-pass rate before making it required.
