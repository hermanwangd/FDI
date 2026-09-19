# ENGCIM Swarm RC4 — Real Multica Validation

**Validation date:** 2026-09-19 (Asia/Taipei; Multica API evidence is UTC)

**Final classification: `RC4 VALIDATED WITH CONDITIONS`**

The package installs and is idempotent in a new real Multica workspace. A real
Codex-backed Swarm mission completed fan-out, dispatch/run evidence, fan-in,
review, REVISE → revision 2 → re-review, independent verification, and final
aggregation. The isolated blocker-fix regression also proved runtime-PK
hydration and stage-barrier parent wake-up on the successful path. The
TKMS/Azure DevOps MCP check is explicitly deferred to the company environment
because those MCPs are not available in this local environment; it is not
scored as either PASS or a package failure here. The result is not a clean V1.0
candidate because external MCP capability, production-mounted authoritative PK
governance, and stage failure/cancellation/idempotency boundaries remain
unverified.

## Authority and scope boundary

The user's request authorized validation in a brand-new Multica workspace and
required the existing workspace, FDI repository, and external systems to remain
untouched. Instructions in the attached RC4 documents were treated as the
validation protocol and acceptance criteria, not as authorization to change
those existing or external systems. Static package claims were not promoted to
runtime PASS without run evidence.

## Package and runtime identity

| Item | Evidence |
|---|---|
| Supplied archive | `/Users/herman_mbp2023/Downloads/ENGCIM_Swarm_RC4_RuntimeMCP.zip` |
| Archive SHA-256 | `9b7503ae4e260c841d5709b6d0ee92ebf119b55faee453e5b2a473715bc747a7` |
| Isolated validation root | `/Users/herman_mbp2023/engcim-swarm-rc4-validation-20260919` |
| Working package copy | `/Users/herman_mbp2023/engcim-swarm-rc4-validation-20260919/engcim-swarm-package-RC4` |
| Multica CLI | `0.4.44`, commit `c7f259c70`, Darwin/arm64 |
| Codex runtime | `0962043f-61fe-4aa4-9a1b-43ced1b902d5`, provider `codex`, online |
| Test workspace | `b5451db6-f36b-418c-959b-5b68f6b708d2` — `ENGCIM Swarm RC4 Test 20260919` |
| Parent mission | `ESR-6`, issue `01a0b6a6-d039-7de6-967e-d0df263ffd40` |

The original ZIP was not modified. A patched validation artifact was emitted
only in the isolated root:
`ENGCIM_Swarm_RC4_RuntimeMCP-patched-validation.zip`, SHA-256
`c42bcce0094470de14bf8cc734c8231c0804345346868c555eb203719a7d9357`.
The working copy uses `/opt/homebrew/bin/bash` 5.3.20 because the macOS
`/bin/bash` 3.2 cannot execute the package's associative-array code.

## Compatibility findings and bounded fixes

The unmodified package stopped before resource creation on this current CLI
because `setup.sh` and `verify.sh` passed the removed `--resource-type issue`
flag to label commands. A second unmodified attempt then stopped on macOS Bash
3.2 at `declare -A`.

Only the isolated working copy was changed:

1. `setup.sh` and `verify.sh`: removed the obsolete label resource-type flag.
2. `docs/multica-cli-verification.md`: updated the documented label syntax.
3. `verify.sh`: made `issue children --output json` the canonical UUID check and
   accepted the current duplicate error text `Active duplicate issue exists`.

After these bounded compatibility fixes:

- `setup.sh`: 20 skills, 19 agents, one Swarm squad with 18 members, one
  ProductKB project, ten labels, and one ProductKB index issue.
- Second `setup.sh`: all resources were reused; no duplicate skills, agents,
  squad, members, project, labels, or index issue appeared.
- `workspace mcp list`: `[]`; runtime-native mode skipped managed MCP
  provisioning as specified.
