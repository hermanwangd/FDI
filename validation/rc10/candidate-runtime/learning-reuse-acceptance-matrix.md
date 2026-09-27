# Runtime learning/reuse acceptance — pending actual capture

This maps the existing RUN-SPEC acceptance to observable receipts. It does not change candidate instructions, schemas, governing policy, or Scenario definitions. No row is PASS until a real role run and its exact input/output evidence are available.

## Positive chain

| ID | Required runtime evidence | Current status |
|---|---|---|
| L01 | Codex CLI creates existing-schema MissionLearningSource from actual independently verified S01 result/closure refs; uploaded bytes and source refs resolve | Supervisor input and dispatch receipts exist; validate against subsequent Curator read |
| L02 | Existing Curator derives a useful WORKSPACE proposal from that source; preserves attribution, limitations and conflicts | PENDING RC10VAL-11 |
| L03 | Separate actual Reviewer/Verifier runs inspect the exact proposal; explicit Orchestrator decision binds proposal digest/key/version, actor, policy and evidence | PENDING RC10VAL-11 |
| L04 | Curator creates the governed record in the configured WorkspaceKnowledge project; body/index readback matches and returns existing-schema capture receipt | PENDING RC10VAL-11 |
| L05 | Same capture replay returns the same provider identity/content version; paginated match count is one | PENDING RC10VAL-11 |
| L06 | Clearly labeled lost-ack simulation reconciles that existing record before retry; no duplicate or false transport-failure claim | PENDING RC10VAL-11 |
| L07 | A separate subsequent Mission performs fresh scoped provider reads; selected IDs, provider revisions, exact statements and provenance are in Authorized Visible Context | NOT_RUN |
| L08 | Actual Scenario worker receives that receipt and records how the procedural guidance affected its work; separate Product Knowledge input remains traceable | NOT_RUN |

## Boundary cases — distinguish capture from retrieval

| ID | Controlled condition | Required observable outcome |
|---|---|---|
| N01 | Governance DEFERRED | No successful capture receipt; no selected reusable context. Preserve decision and reason. |
| N02 | Governance REJECTED | No successful capture receipt; no selected reusable context. Preserve decision and reason. |
| N03 | Past validUntil or STALE/SUPERSEDED lifecycle | Retrieval excludes it, retaining freshness/supersession reason and actual fetchedAt. Approval alone does not select it. |
| N04 | Wrong workspace attribution | Capture rejects before wrong-scope write; retrieval excludes a synthetic wrong-scope envelope. No foreign workspace reads/writes. Report these two gates separately. |
| N05 | Wrong owning project or consumer project not applicable | Capture/owner check and retrieval/applicability check are separate assertions. No write to ProductKB or an unrelated project. |
| N06 | Unresolved conflictRefs | Cannot obtain APPROVED reusable capture; retrieval must exclude any controlled conflicting record/envelope even if an index says APPROVED. |
| N07 | Multiple records with same recordKey | Fresh paginated enumeration detects DUPLICATE_CONFLICT and excludes all ambiguous matches until an authorized canonical decision. Do not label sequential read-before-create as atomic uniqueness. |
| N08 | Current Mission evidence contradicts an otherwise eligible statement | Current evidence wins; context receipt excludes the statement with its conflict/evidence refs, without rewriting historical record. |
| N09 | Body/index disagreement or decision bound to old digest/version | No usable context or successful capture while inconsistent. A stale approval cannot authorize changed content. |

Use isolated synthetic test inputs, explicitly labeled and tied to the actual captured record/contract. Do not invent real authority signatures, historical timestamps, or a provider response that was never observed. Keep the approved positive record unchanged. Tests may use controlled synthetic envelopes for unsafe/foreign-scope states, but must identify them as simulations; do not claim a provider returned those states. Durable duplicate fixtures, if used, must be validation-owned, separate from the positive key, and unavailable for normal reuse.

Each negative case must identify the specific failed gate, all other confounders, actual role/run, input hash, output/exclusion receipt, and whether its input was provider-observed or synthetic. A blanket exclusion caused by malformed JSON or an invalid digest does not independently prove stale, visibility, conflict or duplicate handling. A capture rejection alone does not prove fresh retrieval filtering.

## S02 dependency and actual consumption

Do not dispatch S02 before RC10VAL-11's exact proposal/decision/capture/replay gates are evidenced. Use the accepted S01 r3 archive (SHA256 57af68f6f63561e181df6edab6d0adea160620b78c058cf5c610b296581411ba) as Product Knowledge; use the captured WorkspaceKnowledge through a new provider read as a distinct procedural context lane. Preserve the approved positive record. S02 must show the actual Curator input/result, not only a Supervisor-authored selection demonstration.

The two versioned S02 document inputs are already separately hashed in s02-source-manifest.json. Ingest v1 then v2, retain the staged delta/history/conflict disposition, and replay v2. Neither document authorizes source repairs or promotes the unresolved seeded defects. Existing entry-level trusted facts remain separate from provisional product intent/implementation conflicts.
