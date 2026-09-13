# Independent METHOD-QUALITY-007 source-proof adjudication

Reviewer: `/root/calibration_evaluator_review`, separately attributable from producer and gold author. Status: LEDGER_SEALED_FOR_UNCHANGED_SCORER. Repeated exposed CALIBRATION; formal experiment NOT_RUN.

Active controls bind this task to METHOD-QUALITY-007. Candidate HEAD verified `4d3584aa1a74987e204b63d3556c18dd58157a9d`. The current Plan/Status and changed Backlog selection were read; AGENTS, Overview and Spec were verified unchanged from the already-read preceding candidate. No producer implementation was read or altered. No old evidence, gold semantics, controls or scoring code was changed. This ledger was freshly selected from the current sealed claims; no prior ledger flags were imported.

## Immutable inputs

- Baseline producer SHA256: `5812efb8b316a604b7298e8f9d0f8fd0d0993d25567c4632c30a8c13b3c23eee`.
- Improved producer SHA256: `32b447696d0ac98490127755225dd3825ebe90a27580f03dddb75f5f5455ba3a`.
- Generation SHA256: `9c501e78be3518da34043c0c1f891ee66f1bd14fe5f63c1615c90b060b3c4372`.
- Existing executable JAR SHA256: `ea9d7f2e440ef2642f7c641201ea0be1c6637b9c02be3fd484b0c3ab5f7306dd`.
- Exact clean Petclinic HEAD: `818c4136ea971c21674525f9053de0d9c7ad8cfe`; source Git status empty.
- Truth byte-identical copy of 005: `39aa27459305c05a4c983b0c414c60b39ec3e460491b9edb5da49c620bc2de2c`.
- Sealed proofs.json SHA256: `aa33ddf36acaaa2a0e39788727ec27066b58aeee7e0a3fe701858c3deb444d5c`.

All five input hashes and every output listed in generation.json were recomputed against the current files and matched. Source and test evidence remain on the exact accepted semantic revision, SHA256 `6c854c3d42c348d56720741b573ec88e5d6bd2dc38abb4753540ca23e8aaa9e3`. Both arms bind identical source, input snapshot and JAR. Generation was closed and outputs sealed before evaluator access. No upstream tests, Maven, Graphify run, or producer regeneration occurred during this review.

## Proof standard

Proof requires exact method identity, scenario relevance and valid cited source/test evidence. Source existence alone does not establish a successful scenario; gold membership is not proof. Original exact evidenceRef strings are preserved, including repeated independent references. Each ledger arm binds its complete producer digest.

Directed calls prove static source relations only. Repository interface calls and mock-backed tests do not prove real persistence. Separate GET evidence for a redirect target proves a separate read-side realization, not browser navigation or fresh database content. No redirect-to-GET association is admitted as a directed execution edge. Structural relevance and membership in the frozen necessary business-method set remain separate: source-valid auxiliary methods can still be FP.

## Current seed adjudication

Observation numbers have prefix `http-behavior-observation-`. Production/test references use standard Petclinic package roots `src/main/java/org/springframework/samples/petclinic/` and `src/test/java/org/springframework/samples/petclinic/`.

| References | Independently checked basis | Decision |
|---|---|---|
| 00011, 00012 | OwnerControllerTests:152-172 uses matching surname and surrounding whitespace; OwnerController:95-137 strips and queries. | Support query/normalization handlers and appropriate source relations. |
| 00007 | OwnerControllerTests:115-123 valid owner submission, OwnerController:77-87. | Support creation handler. |
| 00016 | OwnerControllerTests:212-221 modified contact submission, OwnerController:144-162. | Support update handler. |
| 00017 | OwnerControllerTests:224-227 submits unchanged values. | Reject this reference for the modified-contact obligation. |
| 00020 | OwnerControllerTests:260-278 asserts mismatched identity error and redirect to edit. | Reject successful-update and detail-association references. |
| 00022 | PetControllerTests:98-104 valid new pet; PetController:107-137. | Support creation handler and source-declared guard/association relations. |
| 00024, 00027 | PetControllerTests:124-134 and :165-178 show duplicate feedback by name guard or mocked integrity conflict. | Support duplicate creation handler and relevant source guards; no real database conflict claim. |
| 00031, 00034 | PetControllerTests:218-231 and :257-271 show duplicate update feedback. | Support duplicate update handler and relevant name-selection source relation. |
| 00029, 00030 | PetControllerTests:192-210 valid existing-pet update, including retained own name; PetController:144-200. | Support existing-target update handler, guard, update helper and exact identifier lookup. |
| 00036 | VisitControllerTests:76-83 tomorrow/description success; VisitController:97-112. | Support valid visit handler, without persistence claim. |
| 00042 | VetControllerTests:83-88 HTML page request; VetController:44-63. | Support paginated directory handler and its explicit helpers/query boundary. |
| 00019 | OwnerControllerTests:245-256 independent owner detail GET with owner/pets/visits model assertions; OwnerController:169-177. | Support separate detail realization where a valid origin references it. |

