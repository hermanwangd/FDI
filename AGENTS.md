# Repository Agent Instructions

These instructions apply to the entire repository. They provide navigation and execution constraints; governing semantics remain in the linked governing sources.

## Mandatory read order

1. [Repository overview](PROJECT-OVERVIEW.md)
2. [Active governing baseline](governance/CURRENT)
3. [Approved-source lock](governance/locks/approved-source-lock.json)
4. [Governing-source index](governance/GOVERNING-SOURCES.md)
5. [Current readiness status](docs/planning/STATUS.json)
6. [Documentation index](docs/README.md)
7. The applicable public contract, Skill, workflow, validation protocol, and source files for the task

## Runtime and resource limits

- Use Java 17 and Spring Boot 3.4.1 as declared by [the Maven build](pom.xml).
- Run every Maven command with `MAVEN_OPTS='-Xmx2g'`.
- Treat 8 GB as a hard process-memory ceiling. Do not raise Maven or auxiliary-process memory to that ceiling; keep normal Maven heap at 2 GB and use bounded inputs and concurrency.

## Authority and safety boundaries

- Graphify is an implementation behind the provider-neutral `CodeIntelligenceProvider` and `SnapshotBindingAttestor` boundaries. It is rebuildable Structural Intelligence, not authority for Product truth, current Feature truth, Change Surface inclusion, or `SPEC_READY`. Use the [documentation index](docs/README.md) to locate the current provider design and the [project overview](PROJECT-OVERVIEW.md) for the broader authority model.
- Do not edit, reformat, or regenerate governing content unless the task explicitly authorizes a governance change. Preserve governing bytes and verify their locked digests through [the approved-source lock](governance/locks/approved-source-lock.json) and [materialization report](governance/GOVERNING-CONTENT-MATERIALIZATION-REPORT.md).
- Bind source-dependent conclusions to exact immutable revisions. A branch, mutable `HEAD`, remote URL, or provider index label is insufficient where an exact revision is required; follow the [exact-source binding specification](docs/specifications/framework/source-integration/AZURE-REPOS-EXACT-SOURCE-BINDING.md).
- Fail closed on missing, ambiguous, stale, mismatched, oversized, or unverifiable required inputs. Do not infer success, authority, or closure from partial evidence. Follow the applicable contracts and the [structural integration specification](docs/specifications/framework/structural-intelligence/FEATURE-DISCOVERY-INTEGRATION-v0.2.md).
- Keep claims within the evidence actually executed. Local deterministic tests do not prove live provider binding, real Product binding, DEV-204 execution, F001 effectiveness, upstream byte identity, or production readiness. Check [the current status](docs/planning/STATUS.json) and [VERIFICATION-SUMMARY.json](VERIFICATION-SUMMARY.json) before making completion claims.

## Completion verification

Always run the repository-level checks applicable to the changed surface:

```sh
python3 -m pytest tests/test_standalone_governance.py -q
git diff --check
```

When changes touch `src/`, `pom.xml`, `mvnw`, `.mvn/`, or runtime configuration under `config/`, also run:

```sh
MAVEN_OPTS='-Xmx2g' ./mvnw test
MAVEN_OPTS='-Xmx2g' ./mvnw package
```

Also run the relevant validation or packaging verifier when those surfaces change. Report observed results and pre-existing exceptions explicitly; never convert an unexecuted check into a passing claim.
