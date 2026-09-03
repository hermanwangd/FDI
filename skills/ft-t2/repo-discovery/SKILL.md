# Skill — repo-discovery

**Purpose:** produce a bounded high-recall `CandidateRepoSet`.

**Inputs:** `IntentSpec`; selective PA-03/PA-05 Product Intelligence; bounded source search; optional bounded Structural Intelligence.

**Procedure:** start from non-exhaustive seeds; query PA-03 identity/navigation; use PA-05 history as prior; use Structural Intelligence only as non-authoritative candidate/navigation signal; perform bounded semantic/lexical/current source discovery; preserve uncertain candidates and discovery bounds.

**Output:** `CandidateRepoSet`.

**Optimization:** recall before early precision pruning.

**Must not:** declare candidate repo `CONFIRMED`; use GroundTruth/target-answer leakage; invent `GRAFEL` candidate basis; treat history as current truth; silently drop materially plausible candidates without rationale.
