# Package and Runtime Verification Evidence

Verification time: 2026-09-19  
Workspace: `44625a34-7b76-41f1-8ce8-a191b7cf6b46`  
Runtime: `e021607e-b81d-4a52-a755-ef45b8b8dfd6`

## RC6 installation

- `setup.sh` completed successfully.
- Installed skills: 30.
- Installed agents: 19.
- Installed squad: one `Swarm` squad with 18 members plus leader.
- All 19 agents report model `gpt-5.6-luna` and runtime
  `e021607e-b81d-4a52-a755-ef45b8b8dfd6`.
- Skill file tree digest: `131c5069793bc7b305c269b50d699103b92b9ce71ee55d2a88a9f0884cb201df`.
- Agent file tree digest: `0772535c73f7373bdfab450d9bb03a4316652996e22014f11fed095d573e5a22`.

## `verify.sh`

The package verification report is at the external package path:

`/Users/herman_mbp2023/engcim-swarm-rc6-b1-validation-20260919/engcim-swarm-package-RC6/verification-report.md`

Observed result: `PASS=25`, `FAIL=0`, `WAIVED=0`, `NOT-VERIFIED=4`,
`required=0`, `optional-external=4`.

The four `NOT-VERIFIED` items are not promoted:

1. real agent run behavior;
2. TKMS MCP connectivity;
3. Azure DevOps MCP connectivity;
4. dispatch ack observability.

The current host does not provide TKMS/Azure MCP. S01-S06 therefore uses the
frozen local synthetic corpus and reports those external channels as
unavailable/not verified.

## Runtime topology

`multica daemon status` showed one running daemon, PID `70394`, version
`0.4.44`. No second validation daemon was started. Other registered runtimes
remain visible in the host, but the frozen execution path is the Codex runtime
above.

