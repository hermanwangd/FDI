# FDI Framework v0.1-rc4 — Review and Fix Note

## Review verdict on rc3

rc3 added the requested Capability / Feature / `SKILL|CODE|SKILL+CODE` catalog, but four issues remained:

1. `SKILL` type did not name a Skill owner and could be misread as one new Skill per Feature.
2. FC-12 Feature Learning and FC-13 Maintain Product duplicated evolution/governance responsibilities.
3. FC-03 / FC-04 did not sufficiently reuse the existing `PA-Codebase-Inventory` and `PA-Historical-Delivery` Skill boundaries.
4. FC-08 through FC-11 decomposed Layer 1 behavior without mapping every row back to existing root/helper Skills.

## rc4 corrections

- Adds `Execution Owner` to every FrameworkFeature.
- Reuses 11 already-physicalized Layer 1 / FT-T2 / Layer 2 Skills.
- Limits new Lean Skill candidates to four: `PK-S1` through `PK-S4`.
- Corrects repository inventory from `CODE` to `SKILL+CODE`.
- Maps T2 catalog directly onto the five existing FT-T2 helper Skills plus root T2 Skill.
- Merges rc3 `Learn From Feature Delivery` + `Maintain Product Intelligence` into one `FC-12 Evolve Product Intelligence`.
- Makes FC-05 the single Product Intelligence governance/publication path.
- Renumbers Packaging/Consumption to FC-13.
- Keeps the rc2 Lean Core public contract surface at 10; no new public contract was added.
- Keeps 12 Framework Release Gates unchanged.
- Does not promote any deferred Maintenance Engine artifacts back into v0.1.

## Outcome

rc4 is intended to make the catalog usable for implementation/backlog ownership without creating a Skill explosion or a second workflow/governance model.
