# Skill — dependency-closure

**Purpose:** recursively expand and reconcile the bounded feature boundary until material dependencies are dispositioned or explicitly unresolved.

**Inputs:** exact Intention, CandidateRepoSet, ChangeSurfaceSet, EvidenceRecords, bounded Context/Structural capabilities.

**Procedure:** for material surfaces inspect upstream callers/downstream consumers/shared contracts/events/schemas/config/runtime/deployment/validation dependencies; add newly discovered candidates; route them back through current investigation; maintain EvidenceBoundary/coverage ledger; stop only when bounded search yields no material new candidate or unresolved material edges are explicit.

**Output:** `ClosurePackage` with `OPEN|PARTIAL|CLOSED_WITHIN_DECLARED_SCOPE`.

**Must not:** claim absolute completeness; hide unknowns/coverage gaps; turn historical co-change into required current dependency; convert helper closure into `SPEC_READY`.
