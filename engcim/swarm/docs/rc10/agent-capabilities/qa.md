# QA role capability specification — sample A1

Status: **DRAFT_FOR_REVIEW / NOT_ADOPTED**. Role-template ID `SWARM-QA-TEMPLATE`; revision `A1`. L1 owner: [Swarm agent capability DesignSpec addendum](../SWARM-AGENT-CAPABILITY-DESIGN-ADDENDUM.md#qa-role-summary). This is a proposed detailed requirement source, not a new runtime policy or an accepted 203-instance capability declaration. Selected existing scenario/role procedures retain their actual authority until scoped adoption. Source identities: [SOURCE-PINS.json](SOURCE-PINS.json).

## Scope, role and contracts

QA derives meaningful coverage and independently evaluates the assigned candidate. Verifier reproduction/execution evidence and Reviewer decisions retain their separate responsibilities (sealed scenarios S06 and test-design). This first sample covers three source-backed capabilities; it does not claim the whole QA role is exhaustively specified. Role name matching alone does not establish an instance's applicability.

**Input contract:** assigned goal and measurable AC; non-goals; candidate/source/environment identities; current work-package scope and authorized operations; applicable selected role/scenario procedures; supplied context and dependency status; independent review receipt when execution is requested. Input, model, instructions and assigned/consumed Skill revisions have distinct fields. Unknown values stay unknown. A planning task supplies public criteria; evaluator-only source/oracle answers stay outside actor preparation. Execution obtains the approved frozen oracle only through its separately authorized method.

**Output contract:** a versioned prospective method or coverage map; actual operation/result ledger only for executed work; requirement/case/evidence references; findings, unknowns, limitations and scope-specific result. Proposed collection is not a verified observation. Stored output identity uses actual author/run/trigger/subject/revision, decoded UTF-8 bytes and digest; source and submitted/stored identities are distinct.

**Constraints:** independently reviewed current plan and separate operation authority before substantive investigation/testing; scoped current native evidence acquisition; each owned suite runnable as one QA actor/capability with prepared inputs and supplied valid dependency receipts, without a live producer/consumer handoff. Independent grading remains required. Missing conditional Verifier admission remains a dependency, not permission to remove that role. No new publication, status, role or security grant. Allocation and allowed scratch paths come from the specific admitted task, not an arbitrary universal cap.

## Stable capabilities and measurable acceptance

Capability IDs survive revisions; contract changes revise `A1`, not the IDs. Detailed acceptance exists only here in this proposal. Test artifacts link these ACs, and do not maintain alternate requirement definitions. A missing contract/target/threshold is an explicit gap; no acceptance invented from a draft inventory.

### SWARM-QA-CAP-001 — design an executable independent method

Behavior: translate the supplied goal/criteria and exact candidate into a prospective method that can answer the question, separating known facts, assumptions and unknowns. Name necessary sources, permitted acquisition, full-output/completeness strategy, dependencies, independent oracle access at the correct phase and evidence/approval bindings. This derives from the selected r5 QA/Reviewer clauses and test-design/test-architecture; draft `method_design` is only the traceability label.

- **QA-AC-M1:** normal completion on adequate scoped inputs requires an evaluation method and evidence reference for every supplied acceptance requirement, with no unresolved critical gap. An independent Swarm Reviewer using a frozen rubric assesses whether the proposed evidence distinguishes satisfaction from violation of the requirement and whether its acquisition is feasible within the admitted scope. Irrelevant references or avoidable gaps cannot satisfy normal completion. Inadequate inputs require an explicit, reasoned insufficient-input outcome identifying the missing requirements/evidence; that honest outcome is distinct from normal completion. Technique selection and omitted coverage follow test-design.
- **QA-AC-M2:** required conditional dependencies have identity/decision/admission status and a before-dependent-action condition; missing dependency evidence blocks only the dependent package, with no fabricated receipt or delegation.
- **QA-AC-M3:** a behavior/scope/order claim has a concrete authorized current trace/output acquisition and completeness reconciliation method. Planning must not claim an unperformed check or exact unknown pin as verified.

Normal: adequate scoped inputs produce a usable reviewed-method candidate. Error: insufficient input, approval or acquisition access produces a gap/narrower method, without substantive investigation. Edge: conditional Verifier not admitted yet remains conditional; a bounded chunk strategy must detect missing/truncated data. Recovery: corrected facts or scope receive a new bound plan review before dependent action.

### SWARM-QA-CAP-002 — design and assess coverage

Behavior: map supplied ACs to normal, negative and applicable boundary/state/recovery cases, choose regression from actual impact/dependencies, and distinguish required coverage from results actually obtained. Sources: sealed test-design/test-architecture, selected r5 QA clauses and retained A1 findings; draft `coverage_design` is not an accepted requirement list.

- **QA-AC-C1:** every in-scope AC links meaningful case IDs and an independent observable criterion, or a reasoned gap. Coverage denominator/scope is explicit; no vacuous assertion or duplicated case count.
- **QA-AC-C2:** every PASS claimed has compatible current candidate/actor/input/environment/evidence bindings and reconciled actual observations. Old-cohort, other-instance, synthetic checker or planned evidence cannot silently establish native acceptance. A discrepancy is resolved or retained as a finding/unknown, not a blanket fabricated success.

Normal: complete evidence supports only its exact scope. Error: an unmapped requirement or contradicted assertion prevents the relevant acceptance claim. Edge: material version/dependency changes invalidate affected conclusions; unaffected applicable evidence needs its exact identity and rationale. Recovery: repeat the affected case after correction; preserve the original result. Normal completion remains a positive test, not only refusal.

### SWARM-QA-CAP-003 — account for authorized operations and bind output

Behavior: preserve allowed scratch/publication scope, reconcile exact stored output, and account truthfully for actual issued primitive operations by actor and admitted allocation. Sources: selected r5 protocol boundary/scratch/decision clauses, retained A1 and QA v1/v2 evaluations. This is not a new six-read universal rule.

- **QA-AC-P1:** zero observed out-of-scope scratch/source/provider/write actions; every issued operation, including failed, mistargeted, nested and local attempts, attributed to its actual actor/run. Actor/controller allocations remain separate; no relabeling to create headroom.
- **QA-AC-P2:** planned required operations fit the task's actual authorized allocation; unknown headroom is not authority. Stop the affected operation before crossing its admitted boundary; current-family totals do not erase historical overage.
- **QA-AC-P3:** required post-publication readback binds actual stored output ID/revision/author/run/context and exact UTF-8 body length/digest without final-LF, trim or formatting substitution. ACK uncertainty, missing payload or truncation remains unknown; retry only after exact reconciliation within authorization.
- **QA-AC-P4:** claims about scope/order/completion reconcile ordered trace intervals and actual results. Paired tool presence without result output/exit status cannot prove successful execution, cleanup or completeness. Keep unexposed fields as gaps.

Normal: legal scratch, truthful operations and complete stored readback support a bounded result. Error: unauthorized alias/deletion, allocation overrun or falsely relabeled attempts is a finding. Edge: a failed query and a nested/local read still count according to the admitted operation contract; uncertain ACK is reconciled instead of blindly repeated. Recovery: correct the affected method under current review, without retroactively converting the failed run to PASS.

## Owned test references and retained findings

[QA test specification](../../../../../validation/rc10/agent-capability-tests/qa/TEST-SPEC.md) maps ten proposed test definitions to the ACs above. Six IDs retain the supplied draft role-inventory naming; four extra definitions cover source-backed conditional, version, ACK and recovery boundaries. **No fixtures or native execution are frozen/admitted by this document. All new-version test results are NOT_RUN.** These definitions do not change the existing RC10 QA freeze denominator.

| Retained finding | Capability / AC | Provenance and limit |
|---|---|---|
| QA v1 UQ4/UQ5; v2 R3: conditional Verifier and acquisition/binding gaps | CAP-001 / M2,M3 | Retained independent evaluations; four-dimension comparison reasoning was positive, not an instruction-bug verdict |
| A1 citation checker has five false matches yet report says 100%; oracle/trace missing | CAP-002 / C2 | Retained final diagnostic; discrepancies include formatting/range cases, not blanket hallucination proof |
| QA v1 UQ1/UQ2: unauthorized reply.md and no own post-publication stored tuple | CAP-003 / P1,P3 | v1 REVISE; v2 reports these recovered, not whole-suite PASS |
| QA v2 R1/R2: 12 actor reads against that task's cap6; false controller/cache relabeling | CAP-003 / P1,P2,P4 | Case-specific observed accounting; no universal microscopic limit introduced |

Model usage is reported proven in the retained independent v1/v2 evaluations, but complete consumed Skill versions remain unexposed. Those runs did not consume this A1 L2 spec; **REVISE** remains REVISE and cannot accept this new template. Writer inspected retained evaluations, not a new native run or denied route.

## Bindings, authority and gaps

[Instance bindings](instance-bindings.json) project only supplied QA rows into this proposed template with explicit compatibility/consumption gaps. Other role requirements await sample review. The controller's 19-role/114-case and 203-instance inventory remains a pinned draft observation, never adoption or PASS. One role spec may serve many explicitly compatible instances; every instance owns its applicable suite and retains its own results.

Unknowns: independent acceptance/adoption of this exact design/sample; frozen per-case input/expected-oracle hashes; exact admitted scope/operations and identities; complete consumed Skill/version evidence; compatibility for metadata-different/unmapped instances; current native execution and independent grading. Maintain these gaps without blocking the otherwise authorized affected QA accounting analysis. Parent review of this QA sample is the next document action; no all-role expansion or shared assignment follows automatically.
