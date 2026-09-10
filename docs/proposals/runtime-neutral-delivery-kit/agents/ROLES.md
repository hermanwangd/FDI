# Roles and capability ownership

Roles are responsibilities, not permanent processes. The skill catalog maps
capabilities to files; runtime actor assignments exist only in a binding profile.

| Role | Plane | Skills | Allowed output/authority |
|---|---|---|---|
| Delivery actor | FDP | sf-feature-delivery | Propose/maintain engineering contracts and controls within authority; route T4 |
| T4 evaluator | FDP evaluation | sf-feature-delivery (T4 mode) | Exact independent verdict; cannot mutate candidate or relax criteria |
| Execution coordinator | EP | sf-execution | Scheduling, intake, review/remediation/integration routing; no active controls |
| Implementer/integrator | EP | sf-execution (specialist mode) | In-claim candidate changes and evidence; not independent review |
| Independent reviewer | EP | sf-execution (review mode) | Candidate-specific findings/verdict; read-only candidate |
| Product analyst | FDP support | pk-s1-product-semantics, pk-s2-product-realization | Observations and proposals only |
| Inventory/history analyst | FDP support | pa-codebase-inventory, pa-historical-delivery | Identity/history evidence only |
| Human Authority | Human | No skill can impersonate this role | Exact semantic/material decisions and terminal closure |

Independent reviewer actor identity must differ from every producer/integrator
of the exact reviewed candidate; a new session under the same actor is insufficient.
Model diversity is optional and not a substitute for independence. T4 must check
the original accepted intent/spec and candidate, not only the implementation
summary. A reviewer that repairs code becomes a producer; obtain another reviewer.
Closure requires human approval even when runtime reviews and T4 pass.

The six entrypoints are proposed implementations of SF capabilities and the EP
role. They do not claim compatibility by sharing names with PKB-001 or rc4 skills.
T1–T4 reasoning stays within FD-Feature-Delivery; no duplicate FT-T2 authority is
introduced. Learning proposals route through PK-S1/S2 with original provenance.
