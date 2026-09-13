# Independent ingress and Graphify bridge follow-up

Reviewer actor/run: `/root/calibration_code_review`, separate from producer and
integrator. Base: `deec2512293550c233afcf4d6eeed6e00acd7819`.
Disposition: **REVISE — path-containment remediation required before final readiness**.
This receipt supplements, and does not modify, ingress-review.md.

## Previous findings

The public cross-repository generator now verifies the exact five allowed paths,
full digests, bounded regular files and symbolic-link ancestry. Default callers
use the private bound implementation. Runtime repository/revision and graph
SHA256 are compared to selected ingress values. These changes address both
previous findings without altering matching algorithms.

The Plan now explicitly owns validation/liveverifier/CrossRepositoryGraphifyEvidence.java.
The inspected bridge stages digest-verified Java files, invokes external Graphify
extract/build/export, then makes a live get_node call through the existing stdio
client, Graphify adapter and provider API. It verifies source/graph/Python hashes
and clean Git state around processing, preserving failed output namespaces.
This review inspects the implementation; it does not claim independent live
runtime execution or successful indexing/scoring.

## Remaining findings

1. **P1 — source-root ancestor symlinks bypass source containment.**
   `CrossRepositoryGraphifyEvidence.sourceDigests` begins walking
   `source/src/main/java` and `source/src/test/java`. If `src` or `src/main` is a
   symlink, filesystem resolution follows it before walking; the loop never sees
   that ancestor and therefore never rejects it. `stageSource` checks only the
   final input leaf. A clean Git checkout containing such a symlink can therefore
   produce EXACTLY_BOUND evidence from external Java bytes. Check every component
   from the canonical checkout through each source root and each staged input;
   add an ancestor-symlink negative case, not only a leaf-symlink case.

2. **P1 — output containment compares lexical paths.**
   `main` normalizes output but does not canonicalize its existing parent before
   `output.startsWith(source)`. An external directory symlink pointing into the
   source lets `/external-link/new-output` pass the guard and write staged files
   and provider caches into the original source. A later dirty-check failure
   occurs after mutation; ignored destinations may not dirty Git at all. Resolve
   the existing output parent before containment checks, or reject symlink
   ancestors, before creating any output. Test a symlink into the checkout and
   verify rejection leaves the checkout untouched.

## Reviewed bindings and limits

SHA256 of inspected production bytes:

| File (repository relative) | SHA256 |
|---|---|
| src/main/java/com/featuredeliveryintelligence/fdi/validation/liveverifier/CrossRepositoryGraphifyEvidence.java | `8502a1f5197a8577e236302a5214f4bddfc06360363d86f324e5e7d7314ff453` |
| src/main/java/com/featuredeliveryintelligence/fdi/product/realization/scenarioforward/SfBl002RouteEffectivenessRun.java | `c63005a56878eb49513a6252621949fc361713859d857d7db63a4a7ddb2e4510` |

The checkout is evolving. Final approval needs the remediated candidate bound to
review and regression evidence, plus authentic selected-source Graphify artifacts
before real scoring. Live provider success cannot establish Product truth or
observed business execution. No Maven, raw evaluator gold, provider invocation,
code edits or control edits were performed by this review. Only this new receipt
was written; the prior receipt and all other actors' files remain unchanged.
