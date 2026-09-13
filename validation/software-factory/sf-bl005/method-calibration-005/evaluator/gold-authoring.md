# Evaluator-only calibration truth authoring

Author run: /root/calibration_gold (separately attributable delegated evaluator author).
Execution: SF-BL-005-METHOD-CALIBRATION-005.
Status: SEALED_AUTHOR_OUTPUT_PENDING_INDEPENDENT_REVIEW.
Dataset: exposed CALIBRATION; formal holdout NOT_RUN.

Authority was read from AGENTS.md and the five active controls. This evidence
does not change Product meaning, active controls, or scoring semantics.
Only the two assigned evaluator files were authored. No producer proposals,
new methodcalibration implementation, or prior evaluator gold were read.
The public MethodPairData serialization records and signature validation were
read solely for compatibility. No Maven, upstream tests, Graphify or runtime
execution was performed.

## Source and accepted input binding

Petclinic checkout .fdi-work/sfbl002-petclinic-818c413 was verified clean at
818c4136ea971c21674525f9053de0d9c7ad8cfe before authoring.
Accepted semantic input:
validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/accepted-semantics-004.json
SHA256 6c854c3d42c348d56720741b573ec88e5d6bd2dc38abb4753540ca23e8aaa9e3.
The exact hash, not the filename suffix, identifies this input.
Accepted retrieval aids and their acceptance manifest were read at
validation/software-factory/sf-bl002/accepted-scenario-search-intents-002.json
and scenario-search-intent-acceptance-manifest-002.json. Their shared semantic
digest and source revision agree. Retrieval-aid acceptance is not semantic
publication.

## Denominator construction

Necessary source-declared business methods implement the accepted scenario's
observable obligation: selected handler, business transformation/selection,
explicit repository query boundary, and separately required detail display.
This is a minimal realization cut, not every reachable Java accessor or MVC
lifecycle hook. Trivial field accessors, constructors, framework binding hooks,
supporting generic validation and setup-form methods are not independently
necessary business obligations. Specialty accessors are included because they
directly implement the explicitly accepted presentation of available specialties.
Inherited save/saveAndFlush have no declarations in this source revision:
they are documented persistence boundaries, not fabricated repository methods.
A source-declared repository interface method represents an invocation boundary,
never evidence of database execution.

Each chain contains all selected necessary methods, including separate request
components, and only actual directed source calls between them. No redirect
edge, template-dispatch edge, repository-to-entity runtime edge, or mock call
is invented. Consequently these are structural realization subgraphs, not
claims of fully observed successful transactions. Tests were inspected as
behavioral evidence only; mocked repositories do not establish persistence.
No end-to-end runtime completeness is asserted.

Signatures use declaring qualified class, #method and erased qualified parameter
types; primitive int/boolean are preserved and Integer is java.lang.Integer.
Page<T> is canonicalized to org.springframework.data.domain.Page.

## Pair-by-pair source justification

Paths beginning owner/ or vet/ below are relative to
src/main/java/org/springframework/samples/petclinic/ for production and
src/test/java/org/springframework/samples/petclinic/ for tests.
Every reference is at the exact Petclinic revision above.

### HYP-SCENARIO-001

- `org.springframework.samples.petclinic.owner.OwnerController#processFindForm(int,org.springframework.samples.petclinic.owner.Owner,org.springframework.validation.BindingResult,org.springframework.ui.Model)`: Normalizes query, chooses single-result redirect versus paged list, and rejects no-result searches. Production owner/OwnerController.java:95-122; behavioral support owner/OwnerControllerTests.java:145-194.
- `org.springframework.samples.petclinic.owner.OwnerController#findPaginatedForOwnersLastName(int,java.lang.String)`: Creates the bounded page request and forwards the normalized surname. Production owner/OwnerController.java:133-137; behavioral support owner/OwnerControllerTests.java:145-183.
- `org.springframework.samples.petclinic.owner.OwnerController#addPaginationModel(int,org.springframework.ui.Model,org.springframework.data.domain.Page)`: Publishes the owner collection and page metadata for multi-result browsing. Production owner/OwnerController.java:124-131; behavioral support owner/OwnerControllerTests.java:145-148.
- `org.springframework.samples.petclinic.owner.OwnerRepository#findByLastNameStartingWith(java.lang.String,org.springframework.data.domain.Pageable)`: Source-declared repository query boundary accepts the normalized surname and pagination; mock interaction supports argument behavior, not database execution. Production owner/OwnerRepository.java:38-45; behavioral support owner/OwnerControllerTests.java:163-183.
- `org.springframework.samples.petclinic.owner.OwnerController#showOwner(int)`: Independently serves the detail view required for initial selection or subsequent inspection. This is a separate request, never a claimed call from a redirecting handler. Production owner/OwnerController.java:169-177; behavioral support owner/OwnerControllerTests.java:245-256.
- `org.springframework.samples.petclinic.owner.OwnerRepository#findById(java.lang.Integer)`: Declared owner lookup boundary used by the detail handler; repository runtime is mocked in the test. Production owner/OwnerRepository.java:48-60; behavioral support owner/OwnerControllerTests.java:245-256.

