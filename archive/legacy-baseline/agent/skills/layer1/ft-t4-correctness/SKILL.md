# Skill — FT-T4 Correctness

> **Physical status:** STRUCTURAL_ONLY / not fresh-context validated.

**Inputs:** exact active Intention, Spec, ImplementationBundle and candidate revisions.  
**Output:** `correctness.md` (+ evidence appendices).  
**Gate:** `PASS | FAIL | INCONCLUSIVE`.

T4 is independent from T3: distinct accountable owner, distinct evaluation context, no candidate mutation authority during evaluation, and no inheritance of T3 self-verdict as authority.

Procedure: pin exact lineage; verify evidence integrity; evaluate every criterion’s active V&V disposition; verify Spec conformance and validate Intention/intended use separately; assign criterion verdicts; calculate coverage/scope; record limitations/unobserved scope; determine earliest re-entry; calculate overall gate.

T4 cannot repair the candidate then approve it in the same independent evaluation, rewrite Spec to make candidate pass, or introduce new feature scope via evidence.
