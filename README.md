# FDI Standalone Project Baseline v0.4.8.3

This is a **self-contained FDI project baseline for Multica**. It fixes the v0.4.8.2 defect where governing IDs existed without the actual Markdown contents.

## First read

1. `PROJECT-OVERVIEW.md`
2. `governance/CURRENT`
3. `governance/approved-source-lock.json`
4. `governance/GOVERNING-SOURCES.md`
5. `STATUS.json`
6. `DEVELOPMENT-BACKLOG.md`
7. `MULTICA-HANDOFF.md`

## Standalone invariant

Every active governing module ID MUST resolve to a local file/directory and matching digest. The project tree enumerates every Markdown file; placeholder-only authority is prohibited.

## Active governing content

- Layer 1 v0.2 semantic contract — local Markdown present.
- Layer 1 Markdown I/O v0.1 — local Markdown present.
- Layer 2 Product Intelligence v0.1 — local Markdown present.
- Product Asset Profile v0.1 (PA-03/PA-05) — local Markdown present.
- PA maintenance contracts v0.1 — local Markdown present.
- FT-T2 HERM-211 locked surface — 6 contract `.md` + 6 schemas + 5 `SKILL.md` + workflow present.

Do not merge content from older recovery/overlay projects into this baseline. Treat `archive/` as non-authoritative history only.

## Runtime build

The active runtime build targets Java 17 and Spring Boot 3.4.1.

```sh
MAVEN_OPTS='-Xmx2g' ./mvnw test
MAVEN_OPTS='-Xmx2g' ./mvnw package
```

The Python files under `scripts/` remain package/governance wrappers; all active runtime implementation is Java.

DEV-204 commands are available through the executable Spring Boot JAR:

```sh
java -jar target/fdi-0.4.8.3.jar dev204-prepare --scenario-pack <file> --output-dir <dir>
java -jar target/fdi-0.4.8.3.jar dev204-evaluate --red <file> --green <file>
```