The ten emitted redirect-target references were individually checked. Eight have a relevant success origin plus independent GET 00019. The two using unchanged-submit 00017 or identity-error 00020 are omitted. No same-request downstream execution is inferred from any of them.

## Current source-call adjudication

All 28 improved directed edge references and their associated method references were checked against exact declarations, imports, overloads, and caller bodies. The call families are:

- OwnerController.processFindForm -> addPaginationModel / findPaginatedForOwnersLastName; the latter -> OwnerRepository.findByLastNameStartingWith(String,Pageable), at OwnerController:108-137 and OwnerRepository:44. Pagination is a source-supported alternate multiple-result branch, not an assertion that singleton tests executed it.
- PetController.processCreationForm -> BaseEntity.isNew, Owner.getPet(String,boolean), Owner.addPet; Owner.addPet -> BaseEntity.isNew, at PetController:110-123, Owner:97-110/:144-154, BaseEntity:52-54. The inherited declaring identity is real through Pet/NamedEntity/BaseEntity. Guard and plumbing relevance is not necessary-pair status.
- PetController.processUpdateForm -> Owner.getPet(String,boolean) and updatePetDetails; updatePetDetails -> Owner.getPet(Integer), at PetController:151/:168/:187-194. Parameter types disambiguate the overloads. The current candidate does not emit the absent-target creation fallback relations.
- VetController.showVetList -> addPaginationModel / findPaginated; findPaginated -> VetRepository.findAll(Pageable), at VetController:44-63 and VetRepository:55-56. These are invocation boundaries only.

All these source relations are appropriate under the same structural evidence standard used before scoring. No test-body execution, downstream persistence, unknown external implementation or additional gold method is manufactured. Declaration/path/revision identity was checked independently of producer role labels.

## Baseline fresh adjudication

The baseline contains 19 methods and 9 edges from the expanded old algorithm. All were inspected in their current artifact. Broad empty-query seed 00004 does not prove surname/whitespace conditions; generic error seed 00037 does not prove successful visit creation or the accepted future-upper-bound rejection; unpaged JSON seed 00043 does not prove paginated directory behavior. Exception-only duplicate helpers are inappropriate for valid creation/update. These original claims stay in the denominator but are omitted from proofs. Valid success seeds and the seven relevant static-call references are admitted. This yields 12 method proof references and 7 edge proof references, independently of old support flags.

## Ledger completeness

Improved has 54 method claims representing 33 unique pairs and 28 edge claims. Exactly four method references are omitted (handler and read-side association for each of 00017 and 00020); all remaining 50 method references and all 28 edge references are supported. A pair with a rejected reference can still have an independently valid alternate reference. Unsupported claims are never removed from proposals or denominators.

Gold remains all 40 pairs and ten scenarios, with its original missing-chain definition unchanged. No proof rule was loosened to meet strict recall >0.60 / precision >0.80. Ledger is sealed before invoking the unchanged scorer with the verified existing JAR and `-Xmx512m`. Independent receipt review, formal holdout, publication and parent closure are separate gates.

## Scorer receipt after ledger seal

The unchanged JAR executed successfully with `-Xmx512m` and exit 0. Manifest SHA256: `82bcadffddb64713f116eea6bd4f96800f769f050bc20bf7be7ba412d2aadf38`. Comparison SHA256: `ed363f33f14286f3f6fa66a894d9473bd97d8ddf03f5b22e1fce8f0488d48114`. No proof, truth or producer changes followed scoring.

| Arm | TP / FP / FN | Precision | Recall | F1 | Scenario coverage |
|---|---|---|---|---|---|
| Baseline | 11 / 8 / 29 | 11/19 | 11/40 | 22/59 | 7/10 |
| Improved | 28 / 5 / 12 | 28/33 = 0.8484848484848485 | 28/40 = 0.70 | 56/73 = 0.7671232876712328 | 9/10 |

Both strict metric inequalities pass. Improved has 21 duplicate pair references, one abstention and zero unsupported TYPE claims. These duplicates did not inflate unique TP. All five remaining FP have valid source support but concern non-necessary auxiliary identity/guard/presentation functions; proof support does not redefine the gold set.

Improved has two complete defined chains versus baseline zero; nine definitions exist and one remains missing, so both chainCoverage values remain null. The unavailable mandatory chain metric prevents a formal positive experiment interpretation. Output readiness is SCORING_MECHANICS_ONLY and experimentDecision is NOT_RUN. This receipt establishes the two requested calibration numbers only; separate independent receipt verification is still required by the Plan before the Feature Delivery Plane records metric completion.
