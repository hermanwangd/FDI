# RealWorld rejection analysis 002

## Bound evidence

- Diagnostic: `SF-BL-005-REALWORLD-SELECTOR-DIAGNOSTIC-003`
- Integrated evidence commit: `5ce6bbcac538c0e22086da30f3b646a2a30bb7db`
- Diagnostics SHA-256: `79812bc498697f151a58c4bb442068c8903e212ce2320912f87adad6b056de68`
- Frozen scope: 10 intents × 11 observations, 19 route handlers, 23 public test files
- Access boundary: no evaluator truth, gold, proof, diagnosis, or scorer input was used

## Confirmed classification

| Current reason | Pairs | Defensible gap | Expected immediate accepted gain |
|---|---:|---|---:|
| `ENTITY_MISMATCH` | 73 | Four USER pairs for `ProfileApi#unfollow` expose an owner-class-only taxonomy limitation. The other 69 are supported negative pruning. | 0 |
| `ACTION_MISMATCH` | 25 | Six `REJECT + DELETE` pairs expose a mutation-verb taxonomy gap. Nineteen are supported negative pruning. | 0 without assertion and condition evidence |
| `UNSUPPORTED_ASSERTION_DIALECT` | 12 | Five unique RestAssured MockMvc tests use `given/when/put/then/statusCode`; the selector only recognizes MockMvc `andExpect`. | 1 |

The current Cartesian diagnostic is useful for gate attribution, but rejection-count reduction is not a quality KPI. A change is acceptable only when public source evidence supports it and precision risk remains bounded.

## Ordered remediation

1. `SF-BL-005-RESTASSURED-STATUS-DIALECT-001`: after the existing single-request, literal-route and non-lambda gates, recognize exactly one downstream literal `statusCode(int)` on the same coherent RestAssured MockMvc chain. Restrict roots to supported RestAssured shapes. Classify 2xx positive and 4xx error. Do not infer a specific business condition from generic 4xx. Expected diagnostic transition: unsupported 12→0; accepted 0→1; five assertion-polarity and six unmet-condition results.
2. `SF-BL-005-REJECT-DELETE-ACTION-001`: admit DELETE as a REJECT mutation only with downstream assertion support and condition/guard evidence. Six pairs reach later gates, but only the public non-owner delete test is a candidate for acceptance. Missing-title must not match it.
3. `SF-BL-005-MULTI-ENTITY-PROVENANCE-001`: add explicit, provenance-bound multi-value handler entities; use exact normalized tokens and retain conservative fallback for the old schema. Expected diagnostic transition: ENTITY 73→69 and ACTION +4, accepted unchanged. Route substring inference is prohibited.

## Required gates

Each implementation requires its own exact envelope, TDD, Java 17 full regression, Python regression, frozen diagnostic replay, independent review, and immutable failure evidence. None may change scorer, threshold, gold, frozen observations, formal holdout, or Product truth. After the three bounded changes, calibration may be rerun; company readiness still requires a separately sealed, previously unseen cross-repository formal holdout with precision > 0.8 and recall > 0.6.
