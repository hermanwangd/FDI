# S02 — Product Knowledge refresh on RC10-local-candidate-20260927-01

This is a new, current-candidate S02 Mission. It does not inherit verdicts, completion, or candidate identity from historical RC10VAL-20 / RC10-local-candidate-20260926-01. The authoritative input identity record is the attached s02-current-candidate-input-manifest-v2.json. Do not use the original s02-current-candidate-input-manifest.json attachment; it is invalid.

## Superseding identity correction

The original input-manifest attachment 01a0dfb8-5fb6-7067-99f5-842590e2e29a (SHA-256 9cda72f3690f4627ca28372a871e6c0fdbdc1b4dc7a382313bbb7b697477bab6) contains a 37-character applicableProjectRefs value and is INVALID. Use only attached manifest revision 2, which records fresh byte-exact equality between this Mission's 36-character project ID and RC10VAL-46's applicableProjectRefs. This issue remains unassigned; no run has started.

## Mission scope

Using the attached S01 candidate ProductKnowledge archive (SHA-256 4c4125269788178dba6a605952761cba94b4deeb1d50d10dcfd382cadab8b867), create one revisioned candidate refresh from the attached synthetic source v1 and v2. Preserve prior entries and provenance. Produce a RefreshedProductKnowledgeWithDelta report and a resulting candidate archive with hashes and validator output.

Before the Curator starts, the Supervisor will fresh-read this Mission's project ID and RC10VAL-46, create an AUTHORIZED_VISIBLE_CONTEXT receipt, and attach it to this Mission. The Curator must re-read the record and confirm applicability immediately before use. Use its workspace-procedure guidance as read-only context; it is not ProductKnowledge and cannot decide product truth. Show from the actual Curator run input/output how that context changed attribution, boundaries, or dispositions. Do not claim use from receipt attachment, configured settings, or self-report alone.

## Required S02 assertions

- v2 supersedes v1; preserve v1 as stale/superseded history and bind both source digests.
- Keep S01 facts and source evidence. Report additions, changes, retained claims, supersessions, and unresolved conflicts separately.
- Keep provisional synthetic v2 distinct from verified code behavior. Preserve the known chartLimits().max 10-vs-1000 discrepancy and expected openSelectedChart versus exported selectChart gap. RC10VAL-8 and RC10VAL-9 remain unadjudicated and unchanged.
- The fixture source stays pinned at 2eff5f9f84ca709684bfe0b7c90102268f07a0f0; do not edit, repair, or run code as part of S02. A ProductKnowledge validator result is not a software PASS.
- Demonstrate replay/idempotence against an isolated copy or deterministic replay report: the same v1/v2 digests must not create a duplicate logical entry, erase source history, resolve conflicts, or silently change a governance status.
- RC10VAL-22 is already a provisional ProductKB v2 item. Fresh-read it, preserve its current content/status and avoid duplicate or in-place edits. Deliver this S02 candidate result as a versioned artifact on this validation Mission unless an exact documented reason requires an additional scoped operation; never promote provisional knowledge.
- Do not modify RC10VAL-46, ProductKB truth records, RC10VAL-8/9, source repositories, role instructions, RC6 bytes, or production state. Do not start S03 from this Mission.

## Swarm and safety bounds

Use the existing local Codex CLI Supervisor, current S05-adopted role configuration, and existing Multica squad/runtime only. Every workspace-scoped command must pass --workspace-id 0b02adb6-a395-46bd-bd92-6fec14dee20e; fresh-read returned workspace/project identities. At most two active Swarm runs including the parent and one child at a time; finish/fan-in each actual result before dispatching the next. Sequence Curator → independent Reviewer → independent Verifier. Maximum eight required children, two correction revisions, 45 minutes for this Scenario and 20 minutes per child, process memory below 8 GiB. On a bound breach, stop new dispatch and report evidence; do not cancel in-flight work or silently extend the clock.

Bind every WorkItemResult, VerificationResult, and ControlResult separately to its exact issue/revision, run, source/input/output hashes and current candidate identity. Keep all issues in in_review for Human review. Do not claim Human DONE, RC10 S02 PASS before independent gates, runtime instruction consumption without run-bound evidence, or any production/release readiness.
