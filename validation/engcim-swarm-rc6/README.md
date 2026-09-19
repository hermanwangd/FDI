# ENGCIM Swarm RC6 B1 Real Multica Benchmark

Date: 2026-09-19

Final classification: `RC6 B1 VALIDATED WITH CONDITIONS`. The PC1 paired run and stale-context safety challenge completed, but the Scenario chain is not treated as fully validated because required acceptance contracts remain blocked or partial. The autonomous fan-in control is independently `PASS`.

This directory is the only FDI output location for this B1 run. The frozen RC5 validation directory and FDI product baseline were not modified.

## Experimental boundary

- `B0`: frozen RC5 Scenario and autonomous fan-in results.
- `B1-PC0`: RC6 Curated OSS skills with the exact raw RC5 corpus and no composed Product Context.
- `B1-PC1`: same RC6 runtime/model, fixtures, revisions, defects, and criteria, with the frozen Product Context artifact.
- TKMS/Azure MCP channels were unavailable in this environment and are reported as unverified, never as PASS.

## Key evidence

- Package SHA-256: `132bb615f9212bfc5a4cbaaa3b90e3c9bfc4d1d921566812b0b650199ad545d3`
- Workspace: `ENGCIM Swarm RC6 B1 Test 20260919`
- Workspace ID: `d91054ef-1b00-4011-b81f-a6e82cb0e7f7`
- Workspace slug: `engcim-swarm-rc6-b1-test-20260919`
- Issue prefix: `E6B`
- Codex runtime: `f0c0777e-7248-4330-a923-9e453fc751c3`
- Runtime provider/model: Codex / `gpt-5.6-luna`
- Multica daemon: `01a01a54-cb97-7a4c-b7eb-e620cae7890`, PID `3253`, profile `desktop-api.multica.ai`
- Scenario counts: PC0 `1 PASS / 5 PARTIAL / 4 BLOCKED / 0 FAIL`; PC1 paired S04–S10 `1 PASS / 2 PARTIAL / 4 BLOCKED / 0 FAIL`
- Patched package SHA-256: `64b214609c3847130ec2e8eac96fc8e04709ed9df3549aaf8ddc74120ff62e8a`

See the linked reports for run IDs, attachments, artifact revisions, and classifications.
