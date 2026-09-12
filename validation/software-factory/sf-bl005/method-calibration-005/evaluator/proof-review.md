# Independent sealed-candidate source-proof adjudication

Reviewer run: `/root/calibration_evaluator_review`, separate from producer author and gold author.
Execution: `SF-BL-005-METHOD-CALIBRATION-005`.
Disposition: `READY_FOR_DIGEST_BOUND_CALIBRATION_SCORING`.
Generation completed at `2026-09-12T07:56:41.221842Z`; this adjudication began after sealed outputs were delivered. No producer implementation was read or modified. No gold, accepted semantics, controls, or producer artifacts were changed. No upstream tests, Maven, Graphify, or runtime generation were executed by this reviewer.

## Immutable bindings

- Baseline producer SHA256: `e79bd365a8b14ae181219b13bbca43624c49fcbb4ccebfe455afe1a4bc49c786`.
- Improved producer SHA256: `03e885ec096e278cf85314ac6a46f294a6e233264cb73e3b094a16d1d6b035d9`.
- Generation manifest SHA256: `7eb63762fb58d3e0599a0e10410da740613d36b7da73cdf15448ffa97de15961`.
- Source revision: `818c4136ea971c21674525f9053de0d9c7ad8cfe`, verified again with empty source-checkout Git status.
- Input snapshot digest: `41a7f9fc60bf9f0de938a5c2f4fd29e33f8258a060d0826f4240baa180e6fa37`.
- Extractor digest recorded identically in both producers and generation manifest: `2fb7fccb31523f2f95b5c79af1bd9e191ebfef3a2b155b00e4eee59de713b762`.
- Gold SHA256: `39aa27459305c05a4c983b0c414c60b39ec3e460491b9edb5da49c620bc2de2c`; prior independent gold review SHA256: `01821cec35390bceecf568e051becb8062d1266e5a9139443b5b8b8886ff983a`.
- Accepted semantic SHA256 remains `6c854c3d42c348d56720741b573ec88e5d6bd2dc38abb4753540ca23e8aaa9e3`.

Every input and output file digest listed in generation.json was independently recomputed and matched, including the seed observations, route-handler index, and seed proposal. This review authenticates source proofs, not independent reproduction of the recorded executable run. Ledger entries retain the original exact evidenceRef; each arm is bound to its entire serialized producer artifact digest, as required by MethodPairData.Ledger.

## Adjudication rule

Method identity, accepted scenario relevance, and the actual cited evidence were reviewed separately. Gold membership alone cannot establish proof. A seed reference is evidence for its particular test and route; it is not permission to substitute another test or infer a successful branch that the cited test does not exercise. A static-call reference can establish an actual directed source relation appropriate to the scenario. It does not establish that a test or real request executed that relation, or that a downstream database transaction succeeded.

The ledger grants only this bounded source/behavioral support. It grants no persisted-transaction, redirect-following, mock-to-real-database, Graphify-runtime, or upstream-test execution credit. Its edge entries must be described as supported source relations, not observed runtime calls. The frozen scorer separately decides whether supported claims are necessary expected pairs. Thus proof support and gold membership are intentionally not identical.

## Supported seed claims, identical in both arms

All paths below are relative to exact Petclinic revision above.

| Scenario | Evidence reference suffix | Source and test basis | Bounded conclusion |
|---|---|---|---|
| HYP-SCENARIO-003 | 00007 | owner/OwnerController.java:77-87; owner/OwnerControllerTests.java:115-123 | Valid owner form reaches the creation handler and its explicit persistence invocation boundary. Redirect assertion is not subsequent lookup proof. |
| HYP-SCENARIO-004 | 00016 | owner/OwnerController.java:144-162; owner/OwnerControllerTests.java:212-221 | Identified owner update handler preserves the requested identity and invokes persistence; no fresh-read transaction claim. |
| HYP-SCENARIO-005 | 00022 | owner/PetController.java:107-137; owner/PetControllerTests.java:98-104 | Valid pet submission belongs to the owner's creation handler; downstream storage and later display are not established by the redirect. |
| HYP-SCENARIO-011 | 00029 | owner/PetController.java:144-200; owner/PetControllerTests.java:192-198 | Valid existing-pet update request maps to the update handler. |
| HYP-SCENARIO-009 | 00042 | vet/VetController.java:44-63; vet/VetControllerTests.java:83-88 | HTML page request maps to the paginated veterinarian directory handler. |