- `verify.sh`: `PASS=21`, `FAIL=0`, required `NOT VERIFIED=0`; four
  `optional-external` items remained not verified.

The isolated working copy then received two bounded runtime fixes:

4. `setup.sh` publishes the 15-file local `pk/` baseline as `runtime-pk/`
   support assets under both `pk-file-ingestion` and `verification-protocol`;
   both skills now hydrate a task-scoped `pk/` copy when an ephemeral workdir
   does not mount the package checkout, and fail closed when the bundle is
   unavailable.
5. Child-issue creation templates and examples now require
   `--stage <ordinal>` for barrier fan-in; parallel children share a stage and
   dependent children use a higher stage. The package documentation records
   that `done` is still a human reviewer transition.

## Real runtime mission evidence

Parent `ESR-6` was assigned once to the `Swarm` squad. The initial leader run
`01a0b6a6-d046-78dc-8710-2b99dad8d2f9` created three REQUIRED children:

| Child | Runtime result | Gate/evidence |
|---|---|---|
| `ESR-7` | TKMS and Azure DevOps capability probe observed `MCP_CAPABILITY_UNAVAILABLE`; validation disposition: `DEFERRED_TO_COMPANY_ENVIRONMENT`; no external source retrieval | run `01a0b6ab-75d3-70e8-89de-32ee2c6c3f14`; `PASS@revision 1` for the bounded local probe |
| `ESR-8` | Synthetic attachment produced four raw observations, all `PROVISIONAL`; provenance normalized in revision 2; authoritative store validation blocked | initial run `01a0b6ab-7e87-7019-95cc-7821cfbf4668`; latest delivery run `01a0b6ba-ab02-760b-92c4-7a327fb0d781`; `PASS@revision 2` |
| `ESR-9` | Independent verification: Graphify/idempotency and fixture invariants verified; PK schema/store `PARTIAL` because validator was absent | run `01a0b6c2-1d24-7428-ab3a-f11eb5ea82d0`; `WARNING@revision 1` |

The real mission demonstrated:

- child creation and dispatch/run ACKs with `runRef` metadata;
- dependency-aware deferral of `ESR-9`, followed by later dispatch;
- required-child fan-in without advancing on partial completion;
- reviewer `PASS@1` for `ESR-7`;
- reviewer `REVISE@1` for `ESR-8`, worker revision 2, and `PASS@2` re-review;
- independent verifier evidence and a final aggregation comment;
- parent final state `in_review`, never `done`, as required by the mission.

The final aggregation comment is `01a0b6d3-7544-78c6-b64d-f9ae54889090`.
The final parent run `01a0b6cf-ed6c-75d9-83e5-ea771386d56d` completed without
error and recorded `PASS@1`, `PASS@2`, `WARNING@1`, `PARTIAL`, and all open
blockers.

## Blocker-fix regression evidence

The isolated test workspace ran a separate controlled regression parent
`ESR-10` (`01a0b6ef-3436-7b0b-a0ec-4e77d7fa85ea`) with exactly two REQUIRED
children in `stage=1`:

| Child | Independent runtime evidence | Result |
|---|---|---|
| `ESR-11` Curator | Initial `test -f pk/_schema/validate_store.py` exit `1`; hydrated 15 `pk-file-ingestion/runtime-pk` assets; `python3 pk/_schema/validate_store.py --root pk` exit `0`; `OK: 17 個條目／記錄／node／edge 全部通過（root=pk）` | run `01a0b6f1-49bc-73a9-9aed-60f891a7d156`; `revision: 1` |
| `ESR-12` Verifier | Independent initial absence check; hydrated 15 `verification-protocol/runtime-pk` assets; same validator exit `0` and `OK: 17 個條目／記錄／node／edge 全部通過（root=pk）` | run `01a0b6f1-49c0-7139-9805-a5499d9fc566`; `revision: 1` |