Directed edges (each arrow corresponds to the caller's exact source body above):

- `org.springframework.samples.petclinic.owner.OwnerController#processFindForm(int,org.springframework.samples.petclinic.owner.Owner,org.springframework.validation.BindingResult,org.springframework.ui.Model)` → `org.springframework.samples.petclinic.owner.OwnerController#findPaginatedForOwnersLastName(int,java.lang.String)`.
- `org.springframework.samples.petclinic.owner.OwnerController#findPaginatedForOwnersLastName(int,java.lang.String)` → `org.springframework.samples.petclinic.owner.OwnerRepository#findByLastNameStartingWith(java.lang.String,org.springframework.data.domain.Pageable)`.
- `org.springframework.samples.petclinic.owner.OwnerController#processFindForm(int,org.springframework.samples.petclinic.owner.Owner,org.springframework.validation.BindingResult,org.springframework.ui.Model)` → `org.springframework.samples.petclinic.owner.OwnerController#addPaginationModel(int,org.springframework.ui.Model,org.springframework.data.domain.Page)`.
- `org.springframework.samples.petclinic.owner.OwnerController#showOwner(int)` → `org.springframework.samples.petclinic.owner.OwnerRepository#findById(java.lang.Integer)`.

### HYP-SCENARIO-002

- `org.springframework.samples.petclinic.owner.OwnerController#processFindForm(int,org.springframework.samples.petclinic.owner.Owner,org.springframework.validation.BindingResult,org.springframework.ui.Model)`: Normalizes query, chooses single-result redirect versus paged list, and rejects no-result searches. Production owner/OwnerController.java:95-122; behavioral support owner/OwnerControllerTests.java:145-194.
- `org.springframework.samples.petclinic.owner.OwnerController#findPaginatedForOwnersLastName(int,java.lang.String)`: Creates the bounded page request and forwards the normalized surname. Production owner/OwnerController.java:133-137; behavioral support owner/OwnerControllerTests.java:145-183.
- `org.springframework.samples.petclinic.owner.OwnerRepository#findByLastNameStartingWith(java.lang.String,org.springframework.data.domain.Pageable)`: Source-declared repository query boundary accepts the normalized surname and pagination; mock interaction supports argument behavior, not database execution. Production owner/OwnerRepository.java:38-45; behavioral support owner/OwnerControllerTests.java:163-183.

Directed edges (each arrow corresponds to the caller's exact source body above):

- `org.springframework.samples.petclinic.owner.OwnerController#processFindForm(int,org.springframework.samples.petclinic.owner.Owner,org.springframework.validation.BindingResult,org.springframework.ui.Model)` → `org.springframework.samples.petclinic.owner.OwnerController#findPaginatedForOwnersLastName(int,java.lang.String)`.
- `org.springframework.samples.petclinic.owner.OwnerController#findPaginatedForOwnersLastName(int,java.lang.String)` → `org.springframework.samples.petclinic.owner.OwnerRepository#findByLastNameStartingWith(java.lang.String,org.springframework.data.domain.Pageable)`.

### HYP-SCENARIO-003

- `org.springframework.samples.petclinic.owner.OwnerController#processCreationForm(org.springframework.samples.petclinic.owner.Owner,org.springframework.validation.BindingResult,org.springframework.web.servlet.mvc.support.RedirectAttributes)`: Accepts valid owner details and invokes inherited persistence before returning the detail redirect. Production owner/OwnerController.java:77-87; behavioral support owner/OwnerControllerTests.java:115-123.
- `org.springframework.samples.petclinic.owner.OwnerController#showOwner(int)`: Independently serves the detail view required for initial selection or subsequent inspection. This is a separate request, never a claimed call from a redirecting handler. Production owner/OwnerController.java:169-177; behavioral support owner/OwnerControllerTests.java:245-256.
- `org.springframework.samples.petclinic.owner.OwnerRepository#findById(java.lang.Integer)`: Declared owner lookup boundary used by the detail handler; repository runtime is mocked in the test. Production owner/OwnerRepository.java:48-60; behavioral support owner/OwnerControllerTests.java:245-256.

Directed edges (each arrow corresponds to the caller's exact source body above):

- `org.springframework.samples.petclinic.owner.OwnerController#showOwner(int)` → `org.springframework.samples.petclinic.owner.OwnerRepository#findById(java.lang.Integer)`.

### HYP-SCENARIO-004

- `org.springframework.samples.petclinic.owner.OwnerController#processUpdateOwnerForm(org.springframework.samples.petclinic.owner.Owner,org.springframework.validation.BindingResult,int,org.springframework.web.servlet.mvc.support.RedirectAttributes)`: Maintains target identity and submits updated owner details for persistence. Production owner/OwnerController.java:144-162; behavioral support owner/OwnerControllerTests.java:212-227.
- `org.springframework.samples.petclinic.owner.OwnerController#showOwner(int)`: Independently serves the detail view required for initial selection or subsequent inspection. This is a separate request, never a claimed call from a redirecting handler. Production owner/OwnerController.java:169-177; behavioral support owner/OwnerControllerTests.java:245-256.
- `org.springframework.samples.petclinic.owner.OwnerRepository#findById(java.lang.Integer)`: Declared owner lookup boundary used by the detail handler; repository runtime is mocked in the test. Production owner/OwnerRepository.java:48-60; behavioral support owner/OwnerControllerTests.java:245-256.

Directed edges (each arrow corresponds to the caller's exact source body above):

- `org.springframework.samples.petclinic.owner.OwnerController#showOwner(int)` → `org.springframework.samples.petclinic.owner.OwnerRepository#findById(java.lang.Integer)`.

### HYP-SCENARIO-005

- `org.springframework.samples.petclinic.owner.PetController#processCreationForm(org.springframework.samples.petclinic.owner.Owner,org.springframework.samples.petclinic.owner.Pet,org.springframework.validation.BindingResult,org.springframework.web.servlet.mvc.support.RedirectAttributes)`: For creation, associates the valid pet before persistence; for duplicate rejection, detects the existing owner's pet name and returns a field error. Production owner/PetController.java:107-137; behavioral support owner/PetControllerTests.java:98-104,124-134.
- `org.springframework.samples.petclinic.owner.Owner#addPet(org.springframework.samples.petclinic.owner.Pet)`: Associates the new pet with its owner's collection, with duplicate-object protection. Production owner/Owner.java:97-110; behavioral support owner/OwnerTests.java:26-48; owner/PetControllerTests.java:98-104.
- `org.springframework.samples.petclinic.owner.OwnerController#showOwner(int)`: Independently serves the detail view required for initial selection or subsequent inspection. This is a separate request, never a claimed call from a redirecting handler. Production owner/OwnerController.java:169-177; behavioral support owner/OwnerControllerTests.java:245-256.
- `org.springframework.samples.petclinic.owner.OwnerRepository#findById(java.lang.Integer)`: Declared owner lookup boundary used by the detail handler; repository runtime is mocked in the test. Production owner/OwnerRepository.java:48-60; behavioral support owner/OwnerControllerTests.java:245-256.

Directed edges (each arrow corresponds to the caller's exact source body above):

- `org.springframework.samples.petclinic.owner.PetController#processCreationForm(org.springframework.samples.petclinic.owner.Owner,org.springframework.samples.petclinic.owner.Pet,org.springframework.validation.BindingResult,org.springframework.web.servlet.mvc.support.RedirectAttributes)` → `org.springframework.samples.petclinic.owner.Owner#addPet(org.springframework.samples.petclinic.owner.Pet)`.
- `org.springframework.samples.petclinic.owner.OwnerController#showOwner(int)` → `org.springframework.samples.petclinic.owner.OwnerRepository#findById(java.lang.Integer)`.

### HYP-SCENARIO-006

- `org.springframework.samples.petclinic.owner.PetController#processCreationForm(org.springframework.samples.petclinic.owner.Owner,org.springframework.samples.petclinic.owner.Pet,org.springframework.validation.BindingResult,org.springframework.web.servlet.mvc.support.RedirectAttributes)`: For creation, associates the valid pet before persistence; for duplicate rejection, detects the existing owner's pet name and returns a field error. Production owner/PetController.java:107-137; behavioral support owner/PetControllerTests.java:98-104,124-134.
- `org.springframework.samples.petclinic.owner.PetController#processUpdateForm(org.springframework.samples.petclinic.owner.Owner,org.springframework.samples.petclinic.owner.Pet,org.springframework.validation.BindingResult,org.springframework.web.servlet.mvc.support.RedirectAttributes)`: For valid updates delegates target mutation; for duplicate rejection checks same-name identity before any successful mutation. Production owner/PetController.java:144-179; behavioral support owner/PetControllerTests.java:192-210,218-231.
- `org.springframework.samples.petclinic.owner.Owner#getPet(java.lang.String,boolean)`: Owner-scoped case-insensitive name matching enforces the duplicate scenario for both create and update branches. Production owner/Owner.java:144-154; behavioral support owner/PetControllerTests.java:124-134,218-231.

Directed edges (each arrow corresponds to the caller's exact source body above):

- `org.springframework.samples.petclinic.owner.PetController#processCreationForm(org.springframework.samples.petclinic.owner.Owner,org.springframework.samples.petclinic.owner.Pet,org.springframework.validation.BindingResult,org.springframework.web.servlet.mvc.support.RedirectAttributes)` → `org.springframework.samples.petclinic.owner.Owner#getPet(java.lang.String,boolean)`.
- `org.springframework.samples.petclinic.owner.PetController#processUpdateForm(org.springframework.samples.petclinic.owner.Owner,org.springframework.samples.petclinic.owner.Pet,org.springframework.validation.BindingResult,org.springframework.web.servlet.mvc.support.RedirectAttributes)` → `org.springframework.samples.petclinic.owner.Owner#getPet(java.lang.String,boolean)`.

### HYP-SCENARIO-011

- `org.springframework.samples.petclinic.owner.PetController#processUpdateForm(org.springframework.samples.petclinic.owner.Owner,org.springframework.samples.petclinic.owner.Pet,org.springframework.validation.BindingResult,org.springframework.web.servlet.mvc.support.RedirectAttributes)`: For valid updates delegates target mutation; for duplicate rejection checks same-name identity before any successful mutation. Production owner/PetController.java:144-179; behavioral support owner/PetControllerTests.java:192-210,218-231.
- `org.springframework.samples.petclinic.owner.PetController#updatePetDetails(org.springframework.samples.petclinic.owner.Owner,org.springframework.samples.petclinic.owner.Pet)`: Looks up the identified existing pet, transfers submitted properties and invokes inherited persistence. The existing-pet scenario excludes its fallback add branch. Production owner/PetController.java:186-200; behavioral support owner/PetControllerTests.java:192-198.
- `org.springframework.samples.petclinic.owner.Owner#getPet(java.lang.Integer)`: Resolves the existing pet by identifier for mutation or visit attachment; inherited identity access is supporting plumbing. Production owner/Owner.java:126-136; behavioral support owner/PetControllerTests.java:192-198; owner/VisitControllerTests.java:76-83.
- `org.springframework.samples.petclinic.owner.OwnerController#showOwner(int)`: Independently serves the detail view required for initial selection or subsequent inspection. This is a separate request, never a claimed call from a redirecting handler. Production owner/OwnerController.java:169-177; behavioral support owner/OwnerControllerTests.java:245-256.
- `org.springframework.samples.petclinic.owner.OwnerRepository#findById(java.lang.Integer)`: Declared owner lookup boundary used by the detail handler; repository runtime is mocked in the test. Production owner/OwnerRepository.java:48-60; behavioral support owner/OwnerControllerTests.java:245-256.

Directed edges (each arrow corresponds to the caller's exact source body above):

- `org.springframework.samples.petclinic.owner.PetController#processUpdateForm(org.springframework.samples.petclinic.owner.Owner,org.springframework.samples.petclinic.owner.Pet,org.springframework.validation.BindingResult,org.springframework.web.servlet.mvc.support.RedirectAttributes)` → `org.springframework.samples.petclinic.owner.PetController#updatePetDetails(org.springframework.samples.petclinic.owner.Owner,org.springframework.samples.petclinic.owner.Pet)`.
- `org.springframework.samples.petclinic.owner.PetController#updatePetDetails(org.springframework.samples.petclinic.owner.Owner,org.springframework.samples.petclinic.owner.Pet)` → `org.springframework.samples.petclinic.owner.Owner#getPet(java.lang.Integer)`.
- `org.springframework.samples.petclinic.owner.OwnerController#showOwner(int)` → `org.springframework.samples.petclinic.owner.OwnerRepository#findById(java.lang.Integer)`.

### HYP-SCENARIO-007

- `org.springframework.samples.petclinic.owner.VisitController#processNewVisitForm(org.springframework.samples.petclinic.owner.Owner,int,org.springframework.samples.petclinic.owner.Visit,org.springframework.validation.BindingResult,org.springframework.web.servlet.mvc.support.RedirectAttributes)`: Accepts valid future date and description and requests visit attachment and persistence. Production owner/VisitController.java:97-112; behavioral support owner/VisitControllerTests.java:76-83.
- `org.springframework.samples.petclinic.owner.Owner#addVisit(java.lang.Integer,org.springframework.samples.petclinic.owner.Visit)`: Selects the identified pet and delegates visit insertion, ensuring correct pet association. Production owner/Owner.java:173-183; behavioral support owner/VisitControllerTests.java:76-83.
- `org.springframework.samples.petclinic.owner.Owner#getPet(java.lang.Integer)`: Resolves the existing pet by identifier for mutation or visit attachment; inherited identity access is supporting plumbing. Production owner/Owner.java:126-136; behavioral support owner/PetControllerTests.java:192-198; owner/VisitControllerTests.java:76-83.
- `org.springframework.samples.petclinic.owner.Pet#addVisit(org.springframework.samples.petclinic.owner.Visit)`: Adds the visit to the target pet's collection. Production owner/Pet.java:81-83; behavioral support owner/VisitControllerTests.java:76-83.
- `org.springframework.samples.petclinic.owner.OwnerController#showOwner(int)`: Independently serves the detail view required for initial selection or subsequent inspection. This is a separate request, never a claimed call from a redirecting handler. Production owner/OwnerController.java:169-177; behavioral support owner/OwnerControllerTests.java:245-256.
- `org.springframework.samples.petclinic.owner.OwnerRepository#findById(java.lang.Integer)`: Declared owner lookup boundary used by the detail handler; repository runtime is mocked in the test. Production owner/OwnerRepository.java:48-60; behavioral support owner/OwnerControllerTests.java:245-256.

Directed edges (each arrow corresponds to the caller's exact source body above):

- `org.springframework.samples.petclinic.owner.VisitController#processNewVisitForm(org.springframework.samples.petclinic.owner.Owner,int,org.springframework.samples.petclinic.owner.Visit,org.springframework.validation.BindingResult,org.springframework.web.servlet.mvc.support.RedirectAttributes)` → `org.springframework.samples.petclinic.owner.Owner#addVisit(java.lang.Integer,org.springframework.samples.petclinic.owner.Visit)`.
- `org.springframework.samples.petclinic.owner.Owner#addVisit(java.lang.Integer,org.springframework.samples.petclinic.owner.Visit)` → `org.springframework.samples.petclinic.owner.Owner#getPet(java.lang.Integer)`.
- `org.springframework.samples.petclinic.owner.Owner#addVisit(java.lang.Integer,org.springframework.samples.petclinic.owner.Visit)` → `org.springframework.samples.petclinic.owner.Pet#addVisit(org.springframework.samples.petclinic.owner.Visit)`.
- `org.springframework.samples.petclinic.owner.OwnerController#showOwner(int)` → `org.springframework.samples.petclinic.owner.OwnerRepository#findById(java.lang.Integer)`.

### HYP-SCENARIO-008

GOLD_UNAVAILABLE_SEMANTIC_SOURCE_MISMATCH. Accepted semantics require rejecting a future date beyond an allowed future range. VisitController.java:100-102 rejects dates that are not after today; it has no upper future bound. VisitControllerTests.java:97-106 tests today's date, while :76-83 accepts tomorrow. Neither proves the accepted future-range rejection. No expected pair or chain is invented for this scenario. It stays in selectedScenarios; its chain definition is absent (not empty), so the evaluator must report missing chain data and an unavailable mandatory chain metric. This limitation cannot be repaired by relabeling the accepted semantics or substituting a past-date rejection. Separate review must confirm this limitation before scoring.

### HYP-SCENARIO-009

- `org.springframework.samples.petclinic.vet.VetController#showVetList(int,org.springframework.ui.Model)`: Handles the selected batch of veterinarian directory data. Production vet/VetController.java:44-48; behavioral support vet/VetControllerTests.java:83-88.
- `org.springframework.samples.petclinic.vet.VetController#findPaginated(int)`: Builds the page request and invokes the pageable repository overload. Production vet/VetController.java:59-63; behavioral support vet/VetControllerTests.java:75-88.
- `org.springframework.samples.petclinic.vet.VetController#addPaginationModel(int,org.springframework.data.domain.Page,org.springframework.ui.Model)`: Exposes the selected vets and page metadata consumed by the directory. Production vet/VetController.java:50-57; behavioral support vet/VetControllerTests.java:83-88.
- `org.springframework.samples.petclinic.vet.VetRepository#findAll(org.springframework.data.domain.Pageable)`: The explicitly declared pageable overload supplies the paged directory boundary; zero-argument JSON listing is not the selected scenario. Production vet/VetRepository.java:48-56; behavioral support vet/VetControllerTests.java:75-88.
- `org.springframework.samples.petclinic.vet.Vet#getSpecialties()`: Produces the available specialty list read by templates/vets/vetList.html:20. Template expression is source structural evidence, not observed rendering. Production vet/Vet.java:59-64; behavioral support vet/VetControllerTests.java:62-88.
- `org.springframework.samples.petclinic.vet.Vet#getSpecialtiesInternal()`: Provides the backing specialty collection and empty initialization used by specialty-list and count accessors. Production vet/Vet.java:52-57; behavioral support vet/VetControllerTests.java:62-88.
- `org.springframework.samples.petclinic.vet.Vet#getNrOfSpecialties()`: Provides zero-specialty fallback condition read by templates/vets/vetList.html:21; this explicitly realizes the available-specialty presentation. Production vet/Vet.java:66-68; behavioral support vet/VetControllerTests.java:62-88.

Directed edges (each arrow corresponds to the caller's exact source body above):

- `org.springframework.samples.petclinic.vet.VetController#showVetList(int,org.springframework.ui.Model)` → `org.springframework.samples.petclinic.vet.VetController#findPaginated(int)`.
- `org.springframework.samples.petclinic.vet.VetController#showVetList(int,org.springframework.ui.Model)` → `org.springframework.samples.petclinic.vet.VetController#addPaginationModel(int,org.springframework.data.domain.Page,org.springframework.ui.Model)`.
- `org.springframework.samples.petclinic.vet.VetController#findPaginated(int)` → `org.springframework.samples.petclinic.vet.VetRepository#findAll(org.springframework.data.domain.Pageable)`.
- `org.springframework.samples.petclinic.vet.Vet#getSpecialties()` → `org.springframework.samples.petclinic.vet.Vet#getSpecialtiesInternal()`.
- `org.springframework.samples.petclinic.vet.Vet#getNrOfSpecialties()` → `org.springframework.samples.petclinic.vet.Vet#getSpecialtiesInternal()`.

## Seal and review boundary

truth.json is frozen before producer generation. Its SHA256 is reported
separately to the parent without gold identities, counts, or error hints.
This document and truth are evaluator-only. A separate actor must independently
review the gold and later adjudicate exact proposal-digest-bound source proofs.
The author cannot provide that independent review. The absent definition above
is an explicit readiness limit, not an empty-chain success. No tuning against
scored misses is authorized. The report must retain CALIBRATION and NOT_RUN
for the formal experiment regardless of any arithmetic outcome.

