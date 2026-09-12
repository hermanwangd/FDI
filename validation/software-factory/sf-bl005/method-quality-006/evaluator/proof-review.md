# Independent METHOD-QUALITY-006 proof review

Reviewer: `/root/calibration_evaluator_review`, separately attributable from producer and gold author. Execution: SF-BL-005-METHOD-QUALITY-006. This is repeated exposed CALIBRATION, not a holdout. The user's desired thresholds did not determine proof admission.

Read-only controls were rechecked at source code candidate `18c9a12d3578b42dbc2052c6d69e881395769f9b` on `codex/sf-bl002-route-aware-correction`. The active Plan authorizes this new immutable namespace. Production generation finished at `2026-09-12T08:48:56.938264Z` before this review. No new producer source implementation was read, edited or rerun. No old artifact, accepted semantics, control, or scoring implementation was changed.

## Seals

- Producer baseline: `14b2f92ff59aecf5d3ee11c6938e55b85d14552f43eb9b2c06c46bf66b29118d`.
- Producer improved: `0d173517285edaa84c1ec78320c0885e118e141638fa0a04e6897a770e7eb9f1`.
- Generation manifest: `9dad9e8c1652068f3dc7416364af47700d4d15827ada2dd1284ddb597310a127`.
- Runtime JAR independently hashed: `1e4a46d527b4c51aa57c952f2656e7cb3eeb69b1d287d4b29ab1fc78d9b0561d`.
- Truth copied byte-for-byte from 005: `39aa27459305c05a4c983b0c414c60b39ec3e460491b9edb5da49c620bc2de2c`.
- Final pre-scoring proofs.json: `48227b451f176cf4b0bb6709db0175275417bea4d9c851e4e068c79f8d85857c`.
- Petclinic: `.fdi-work/sfbl002-petclinic-818c413`, HEAD `818c4136ea971c21674525f9053de0d9c7ad8cfe`, Git status empty.

All five input and all generation-output digests were recomputed and matched. The accepted semantics remain bound to SHA256 `6c854c3d42c348d56720741b573ec88e5d6bd2dc38abb4753540ca23e8aaa9e3`. Both arms use the same input snapshot and executable digest. This review does not independently attest the reported full test totals; it owns source-proof adjudication only.

## Admission standard

Each emitted method and edge was inspected for exact canonical identity, source declaration and call-site support, scenario relevance, and the specific cited evidence. Gold membership does not establish proof. Ledger entries retain original pair/edge and evidenceRef bytes and bind the entire corresponding producer artifact. Multiple distinct references are retained; they do not multiply unique TP.

Static source calls prove source relations only. Inherited source-declared `BaseEntity.isNew()` resolves through Pet -> NamedEntity -> BaseEntity; the declaration is real, but structural relevance does not make it a necessary expected business pair. Repository interface invocations do not prove persistence. A conditional source call can be supported as a structural realization of an accepted alternate branch without claiming that its seed test executed that branch; a branch incompatible with the accepted scenario is rejected.

Redirect-associated detail methods have an independent exact GET test (observation 00019). These establish separate read-side realization associations only. They establish neither browser redirect following nor successful persistence followed by a fresh query. No redirect is credited as a directed call edge. All previously sealed missing-chain availability remains unchanged.

## Baseline independent adjudication

The baseline is the 005 improved producer reexecuted under this JAR: 19 method claims and 9 edges. Its evidence references and source relations were checked again, not accepted from old proof flags. The broad-empty-query seed does not prove matching-surname or whitespace conditions; a generic error seed does not prove successful visit creation or a future upper-date rejection; the unpaged JSON veterinarian endpoint does not establish the batched-directory scenario. Exception-only duplicate helpers do not establish valid create/update behavior. These claims remain omitted from the ledger and present in the precision denominator.

The supported original success seeds remain valid at their exact tests. Explicit pagination/update source calls remain supported as structural relations. This independently yields 12 supported method references and 7 supported edge references. A supported structural method outside the necessary gold set remains FP under the unchanged scorer.

## Improved seed and read-side review

Observation IDs below use prefix `http-behavior-observation-`. Test and source paths are relative to Petclinic, with ordinary package prefixes `src/test/java/org/springframework/samples/petclinic/` and `src/main/java/org/springframework/samples/petclinic/`.

| Evidence | Source/test verification | Disposition |
|---|---|---|
| 00011, 00012 | OwnerControllerTests:152-172 supplies a surname and surrounding-whitespace variants; source OwnerController:95-137 normalizes and queries it. | Admit matching query/normalization handler proofs. |
| 00007 | OwnerControllerTests:115-123 valid create submission; OwnerController:77-87 invokes save and returns detail location. | Admit creation handler and separately GET-evidenced read-side association. |
| 00016 | OwnerControllerTests:212-221 submits modified contact fields; OwnerController:144-162 checks identity, invokes save, redirects to detail. | Admit update handler and independently evidenced detail association. |
| 00017 | OwnerControllerTests:224-227 posts unchanged values. | Omit this reference for modified-contact scenario; another valid reference may support the same pair. |
| 00020 | OwnerControllerTests:260-278 supplies mismatched identity and asserts error plus redirect to edit form. | Omit handler-success and detail-association references: this is an error redirect, not successful update. |
| 00022 | PetControllerTests:98-104 submits valid new pet data; PetController:107-137 associates and submits persistence. | Admit create handler and independently GET-evidenced detail association. |
| 00024, 00027 | PetControllerTests:124-134 and :165-178 assert duplicate field feedback from explicit name conflict and injected persistence conflict. | Admit duplicate create-handler references; mocked exception is not real database evidence. |
| 00031, 00034 | PetControllerTests:218-231 and :257-271 assert duplicate update feedback. | Admit duplicate update-handler references under the same limited boundary. |
| 00029, 00030 | PetControllerTests:192-210 submits valid existing-pet changes, including preserving its own name; source PetController:144-200 selects and mutates existing target. | Admit update handler and independently GET-evidenced detail association. |
| 00036 | VisitControllerTests:76-83 supplies tomorrow and description; VisitController:97-112 invokes association/persistence. | Admit valid visit handler and separately GET-evidenced detail association; no real transaction claim. |
| 00042 | VetControllerTests:83-88 requests page 1 HTML directory, with exact source VetController:44-63. | Admit directory handler. |
| 00019 | OwnerControllerTests:245-256 independently requests owner detail and asserts model fields, pets and visits; OwnerController:169-177 identifies its own lookup boundary. | Admit separate detail realization for valid origin references only. No automatic downstream execution credit. |

