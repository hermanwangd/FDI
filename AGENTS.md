# Repository Agent Instructions

These instructions apply to the entire repository. They provide navigation and execution constraints; they do not define Product truth.

## Mandatory read order

1. [Repository overview](PROJECT-OVERVIEW.md)
2. [Framework specification](FRAMEWORK-SPEC.md)
3. [Implementation plan](IMPLEMENTATION-PLAN.md)
4. [Current status](STATUS.json)
5. The applicable source, contract, configuration, validation, and test files

## Runtime and resource limits

- Use Java 17 and Spring Boot 3.4.1 as declared by [the Maven build](pom.xml).
- Run every Maven command with `MAVEN_OPTS='-Xmx2g'`.
- Treat 8 GB as a hard process-memory ceiling. Do not raise Maven or auxiliary-process memory to that ceiling; keep normal Maven heap at 2 GB and use bounded inputs and concurrency.

## Prototype and safety boundaries

- Graphify remains behind `CodeIntelligenceProvider` and the Graphify adapter. Discover the installed runtime interface; do not assume provider-native operations.
- Bind source-dependent conclusions to an exact immutable Git revision and frozen source snapshot.
- Product Team owns Product meaning. Graphify supplies structural observations and history supplies evidence; neither publishes Product truth.
- Reverse results remain proposal-only until human review.
- Everything under `archive/` is historical reference and must not determine current project truth.

## Completion verification

Always run the repository-level checks applicable to the changed surface:

```sh
python3 -m pytest -q
git diff --check
```

When changes touch `src/`, `pom.xml`, `mvnw`, `.mvn/`, or runtime configuration under `config/`, also run:

```sh
MAVEN_OPTS='-Xmx2g' ./mvnw test
MAVEN_OPTS='-Xmx2g' ./mvnw package
```

Also run the relevant PKB-001 validation when those surfaces change. Report observed results and pre-existing exceptions explicitly; never convert an unexecuted check into a passing claim.
