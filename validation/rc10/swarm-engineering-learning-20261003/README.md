# Swarm engineering experience — seven public-safe review candidates

Status: **LOCAL_CANDIDATES_READY_FOR_REVIEW; NOT_GOVERNED_OR_PUBLISHED**.
Classification: validation evidence / local Knowledge candidates.
Source snapshot: `88002deacc71d041999499680645e9ffb73f0504`.
Local pack revision: **r3 public-safe**; full earlier evidence is preserved in
an owner-only deliverable. This is a pack revision, not a live record version.

Two parallel purposes remain: **external GitHub cases provide validation and
effectiveness evidence; real Swarm engineering, operation and troubleshooting
experiences supply reusable Swarm knowledge**. Neither replaces the other.
The synthetic method in episodes 05/07 was an input in real validation runs;
these seven records are not seven new synthetic cases or rerun requests.

This pack contains **seven WorkspaceKnowledgeProposal** drafts and **seven
MissionLearningSource** drafts using the existing schemas, without extensions.
Full findings and hypotheses are in [OBSERVATIONS.md](OBSERVATIONS.md).

| # | Episode / reporting label | Existing disposition and next review |
| --- | --- | --- |
| 01 | Repository key mismatch — VERIFIED OBSERVATION | Procedural method; verify request-only correction and unchanged evidence |
| 02 | Read contexts disagree — OPEN PROBLEM | Investigation method; keep environmental cause unknown |
| 03 | Execution-layer configuration scope — PROPOSED PRACTICE | Swarm/role guidance candidate; verify exact-field restoration |
| 04 | One directory wait resolved — VERIFIED OBSERVATION; other waits OPEN | Investigation method; legitimate exclusion/automatic start; no fix implied |
| 05 | Actual design use — VERIFIED OBSERVATION | SEMANTIC context/history; incremental benefit remains UNASSESSED |
| 06 | Generated → original-Mission-persisted feedback — VERIFIED OBSERVATION | Procedural method; retain early gap and verified closeout separately |
| 07 | Comparison integrity/timeboxing — PROPOSED PRACTICE | Evaluation guidance; preserve blind failure, cancellation and incomplete stages |

These labels describe reported evidence, not workflow enums, approvals or
independent cloud runtime verification. Six drafts are PROCEDURAL; one is
SEMANTIC. There is no governed record, capture receipt or live publication.

## Public/private boundary and review readiness

- Public-facing sources use `private-evidence:SWARM-EXP-20261003-NN` aliases.
  The owner-only deliverable maps them to exact native/work-item/artifact
  identities, receipts, times and full original findings. Original native bytes
  not accessed by this task remain explicitly owner-reported.
- `private-workspace:...` and `private-mission:...` values are source locators,
  **not provider IDs**. Existing owner/Curator resolves real scope/Mission
  identity and required evidence before live intake. Unknown scope excludes
  live reuse; these files do not define a new mapping service or schema.
- Public-safe material omits credentials, raw private runtime logs, local
  machine/home/profile paths, private thread/workspace/run/Library IDs and
  changeable user-specific model settings. Meaningful public code/version
  references and bounded technical outcomes remain.
- Reviewable operator lessons: name execution layer/fields before configuration
  changes; require actual source/holder evidence before cause claims; reserve
  closeout/readback time inside the agreed window. These are proposals through
  existing owners, not copied private instructions or new authority.
- A single integration owner handles the separately authorized native-worktree
  change, parallel check and **one integration branch / one PR**. This task
  hands over a public-safe patch/bundle; it creates/pushes no PR and changes no
  native runtime or live WorkspaceKnowledge/ProductKB.

## Existing intake, review, disposition, use and feedback

1. Supervisor source intake reconciles each `sources/NN.json` alias with the
   originating Mission, subject revision and exact receipts. Stage findings
   can be sources without fabricated final closure or Human DONE.
2. Existing Curator extraction/correlation/synthesis reviews the paired
   `proposals/NN.json`, deduplicates lineage and preserves observations,
   hypotheses, failed approaches, counterevidence and applicability. These
   drafts do not claim a Curator-issued result.
3. Existing LearningDisposition guidance leaves one-off facts in Mission history,
   applicable methods in the WorkspaceKnowledge proposal lane, reusable Swarm/
   Skill methods with their existing maintainer and native mechanics with the
   platform owner. No capability is deployed by recording a lesson.
4. The applicable existing policy owner supplies required review/decision
   evidence. Missing authority retains a proposal; no approval is invented and
   no new approval system is added. Any eventual decision binds resolved scope,
   exact content/version/digest and actual actor/evidence.
5. Only after an authorized decision does Curator use existing issue-backed
   body/index mapping, deduplicate/reconcile writes and read back the actual
   approved record. Only then can capture/retrieval eligibility be established.
6. In a naturally applicable later Mission, fresh scoped retrieval and semantic
   applicability precede actual worker handoff/use. Keep ADOPTED/REJECTED and
   UNASSESSED/EFFECTIVE/INEFFECTIVE meanings; preserve original-Mission feedback,
   exact readback and separate Curator disposition. Do not rerun to manufacture
   reuse/effectiveness.

Source mapping: [MissionLearningSource schema](../../../engcim/swarm/contracts/rc10/MissionLearningSource.schema.json),
[proposal schema](../../../engcim/swarm/contracts/rc10/WorkspaceKnowledgeProposal.schema.json),
[existing lifecycle procedure](../../../engcim/bootstrap/overlays/multica/RC10-WORKSPACE-LEARNING.md),
[Knowledge design §§13, 26.2–26.3, 31.2](../../../engcim/swarm/docs/rc10/RC10-KNOWLEDGE-DESIGN.md),
[existing lifecycle implementation](../../../engcim/swarm/src/main/java/com/featuredeliveryintelligence/fdi/orchestration/SwarmKnowledgeLifecycle.java).

Validation covers existing-schema conformance, source links, draft digests,
public/private separation and recoverable export. It is not live ingestion,
runtime adoption, causal efficacy, acceleration or Human DONE.
