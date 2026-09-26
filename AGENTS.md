<!-- grafel:mcp-usage:start v=2 -->

## grafel MCP

This repo is part of grafel group **fdi-adr001-runtime**. When an AI coding agent needs structural understanding, it should prefer the grafel MCP tools over broad repository searches.

For structural questions such as where a symbol is defined, who calls it, how a request flows, or what a change affects, use the graph queries (`grafel_find`, `grafel_inspect`, `grafel_related`, `grafel_trace`, and `grafel_impact_radius`) when available. Raw text search remains appropriate for comments, TODOs, configuration values, and log strings.

Do not edit between these markers; this block is maintained by `grafel install`.

<!-- grafel:mcp-usage:end -->

## Repository navigation

Read these entry points before making project-level changes:

1. `README.md`
2. `docs/README.md`
3. `docs/FILE-CLASSIFICATION.md`
4. `PROJECT-OVERVIEW.md`
5. `governance/CURRENT`
6. `STATUS.json`

The classification document determines whether a file is governing authority, candidate documentation, executable code, agent procedure, validation evidence, or generated release material. Do not infer authority from filenames, version labels, archives, or old worktrees.

## Java 17 Framework Rule

- Implement all new executable FDI framework behavior in Java 17 with Spring Boot 3.4.1.
- Do not add `.py` or `.pyi` source files, and do not add features to legacy Python wrappers.
- Treat the tracked Python files under `scripts/` and `tests/` as a closed migration baseline. Port remaining active consumers to Java before removing Python sources or Python-only tests.
- Keep external Python providers, including Graphify, behind the Java provider boundary.
- Run `JavaOnlySourcePolicyTests` with the Java 17 test suite before claiming completion.

## Change and evidence boundaries

- Preserve approved governing bytes and exact revision bindings.
- Keep generated files out of source and governance directories.
- Do not claim runtime readiness, adoption, delivery, or closure from a passing structural check alone.
- Verify the exact input revision, artifact, and receipt before reporting a completed gate.