Production prefixes are `src/main/java/org/springframework/samples/petclinic/`; test prefixes are `src/test/java/org/springframework/samples/petclinic/`. Full canonical signatures and full original seed references are retained in proofs.json. Route signatures and parameter qualification were crosschecked against source, not accepted from the seed index alone.

## Supported improved static-call claims

- HYP-SCENARIO-001 and HYP-SCENARIO-002: `OwnerController.processFindForm` explicitly calls `findPaginatedForOwnersLastName` and `addPaginationModel` at OwnerController.java:108 and :121. The first consumes the normalized surname and the second presents matching multiple results. Both are relevant source relations for these scenarios. Their receiver identities are unambiguous within the same class. Supporting the latter for 002 does not add it to the sealed necessary gold denominator; it remains subject to the scorer's independent expected-pair check.
- HYP-SCENARIO-011: `PetController.processUpdateForm` explicitly calls `updatePetDetails` at PetController.java:168. Its valid branch transfers the existing pet's submitted properties, with the exact Owner/Pet descriptor independently verified. This supports the proposed method and directed structural edge, not proof of durable database mutation.
- HYP-SCENARIO-009: `VetController.showVetList` explicitly calls `findPaginated` and `addPaginationModel` at VetController.java:45-46. These source relations implement page selection and model presentation; the helpers have canonical int/Model/erased Page descriptors matching their declarations.

There are seven supported improved static method claims and seven supported directed source edges. Their source relation can be validated independently even when another proposed seed evidenceRef for the caller is insufficient for its claimed scenario. A chain cannot become complete from these edges alone: the scorer still requires every necessary method to have its own valid proof.

## Unsupported claims retained outside the ledger

| Arm | Scenario / claim | Reason |
|---|---|---|
| Both | 001 processFindForm, seed 00004 | PetClinicIntegrationTests.ownerList at :62-65 submits `lastName=` and checks HTTP 200 only. This proves the broad empty-query route, not the accepted matching-surname behavior. The separately present surname controller tests cannot silently replace the claim's cited test. |
| Both | 002 processFindForm, seed 00004 | The same test contains no surrounding whitespace. Correct handler identity and normalization source do not convert this exact seed reference into evidence for whitespace normalization. |
| Both | 007 processNewVisitForm, seed 00037 | VisitControllerTests.processNewVisitFormHasErrors at :87-93 omits required data and asserts errors/form redisplay. It does not establish the valid visit-creation branch. The success test elsewhere is not the cited evidence. |
| Both | 008 processNewVisitForm, seed 00037 | The cited generic validation-error test does not exercise an upper-future-date rejection. Exact source has no such bound; the independently sealed gold availability limitation remains. |
| Both | 009 showResourcesVetList, seed 00043 | VetControllerTests.showResourcesVetList at :93-98 requests the unpaged JSON route and checks an identifier. Source uses the zero-argument repository overload; this does not realize the accepted batched-directory scenario. |
| Improved | 005 and 011 isDuplicatePetNameViolation, plus corresponding edges | PetController.java:125-130, :170-175 and :203-205 place this helper only in duplicate-integrity exception handling. The selected scenarios concern valid creation/update, and the cited successful seed tests do not throw that exception. A real call site exists, but it is not proof of the selected successful scenario path. |

Unsupported method claims are not deleted from proposals or precision denominators. Expected methods with insufficient evidence remain eligible for both FP and FN under the frozen scorer. The unresolved scenario remains an abstention. No proposal identities, gold, or scores were adjusted to compensate.

## Ledger completeness and next gate

Baseline: ten method claims inspected; five supported method proofs, zero proposed or supported edges. Improved: nineteen method claims and nine edges inspected; twelve supported method proofs and seven supported source-edge proofs. These are proof-adjudication counts, not quality scores; one supported claim is outside the necessary gold set.

proofs.json contains only original claims from the corresponding exact artifact, without duplicate entries or cross-arm substitution. JSON shape matches the public serialization records. Formal experiment remains `NOT_RUN`; this exposed dataset remains `CALIBRATION`. Missing gold chain availability prevents a positive overall experimental conclusion. The next step is the unchanged digest-bound scorer; no producer tuning or rerun is authorized by this review.
