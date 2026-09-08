# Skill — FT-T3 Implementation

> **Physical status:** STRUCTURAL_ONLY / not fresh-context validated.

**Input:** exact active `spec.md` (`SPEC_READY`).  
**Output:** `ImplementationBundle`, core `implementation.md`.  
**Gate:** `CHANGE_SET_READY | BLOCKED`.

Procedure: pin Spec and repo bases; verify ownership/permissions; map tasks to obligations; create reviewable candidates only for approved `CHANGE` repos; produce evidence-backed `VERIFY_ONLY`/`NO_CHANGE` dispositions without fake candidates; coordinate cross-repo dependencies; run implementation checks; pin heads/paths/migrations; record deviations; stop and re-enter T2 on new required scope; prepare T4 handoff.

No merge/deploy/release authority is implied by the gate. T3 cannot self-declare Correctness.
