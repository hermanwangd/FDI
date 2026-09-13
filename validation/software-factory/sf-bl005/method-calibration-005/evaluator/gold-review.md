# Independent evaluator gold review

Reviewer run: `/root/calibration_evaluator_review`.
Author run: `/root/calibration_gold`.
Execution: `SF-BL-005-METHOD-CALIBRATION-005`.
Review sealed at: `2026-09-12T07:41:54Z`, before authorized producer generation.
Disposition: `PASS_FOR_SEALED_CALIBRATION_WITH_DOCUMENTED_AVAILABILITY_LIMIT`.
Formal holdout: `NOT_RUN`. This review grants no experimental GO or Product publication.

## Exact reviewed inputs

- Framework checkout HEAD: `1c8706982cbda6513b5a8ad90c94b53d5322d884`; current selected controls and AGENTS read, including the co-delivered calibration selection.
- `truth.json` SHA256: `39aa27459305c05a4c983b0c414c60b39ec3e460491b9edb5da49c620bc2de2c`.
- `gold-authoring.md` SHA256: `33a6f29d26d1fcf2cb36db6bb66671dcf467a9bf131dff7903edb28857b71b28`.
- Accepted semantics: `validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/accepted-semantics-004.json`, SHA256 `6c854c3d42c348d56720741b573ec88e5d6bd2dc38abb4753540ca23e8aaa9e3`.
- Petclinic checkout `.fdi-work/sfbl002-petclinic-818c413`: HEAD `818c4136ea971c21674525f9053de0d9c7ad8cfe`; `git status --porcelain` empty.

## Independent checks

All declared production methods were compared directly with the exact source declarations, package/import resolution, parameter order, primitive versus boxed types, and erased generic types. The overload distinctions and canonical repository-relative paths are correct. Each directed edge corresponds to an explicit call in the declared caller body. Repository declarations identify invocation boundaries, without asserting database execution.

Accepted scenario membership exactly matches the semantic input. Read-only JSON checks passed for expected-pair uniqueness, complete equality between each defined chain's method set and its scenario pairs, edge endpoint membership, edge scenario identity, exact source revision, and source-file existence. Source inspection covered OwnerController, PetController, VisitController, Owner, Pet, Visit, OwnerRepository, VetController, Vet, and VetRepository. Corresponding controller tests and the veterinarian template were independently inspected.

The authored minimal business realization cut is supportable: business query/pagination, target selection, mutation/association, explicit duplicate checks, and separately required detail presentation are represented. Routine accessors, framework lifecycle binding, inherited external persistence implementations, and generic validation plumbing are not fabricated as additional obligations. Specialty projection and empty-specialty handling are specifically evidenced in `templates/vets/vetList.html` and the Vet methods. The cut is not the transitive closure of every called method.

Separate detail requests remain separate components; no redirect establishes a directed call or observed downstream execution. Mock-backed controller tests establish behavioral evidence within their test boundary. They do not establish a persisted transaction followed by a successful fresh query. The author accurately limits the chains to source realization subgraphs and does not claim upstream test execution, a new Graphify run, or an end-to-end transaction.

## Required availability limit

The absence of `HYP-SCENARIO-008` pairs and chain is justified. Its accepted semantic text requires rejection beyond an allowed future-date range. Exact `VisitController.java:100` checks that a date is after today and supplies no future upper bound. `VisitControllerTests.java` accepts tomorrow and rejects today, which cannot establish the accepted upper-future-bound behavior. `Visit.java` adds no such validation. The scenario remains selected; no substitute past-date obligation or empty-chain success is permitted.

Consequently mandatory complete-chain availability is insufficient for an overall positive comparison decision. The scorer must retain missing-chain reporting and the resulting INCONCLUSIVE limit under the frozen contract. Individual available quality measurements may be reported with this explicit limit after valid candidate-bound proof review. This is an evidence availability finding, not permission to amend accepted semantics or tune a producer against gold.

## Independence and next gate

The reviewer is distinct from the gold author and producer author. No new producer implementation, generated producer proposal, old evaluator gold, or prior proposal output was read. Only this review file was authored. No Maven, upstream test run, runtime install, commit, or implementation change was performed. Review used bounded file reads, shell digest/Git checks, jq, and a read-only Node JSON consistency check.

Gold authoring requires no remediation. Both reviewed files remain unchanged at their exact seals. Producer generation must complete and both candidate outputs must be sealed before this reviewer receives proposals for a separate, exact proposal-digest-bound source-proof adjudication. That later gate is pending and is not satisfied by this gold review.
