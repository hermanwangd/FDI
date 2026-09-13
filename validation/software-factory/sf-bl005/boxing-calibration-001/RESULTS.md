# Boxing calibration 001 — no measured improvement

Same sealed Petclinic inputs, source revision and gold as 007. Opt-in source
boxing candidate: `78465e6efaec0cf2a9005072e05735a1c706e915`; combined Java
candidate with evaluator: `4b52e4366f4302bd26c1b454c76cf8831ecc3006`.
Full Java 17 verification: 1420 tests, zero failures/errors/skips.

Improved TP/FP/FN = **28/5/12**, precision **0.8484848484848485**, recall
**0.7**, F1 **0.7671232876712328**. Delta versus 007: zero. Proposal content
exactly matches 007; runtime bindings differ. Candidate pairs 59, gold
candidates 28, candidate precision 0.4745762711864407, candidate recall 0.7,
retention 1.0. Trace-only UNKNOWN remains 12. No improvement claim.

Independent evaluator `/root/trace_review` verified proposal/reference parity
and rebound all four unchanged proof-ledger arrays to the current artifact
digests. Same frozen truth; official scorer policy unchanged. Independent
reviewer `/root/evaluator_review` recomputed counts and metrics without rerunning
the scorer, checked all declared input/output hashes and retained runtime,
and returned PASS for this bounded negative result. Parent closure, formal GO
and holdout readiness are not established. Chain definition remains incomplete.

## Attempts and limitations

Each evaluator command had two attempts, not exactly one: initial prepared
proof JSON contained tool-output truncation text and was rejected before output;
corrected fresh `proofs-002.json` / `comparison-manifest-002.json` succeeded.
Failed inputs remain preserved. The evaluator recorded exit outcomes; the
independent reviewer verified invalid first-input bytes and fail-before-write
control flow, but did not independently reconstruct historical process exits.
See evaluator/adjudication-receipt.json. No truth or old results were changed.

## Digests

- Runtime: `b10ff6376ec0285b5cddeef284136e6e445173fe09bdccec6d63d0782dfe9dcb`
- Generation: `806f8814b5b4f5c0c6536d520980ecfb8577e419f008a5bf463d7b64637c4936`
- Successful manifest: `fa67470061d4d4e02b54b629a035debfdb8e874364361f02a142d62c4d807511`
- Comparison: `db58434d388da45151a944c75014ed620ae0b711fa2b66eab4f64c7a748d4b38`
- Stage aggregate: `b7dbe956dc857df7379f67af7318d7a5ab84e6a1ef9784db52398524b19a6a27`
- Adjudication receipt: `db738b6633f7d5bb6eb8cddf9c524ae7bc89494b42aa8233a88cc796a48edc82`

Exact JAR is retained at
`/Users/herman_mbp2023/ClawProjects/skills/Software-Factory/.fdi-work/retained-runtimes/candidate-improvements/b10ff6376ec0285b5cddeef284136e6e445173fe09bdccec6d63d0782dfe9dcb.jar`.

## Follow-up

Public source inspection found a source ancestor implementing the JDK marker
interface Serializable. Java 17 javap confirms that interface has no methods.
BOXING-001 conservatively treats all non-source ancestors as unknown. The
separately selected BOXING-002 verifies bootstrap-JDK method inventories instead
of assuming third-party ancestry. It requires its own new run and comparison.

Overload design reference: [JLS 17, method invocation phases](https://docs.oracle.com/javase/specs/jls/se17/html/jls-15.html#jls-15.12.2).
Strict invocation precedes boxing; competing widening overloads and unknown
hierarchies remain unresolved, not guessed.
