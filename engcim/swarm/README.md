# ENGCIM Swarm Module

This portable Maven module contains the Java ENGCIM Swarm implementation,
RC6 baselines, RC10 contracts, tests and delivery evidence. Mission remains an
execution/request instance; this module does not introduce an eighth ENGCIM
component or a new workflow engine.

Build from the repository root:

```sh
MAVEN_OPTS='-Xmx2g' ./mvnw -pl engcim/swarm test
```

Build independently with a Maven installation from this directory:

```sh
MAVEN_OPTS='-Xmx2g' mvn test
```

RC10 delivery reports are under `docs/rc10/`. Run the live Swarm composition
gate from the repository root with the active workspace overlay available:

```sh
engcim/swarm/tooling/verification/verify_swarm_runtime.sh
```

The company import manifest path/hash checks are a Java 17 JUnit test included
in this module's Maven suite. Run it from the repository root with:

```sh
MAVEN_OPTS='-Xmx2g' ./mvnw -pl engcim/swarm -Dtest=CompanyImportManifestTests test
```

The candidate review index is under `release/RC10-CANDIDATE-PACKAGE/`.
