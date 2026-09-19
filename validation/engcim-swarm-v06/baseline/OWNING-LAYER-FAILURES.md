# Owning-Layer Register

Only the prescribed ownership taxonomy is used.

| Owner | Current preparation issue | Scope / disposition |
|---|---|---|
| EVIDENCE | S01-NEGATIVE-001 is refuted by exact Petclinic source: URL-driven locale switching is implemented, while the frozen negative claim rejects language selection as unestablished | blocks S01 and PK/top-level seals; create a new gold revision that narrows the claim or separates implementation from Product authority, then rerun S01 review |
| CONTEXT | PC1 R-003 says viewer failures are recoverable, conflicting with the frozen HTTP 404 `retryable=false` rule in S04 Case A and DecisionResponse A | blocks S04 and downstream/top-level seals; create a new Product Context revision with the non-retryable rule and rerun affected review |
| FIXTURE_ENVIRONMENT | Frozen S05 fixture `npm test` fails before test execution because `test/interaction.test.js` imports `openSelectedChart` while `src/interaction.js` exports `selectChart` | blocks S05 and downstream/top-level seals; create a new fixture revision correcting the test/source contract and rerun S05 review |
| EVIDENCE | RC7-B runtime-gated control closure is preserved and independently bound through Gate-0 | scoped control closure is PASS; it does not clear S01–S06 baseline blockers or authorize effectiveness execution |
| EVIDENCE | No S01–S06 effectiveness evidence exists in this preparation task | expected until execution phase; not a baseline PASS |
| CONTEXT | TKMS/Azure MCP unavailable | explicitly excluded input; no success claim |

No current blocker is reclassified as a Scenario, Skill, Control, Composition,
Runtime, or Model Contract defect without execution evidence.
