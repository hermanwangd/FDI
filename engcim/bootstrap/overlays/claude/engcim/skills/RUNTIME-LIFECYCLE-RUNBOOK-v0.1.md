# ENGCIM Runtime Lifecycle Runbook v0.1

**Owner:** Claude Supervisor

ENGCIM Swarm builds/improves runtime packages. Claude Supervisor verifies, activates, validates, and rolls them back.

## R0 — Candidate Preflight

For candidate package:

```sh
PACKAGE="<exact-package-path>"
test -f "$PACKAGE"
```

Compute digest using an available host command:

```sh
sha256sum "$PACKAGE"
```

or:

```sh
shasum -a 256 "$PACKAGE"
```

Inspect archive/package contents read-only before activation.

Record:

```text
candidateRevision
candidatePackageRef
candidatePackageDigest
```

## R1 — Capture Current Qualified Runtime

Before upgrade resolve:

```text
activeRevision
activePackageRef
activePackageDigest
installRoot
activationMethodRef
lastSmokeStatus
```

Only a qualified current runtime may become `lastKnownGood*`.

If exact identity is unknown:

```text
BLOCKED_RUNTIME_IDENTITY
```

## R2 — Resolve Activation Method

Accept only:

```text
A. package-declared installer/activation procedure
B. workspace-documented and already-verified activation procedure
```

Reject:

```text
guessing install destination
blind unzip over active runtime
invented restart command
editing Supervisor assets to simulate a new Swarm runtime
```

If no verified method exists:

```text
BLOCKED_RUNTIME_ACTIVATION_METHOD
```

## R3 — Activate and Verify

Run only the verified activation method.

Then independently re-resolve active runtime identity.

Prove the deployed runtime corresponds to the intended candidate revision/package identity. Installer exit code alone is insufficient.

## R4 — Smoke

Use package/workspace smoke procedure.

At minimum prove:

```text
runtime loadable
Swarm can invoke it
active identity is intended candidate
```

Persist smoke status/time.

## R5 — Re-run Target Case

Upgrade success is not closure.

Re-run the original frozen/replay case and collect fresh evidence.

## R6 — Rollback

Rollback on:

```text
activation failure
identity mismatch
smoke failure
critical regression
```

Restore exact:

```text
lastKnownGoodRevision
lastKnownGoodPackageRef
lastKnownGoodPackageDigest
```

Then independently verify rollback identity and smoke.

Never rollback by an unverified revision label alone.