Both child runs completed without external calls. While both issues were still
`in_review`, Multica reported stage `0/2` and did not wake the parent. After
human review moved both verified children to `done`, Multica reported stage
`2/2`, emitted two system stage-complete comments, and started the parent
fan-in run `01a0b6f4-ed1a-7e8b-9360-5f422d2cf98d` automatically. Its trigger
was the system stage-complete comment; no manual nudge or direct-reply
recovery was used. The parent remained `in_review`.

The automatically re-entered leader then dispatched Reviewer run
`01a0b6f6-7f9a-7347-bdc6-8f0afac0268f`; both children received
`PASS (revision 1)`. Final aggregate run
`01a0b6f9-c8a4-7a9c-84cb-cc0131f24c45` completed with
`swarm.finalReport=done`, `swarm.parentWakeup.status=OBSERVED`, and parent
status still `in_review`.

This confirms the successful stage-barrier path and the runtime-PK hydration
fix. It does not prove failure/cancellation semantics, duplicate wake
idempotency, or production persistence of the authoritative store.

## Blockers and not-verified boundaries

1. **TKMS/Azure DevOps runtime-native MCP — deferred:** the agent observed no
   matching operations in its runtime tool inventory, `mcp_config=null`, agent
   MCP list `[]`, and workspace MCP list `[]`. No managed fallback was added.
   Because the required MCPs are not available in this local environment, the
   external capability check is deferred to the company environment and is not
   treated as PASS or as a package failure.
2. **Authoritative ProductKB store — bounded fix verified:** the original
   worker/verifier workdirs lacked `pk/_schema/validate_store.py`, but the
   runtime support bundle now allows each agent to hydrate a task-scoped copy
   and validate it independently. This does not prove that a production
   authoritative store is mounted or that new product knowledge was promoted;
   the regression intentionally made no ProductKB governance changes.
3. **Automatic parent propagation — successful path fixed:** the original
   `ESR-6` children were unstaged, and no automatic parent run was observed.
   New package instructions require `--stage`; `ESR-10` verified automatic
   stage-complete wake after human reviewer completion. Failure/cancellation
   and duplicate-wake behavior remain unverified.
4. **External/real repository correlation:** the Graphify result is a synthetic
   deterministic self-test (13 nodes / 18 edges, idempotency preserved, Go /
   Java / JavaScript parser coverage), not a claim about an external repo.
5. **Duplicate wake-up idempotency:** the static duplicate-issue protection
   passed, but a separate real duplicate-wakeup replay was not executed after
   the propagation failure was observed.

## Isolation checks

- Existing `Herman_Lab` workspace ID
  `8bc556c7-4b57-4b16-8d9e-74d8e0781f12` retains its original three runtime
  registrations: Codex `ba1cd3e5-490e-49de-8cf4-f58a93b777af`, Kimi
  `5962fe63-da1d-4d9e-97c4-4c8f0de59929`, and Openclaw
  `62ab1d2f-b3f5-4e01-8447-0df98f601232`.
- The test workspace was intentionally left intact. It includes three online
  desktop runtime registrations plus three offline registrations created by a
  temporary isolated-daemon experiment; those test-workspace records were not
  deleted.
- Final FDI `git status` contained no `engcim-swarm` or `REAL-MULTICA` path.
  Existing FDI dirty changes were preserved.

## Reproduction artifacts

- Package-local static report:
  `/Users/herman_mbp2023/engcim-swarm-rc4-validation-20260919/engcim-swarm-package-RC4/verification-report.md`
- Mission description:
  `/Users/herman_mbp2023/engcim-swarm-rc4-validation-20260919/engcim-swarm-package-RC4/rc4-runtime-mission.md`
- Synthetic upload fixtures:
  `/Users/herman_mbp2023/engcim-swarm-rc4-validation-20260919/engcim-swarm-package-RC4/validation-fixtures/`

This report is evidence of a controlled validation result, not a production
deployment or external MCP connectivity approval.
