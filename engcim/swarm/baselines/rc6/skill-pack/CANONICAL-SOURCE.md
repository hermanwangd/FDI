# ENGCIM Swarm RC6 Canonical Source Baseline

Status: `CANONICAL_GIT_SOURCE_BASELINE`

## Identity

- Release: `RC6`
- Canonical Git path: `engcim/skill-packs/rc6/`
- Source archive: `ENGCIM_Swarm_RC6_CuratedOSS_SkillPack_PCAB.zip`
- Source archive SHA-256: `132bb615f9212bfc5a4cbaaa3b90e3c9bfc4d1d921566812b0b650199ad545d3`
- Skill count: 30
- Agent count: 19
- Frozen inventory fingerprint: `1a4de34647b6e449d0929044779518ddcba5af78266d7b351b113fe2348b888d`
- Source acquisition date: 2026-09-20
- Original source path: `/Users/herman_mbp2023/Documents/ENGCIM_Swarm_RC6_CuratedOSS_SkillPack_PCAB.zip`

The complete machine-readable inventory is [SOURCE-MANIFEST.json](SOURCE-MANIFEST.json).

## Authority boundary

This Git directory is the long-term canonical source for the RC6 baseline. The tracked ZIP preserves the complete sealed package byte-for-byte. The extracted `skills/` and `agents/` trees provide a reviewable representation for source changes and role-binding comparisons.

The Multica workspace registry remains the operational consumer, not a second source of truth. The frozen workspace checked against this baseline was:

- workspace ID: `44625a34-7b76-41f1-8ce8-a191b7cf6b46`;
- registry count: 30;
- registry inventory fingerprint: `1a4de34647b6e449d0929044779518ddcba5af78266d7b351b113fe2348b888d`.

`/Users/herman_mbp2023/.codex/skills/` is the global Codex skill directory and is not part of this canonical source.

## Materialization policy

- Every RC6 `skills/<name>/SKILL.md` is Git-reviewable and its SHA-256 is recorded.
- All non-Python files under the package skill tree are Git-reviewable when they are part of the extracted source surface.
- The exact ZIP remains authoritative for the complete package, including package files not materialized as standalone FDI files.
- Python and Python-stub files from the external package are intentionally not extracted into the FDI source tree. This preserves the repository Java 17 framework rule; it does not alter the sealed archive.
- The embedded package-manifest SHA has `UNKNOWN_SCOPE`; the outer archive SHA above is the release identity.

## Change control

A skill-pack change must create a new release/revision directory or an explicitly reviewed manifest revision. It must not edit RC6 in place. The change must update, as one atomic baseline:

1. the sealed archive;
2. `SOURCE-MANIFEST.json`;
3. extracted skill and role files;
4. the workspace parity evidence for the target Multica registry.

Before validation, verify the archive SHA, skill/agent counts, every entrypoint hash, and registry inventory fingerprint. Tracking files in Git does not prove that a runtime loaded them or that runtime gates enforced them.