## Improved source-call review

The complete qualified-call families in the producer were checked against these exact source bodies; these account for every emitted static method/edge reference:

- OwnerController processFindForm -> addPaginationModel / findPaginatedForOwnersLastName, and the latter -> OwnerRepository.findByLastNameStartingWith: OwnerController:108-137 and OwnerRepository:44. The paginated display branch is a legitimate source branch for multiple-result matching/normalized queries. Singleton-return seed tests do not establish execution of this alternate branch; the ledger credits only structure. The non-necessary pagination claim remains in the scorer's denominator.
- PetController processCreationForm -> BaseEntity.isNew / Owner.getPet(String,boolean) / Owner.addPet: PetController:110-123; Owner:97-110 and :144-154; BaseEntity:52-54. These are actual guard/association calls with source-resolvable receiver and overload identity. The guard calls may be relevant without being necessary gold pairs.
- Owner.addPet -> BaseEntity.isNew: Owner:104-107. This is an actual identity guard for creation. No database execution is implied.
- PetController processUpdateForm -> Owner.getPet(String,boolean) / updatePetDetails: PetController:151 and :168. Existing-name guard and successful target update are structurally appropriate where claimed.
- PetController updatePetDetails -> Owner.getPet(Integer): PetController:187-194. The Integer overload identifies the existing target.
- PetController updatePetDetails -> Owner.addPet and subsequent Owner.addPet -> BaseEntity.isNew in the existing-pet update scenario: rejected. PetController:196-198 is the missing-target fallback creation branch, incompatible with the accepted identified-existing-pet obligation and the cited existing-target tests. Both method and edge references are omitted, including both repeated seed references.
- VetController showVetList -> addPaginationModel / findPaginated, and findPaginated -> VetRepository.findAll(Pageable): VetController:44-63 and VetRepository:55-56. These are exact declared pagination calls; no repository implementation execution is inferred.

No unsupported inherited external repository methods were fabricated. Canonical declaring classes, parameter order, primitive/boxed distinction, erasure and repository-relative source paths were checked. Repeated source calls remain original references in the ledger.

## Completion and limitations

All improved 58 method references (35 unique pairs) and 32 edge references were adjudicated. Ledger admits 50 method references and 28 edge references; eight method references and four edge references are omitted for the documented no-change/error/fallback mismatch reasons. Support counts are not TP counts. Ordinary identity/guard helpers and other non-necessary claims can remain FP even when source-proof support exists; nothing is removed from the proposal denominator.

The immutable ledger is ready for the unchanged METHOD-PAIR-001 scorer using the digest-bound comparison manifest and existing JAR with `-Xmx512m`. No compilation or upstream runtime test is required or performed. Metric achievement is decided by the resulting raw scores; formal experiment remains NOT_RUN and missing gold chain coverage remains unavailable.

## Post-seal scorer result

The unchanged existing JAR ran `method-pair-compare` with `-Xmx512m`, returned exit 0, and created comparison.json SHA256 `bc653aec887167e6c449a72d850c27f86e9537f9269b4685252c569e8b8e3e43` from manifest SHA256 `8170fabb60be1fb078bda92fb0a802744f53e14a63d6dcea0c09f2a5ac811dd8`. No ledger change followed scoring.

| Arm | TP / FP / FN | Precision | Recall | F1 | Scenario coverage | Chain availability |
|---|---|---|---|---|---|---|
| Baseline | 11 / 8 / 29 | 11/19 | 11/40 | 22/59 | 7/10 | 0 complete of 9 defined; 1 missing |
| Improved | 28 / 7 / 12 | 28/35 = 0.80 | 28/40 = 0.70 | 56/75 | 9/10 | 2 complete of 9 defined; 1 missing |

The strict precision >0.80 goal is not met. Recall >0.60 is met. Combined goal remains incomplete; do not round 0.80 upward. Improved has 23 duplicate pair references and one abstention; duplicates did not inflate TP. Missing-chain availability keeps complete-chain coverage undefined, not 2/9 as a published metric. Formal experimentDecision stays NOT_RUN.

At the unique-pair level, five improved false positives have valid structural proof but are outside the frozen necessary business-method set (auxiliary identity/guard/presentation calls). Two are unsupported missing-target fallback/identity-plumbing contamination of an existing-target update. This separates proof validity from necessity; it supplies no missing-gold identities or producer selection instructions. Any new producer increment requires a fresh immutable run and independent adjudication.
