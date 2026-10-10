# Four-role test assets

This directory is the single editable four-role test source on the selected Git ref. The shared [capability inventory](../capabilities.json) covers all 19 observed role templates; this directory does not represent the entire Swarm. The development source entry is [Swarm module](../../../README.md); the existing [source-to-effective map](../../../SOURCE-TO-EFFECTIVE.json) binds code, instructions, Skills and these tests. Exact file versions are in [SOURCE-MANIFEST.json](SOURCE-MANIFEST.json). External dated evidence folders retain historical inputs/results only.

## Active definitions

- [unit-suite.json](unit-suite.json): original 36 decision cases.
- [supplement-suite.json](supplement-suite.json): 37 supplemental cases.
- [original-branch-suite.json](original-branch-suite.json): six original QA/Architect responsibility branches.
- [focused-suite.json](focused-suite.json): nine focused native cases, including T1/T2 and the added O-SPECIALIST-UNFAMILIAR case for the same Orchestrator responsibility boundary. Original eight definitions remain unchanged; the new case is UNTESTED.
- [shared capabilities.json](../capabilities.json): 65 original detailed ability mappings plus all 19 role/source/Skill/test references. The other roles retain their six historical draft cases each; current fixtures, admission and native results remain gaps.

These are definitions, not admission or PASS. The former two-case Orchestrator suite is an immutable historical fixture, not a second active source. Keep one active filename per object; retain prior versions in Git. Use the same committed input/criteria, configuration and method for any claimed before/after comparison.

## Native preparation

[native-preparation.json](native-preparation.json) owns the current M2 preparation method for T1/T2. Submit only its neutral title/body projection; case ID, source/configuration pins, prior results and oracle stay controller-side. T1 derives from the original focused input exactly; T2 changes only the destination locator to `deliverable`. The original nine cases remain unchanged. M2 differs from historical M1; results across those methods cannot establish an instruction-change effect.

A fresh filename or issue does not isolate a working-directory listing or session history. Before triggering, disclose inherited instructions and every visible root, retain a fresh public cwd inventory and either a genuine public new-empty session reservation or supported fresh-creation-at-launch control. The latter does not require future session/run IDs; the public v0.6.1 Kimi adapter returns a new session ID during launch. After launch, separately verify actual creation and run/session binding. Missing or UNKNOWN freshness evidence means HOLD. The installed Kimi runtime can add a cwd listing during prompt preparation; this mechanism alone does not prove a particular run consumed it. Missing complete initial prompt alone does not veto separately reviewed focused checks; it limits loading/clean-context claims.

The Java `NativeValidationPreparationTests` checks canonical task derivation and rejects contamination, old artifacts and incomplete/mismatched isolation declarations using synthetic samples. It is a local test/preparation check, not a deployed admission service. Synthetic PASS and receipt fields cannot replace independent inspection of actual public evidence. Real fresh-session binding and native clean baseline remain UNVERIFIED. No private daemon/session fallback is permitted.

## Existing local checks

From this directory, with Node heap limited to 256 MiB. Fixture reads resolve from each test module, so the caller working directory does not select fixture content:

```sh
node --max-old-space-size=256 --test role-unit-evidence-v3.test.mjs role-unit-output-schema-r1.test.mjs native-adapter/consumer.test.mjs
node --max-old-space-size=256 qa-own-issue-harness-r4.unit.mjs
node --max-old-space-size=256 qa-reused-fixture-scope-r3.unit.mjs
```

The same checks from the repository root:

```sh
node --max-old-space-size=256 --test engcim/swarm/tests/agents/four-role/role-unit-evidence-v3.test.mjs engcim/swarm/tests/agents/four-role/role-unit-output-schema-r1.test.mjs engcim/swarm/tests/agents/four-role/native-adapter/consumer.test.mjs
node --max-old-space-size=256 engcim/swarm/tests/agents/four-role/qa-own-issue-harness-r4.unit.mjs
node --max-old-space-size=256 engcim/swarm/tests/agents/four-role/qa-reused-fixture-scope-r3.unit.mjs
```

Provider/schema checks retain 107 cases; [Adapter consumer checks](native-adapter/consumer.test.mjs) own the same 17 memory-only regression groups beside their subject. They remain one local suite with 124 cases. The consumer file also runs independently with `node --max-old-space-size=256 --test native-adapter/consumer.test.mjs` from this directory. QA checks retain 46 and 9 assertions. These are local checker results. The shared 19-role inventory and native acceptance gaps are unchanged.

SOURCE-MANIFEST keeps historical command receipts under `selfTests.historicalExecution`; current commands/results bind the current tools, fixtures and definitions separately. Older phase notes below retain their then-current cwd requirements and results; use these current commands for local checks.

These are existing external test-provider assets, not a new FDI runner or framework CLI. Their self-tests validate local checker behavior only. Java runtime/tests stay in `engcim/swarm/src/`; native role runs and independent semantic grading retain their existing procedures.

## Native adapter maintenance and local integration

The located ZG consumer consists of `phaseZG-controller.cjs`, `phaseZG-capture-method.cjs`, `phaseZG-routing-method.cjs` and `phaseZG-ops.cjs` in the owned historical evidence directory. `PHASE-ZG-EXECUTION-SUBJECT.json` revision `ZG-EXECUTION-5` pins those files; `PHASE-ZG-DERIVATION.json` explicitly labels them `PER_RUN_EXTERNAL_EVIDENCE_ONLY_NOT_MAINTAINED_FRAMEWORK`. Their exact paths are absent from the inspected Git tree and local-ref history. This does not establish that no external adapter exists. The source-location result is `NATIVE-CONSUMER-LOCATION-RESULT.json` in that evidence directory; it is a result, not another authoring source.

The single maintenance location for the local candidate of that same consumer is `engcim/swarm/tests/agents/four-role/native-adapter/`, owned by the assigned Swarm Dev implementer. Stable filenames replace the phase-suffixed source names; historical evidence stays unchanged. The import and exact file versions are bound in SOURCE-MANIFEST, not in a second suite or executable source directory.

| Maintained candidate below `native-adapter/` | Historical source reused | Responsibility |
| --- | --- | --- |
| `controller.cjs` | `phaseZG-controller.cjs` | Exact reviewed subject, dispatch/observation lifecycle, bounded STOP and terminal closure |
| `capture-method.cjs` | `phaseZG-capture-method.cjs` | Owned loading-source observation and capture request checks |
| `routing-method.cjs` | `phaseZG-routing-method.cjs` | Owned delegation, directory and request observations |
| `operations.cjs` | `phaseZG-ops.cjs` | Explicitly scoped external CLI operations, budgets and durable evidence |

**The four sources are imported into the local candidate and exercised only with mocked operations; no fresh Native execution is established.** Current independent plan review classifies them by their actual responsibility: the existing external test operator, confined to the agent-test assets, with no FDI runtime/API/service/CLI integration or new actor authority. This is not a Node framework exception. New executable FDI framework behavior still belongs in Java 17; a directory name or passing `JavaOnlySourcePolicyTests` cannot grant an exception. That test currently checks only for Python files under `src/main`. The historical per-run scripts remain evidence, not a competing editable source.

The maintained controller requires an explicit physical evidence root, case key and caller/workspace/runtime context, matching the exact execution subject and review. Main takes admission before using preparation, and the operator locks the first valid subject hash computed from the same bytes it parses; every later read/operation rejects subject replacement, even if a new review matches that replacement. A fresh operator instance can admit a newly reviewed subject. Importing the four modules performs no filesystem observation, dispatch or CLI operation. Repository helper imports are relative. The current controller, both methods, operations, evidence provider and paged-source helper must all have current dependency pins before dispatch. Preparation and both agent capability records require unique named dependency pins; their verified bytes are parsed before use. An existing dispatch-wait supplement requires a named pin before it can enlarge preparation budgets. Conditional unfamiliar readiness is pinned by the exact execution review, with its verdict and subject hash checked before every operation; this avoids a circular subject/readiness hash. Original `PHASE-ZG-*` evidence filenames remain the compatible per-run record format; they do not select the code version or grant reuse of a prior review. No ambient working-directory default, private evidence root, workspace override or blind operation retry is admitted. This source change grants no native execution permission; fresh execution still needs its exact applicable subject review.

### Minimal integration scope

1. Reuse the four located sources and their exact historical pins; retain the originals as immutable results. Repository-owned helper imports resolve relative to the maintained module, while per-run inputs explicitly bind evidence output, actor/workspace/runtime/run, approved public roots and budgets. Per-run preparation/state/results remain evidence, never editable code SSOT. Operations now require exact admission and current dependency/context matching before every CLI request, including preparatory budget kinds; evidence-root scope, workspace overrides and invalid operation labels are rejected before any operation. These are disclosed operator-boundary changes, not role-capability changes.
2. Preserve structured inspector results at the capture `inspectOwnedCaptureCommand` boundary and the routing workspace/member/Java/delegation/directory boundaries before they become reason strings. Reuse existing `workspaceInspections` and normalized receipts; supplement missing observations instead of duplicating them. Bind each retained raw result to its request/trace identity, run and sequence, and the inspector/classifier source version. Do not reconstruct a fabricated status from a flattened reason.
3. Apply the existing `interpretMethodObservation` to those raw results and retain a separate interpretation beside the original evidence. The controller currently aggregates `method.scope` reasons into `violations` and routes them to `SCOPE_STOP`; retain those reasons, grants, STOP behavior and budgets. Unsupported/incomplete interpretation does not authorize continuation, establish an actual private read, or produce a role verdict. Unknown or absent evidence remains incomplete; real contract conflicts remain conflicts. Do not introduce a second classification vocabulary.
4. Keep mechanical checks, method interpretation, effects/loading, controller settlement and independent semantic acceptance separate. The classifier's `roleAcceptance: UNVERIFIED` is intentional. Changing observer coverage does not repair a role instruction, establish full capability coverage or upgrade a historical STOP.
5. Update active file pins in the existing SOURCE-MANIFEST, the source-map binding and corresponding release rows together. Freeze a new exact execution subject and obtain its applicable independent review before a future native invocation. The historical subject pins provider SHA `459c46934b983ae3f86f044bf6e920d8af1e7a7d098d33a8ea164dc7f358a716`; its admission cannot be reused with the later classifier version.

### Verification plan and pending acceptance

The existing suite retains provider/schema checks in their owning test files and the same 17 consumer groups in native-adapter/consumer.test.mjs; fixtures stay in their existing location. Original provider/schema baseline: 107 PASS. Two consumer checks first failed because the maintained entry was absent; after integration they passed. Further regressions exercise actual capture/routing calls, whole-controller normal and stopped paths with in-memory filesystem/CLI, import purity, admission/dependency/context rejection and durable once-only failure/budget handling. The first running-controller mock used non-UUID identities and correctly hit the existing deadline identity guard; that failed attempt is retained and the mock corrected without weakening the guard. Independent review reproduced a missing-input-pin gap: changing an unsealed preparation target could reach a mocked foreign assignment. Four regression groups first produced 118 PASS / 4 FAIL; named-input guards corrected them. A further independent counterexample swapped subject and matching review after assignment while the controller retained old preparation; the frozen-subject regression produced 123 PASS / 1 FAIL and passed after subject locking. The final local suite has 124 PASS, including late capability/readiness changes and zero-operation rejection for missing, duplicate or changed inputs. Positive mocks now seal the complete relevant input closure, so their result is local operator evidence only. Local method evidence never becomes Native or role acceptance. Fresh native expiry and unfamiliar cases remain unexecuted.

| Criterion | Current evidence or gap | Required after integration |
| --- | --- | --- |
| Structured observation reaches common interpretation | Original provider/test references existed without maintained native caller | Local candidate consumer retains raw result, separate interpretation, run/seq/provider pin and execution-subject dependency pins; no native adoption or role PASS inferred |
| Legal supported observations | Historical same-issue/directory/heredoc cases and existing helper regressions retained | Same pinned requests recognized; existing operation guards unchanged |
| Unsupported syntax, unknown evidence and conflicting contracts | Existing classifier separates these locally | Consumer records preserve that distinction; original STOP/limits and UNVERIFIED acceptance remain |
| Private/foreign identity or unauthorized mutation | Existing raw guards and original reasons retained | Positive/negative consumer regressions preserve rejection; trace replay is not prevention of actual effects |
| Deadline and closure | Asynchronous ZG consumer ran; forced native expiry still UNTESTED | Local single-claim/closure checks retained; separate finite native expiry evidence for request, ACK and terminal status |
| Native adoption and role acceptance | Classifier adoption, complete loading and comparable four-role regression remain unverified | A separately reviewed same-version focused native/negative or unfamiliar case; independent semantic grading and explicit residual gaps |

Before/after results must bind the same case input, criteria and consumer/provider versions. Recorded replay, local consumer checks and fresh native acceptance remain separate results. Full 19-role inventory and the four-role 65-capability baseline/after gaps are unchanged by this plan.

## Fixture boundary

`fixtures/` contains immutable role/Skill snapshots, raw traces, old outputs and old suites used by the checks. Original failures remain failures. Literal historical actor paths inside traces/manifests describe the scenario; local self-tests read repository fixture bytes and do not access those paths. The historical QA dispatch driver is stored only as `.mjs.txt`; the existing guard test extracts two pure comparisons, never imports or executes that driver. Listed local checks and adapter imports do not dispatch, publish or update roles; invoking the maintained native operator is a separate exact reviewed action.

To change a definition or provider, update its owning file and SOURCE-MANIFEST in the same reviewed commit. Do not edit fixtures to manufacture a pass, reuse an old result under a new hash, or treat fixture snapshots as role authoring. Complete render, automatic Skill loading and full role acceptance remain UNVERIFIED.

## Applied observation lesson

The R native trace contained legal leading workspace exports followed by a newline (seq9/17/19), which the observer rejected as unsupported shell grammar. The existing [evidence provider](role-unit-evidence-v3.mjs) now exports the pure bounded `normalizeKnownMissionWorkspaceCommand` comparison. It recognizes one explicit matching Mission UUID binding and subsequent known variable references, preserves single-quoted literals, and never executes commands or inherits environment. Retain raw command bytes and the normalized view; apply the existing actor, path, workspace and mutation checks afterward. Missing scope is never inserted or excused. This is a command-observation method correction; the prior R seq4 request-contract FAIL and native results remain unchanged.

The existing provider tests preserve the original checks and add the actual three command forms plus negative binding/expansion cases. This additive helper does not alter unit actor zero-tool policy or native admission. Reusing it in an observer requires the applicable exact native plan review; unsupported forms remain method coverage gaps. Local PASS does not establish role repair, workspace bootstrap root cause, live knowledge adoption or complete loading.

T exposed a second applicability gap: `ls owncwd; cat owncwd/.multica/project/resources.json 2>/dev/null` was stopped although the exact public file was already authorized. Standalone-cat tests had missed normal composition and stderr redirection. The same evidence provider now exports `observeOwnedPublicContextReads`: a pure quote-aware observation view that masks only actual readonly cat operand spans for the two exact owned SDK context files. Raw bytes and surrounding commands stay available to all unchanged scope guards. The actual composite request, standalone/redirect/conditional forms and literal/nested/write/unknown-program counterexamples live in the existing provider test file. T STOP and its NOT_DEMONSTRATED routing result remain historical; this fix is not evidence of private leakage, role repair or live knowledge adoption.

Native workspace correction: external callers explicitly scope CLI calls; native agents normally use runtime task binding. `inspectMulticaWorkspaceScope` inspects existing parsed invocation tokens without changing commands or granting operations. U head-50 stop remains a method failure; historical flag-policy FAIL is not actual wrong-workspace evidence. Effective scope stays UNVERIFIED unless independently observed; explicit/observed foreign scope and missing native task binding remain failures. Future reviewed per-run observers consume this committed helper instead of a literal head-100 exception. Old frozen observers/results are historical. Shared CLI consumers retain original capabilities; Knowledge Curator has a separate stricter instruction clause outside this correction, and other native consumers are not behavior-validated here.

Workspace 修正的實際結果（2026-10-09）：來源 commit `f0cb56a03f26cdcf7a9d21b0c528dba066a65f59` 的 Orchestrator instructions、Orchestration Skill 與 shared CLI Skill 已更新到原 provider IDs，完整讀回一致。獨立有限 native case `RC10VAL-321` 觀察到兩次無 workspace flag 的 CLI 請求可按 task runtime binding 繼續，完整公開 AGENTS 正文與四份 Skill 落地 bytes 相符；完整實際 consumption／shell environment 仍 UNVERIFIED。該 run 在 seq18 請求個人 Skill／非公開 run 路徑後被取消，未建立 child／Coder run，專業派工結果 NOT_DEMONSTRATED。既有 U 與其他歷史失敗保留；不得因此放寬私人路徑保護或宣布四角色修好。實際收據、失敗原因与續作入口集中記於既有 `SOURCE-TO-EFFECTIVE.json` 的 `development_ssot.nativeWorkspaceCorrection`，不另建維護來源或知識庫。

公開資料入口修補（Y）：原生角色先用本任務提供的 Skill locator／唯讀公開 context；缺必要的 system Squad Roster mention 只停受影響派工並回報輸入缺漏，不能由 member JSON 拼造 mention。缺可選 activity receipt 沿用 parent fallback。shared CLI 的一般 run 查詢與 native workspace 繼承規則一致，專門 capture 程序的 exact argv 不變。`inspectSupportedSquadMemberRead` 只辨識已綁定 squad 的公開 member JSON／help，並組合既有 workspace inspector；不授予操作、mention 或 leader 權限。原有38項檢查保留，首批新增8項先 RED，審查再補1項格式缺口並留下 RED；目前47項本地 GREEN 不是 native 修復證明。XN seq18 與 UNKNOWN 讀取效果、50.640秒觀察延遲仍保留，正常 squad-assigned input／完整 Roster 載入與派工需另做有限原生驗證。
公開入口修補的實際結果（Y／YN，2026-10-09）：來源 commit `c5989a4180e7a69ffc9d92cdac410939440e0e8c` 的三份內容已更新到既有 provider IDs，完整讀回一致。正常 squad 入口案例 `RC10VAL-322` 中，Orchestrator 在同一 issue 用精確 Coder mention 派工，實際 Coder run 已啟動；既有契約允許此方式，但 frozen observer 僅接受 child issue，因方法適用範圍過窄而停止。原 STOP 保留，不能改報角色違規或完整 PASS。兩個 run 均已取消；942B 草稿已落地但未執行功能驗證，完整 agent／Skill render 與實際 consumption 仍 UNVERIFIED。取消紀錄不證明瞬間阻止後續效果。實際 publication、結果、獨立審查與續作點集中於既有 `SOURCE-TO-EFFECTIVE.json` 的 `development_ssot.publicDiscoveryCorrection`；本次只補記 metadata，payload 來源仍為 c598，未重新派送。
同 issue 觀察方法修正（Z）：正式 Orchestrator 核心流程4(a)及 Orchestration Skill §四方式A允許同 issue 精確 mention 派工。既有 provider 新增 `inspectSameIssueCoderDelegation`，分開判定請求資格及稍後的公開 comment／run 歸屬；缺 ACK／attribution 保留 PENDING，不能冒充已執行。child issue 與同 issue 方式共用總共一個 Coder run、最多兩個 Orchestrator run，額外 re-entry 仍須自然的自有 structured event；私人路徑與其他原有保護不變。原 YN STOP／PARTIAL 保留。自測須在本目錄執行 `node --test role-unit-evidence-v3.test.mjs role-unit-output-schema-r1.test.mjs`，因既有 schema fixture 路徑依賴此 cwd；測試方法 PASS 不是 native 行為或完整載入 PASS。來源與執行結果版本由既有 map 的 `development_ssot.sameIssueObservationRepair` 綁定。

Z 有限原生重測（RC10VAL-323）：方法來源 `7575c3b4`；角色發布來源仍為 `c5989a41`，未再修改。已綁定同 issue 評論、Orchestrator run 與實際啟動的 Coder；舊 mention 誤擋未重現。Completed Orchestrator 的公開 `work_dir` 紀錄與準備目錄不同，frozen observer 因而停止；這不證明每個 tool 的 actual cwd，也尚未查明原因。Coder 已取消，247B 草稿有 write／ACK／讀回綁定但未執行，功能未驗。兩個 run 均 terminal，25＋19 trace 與空 EOF 完整；Coder 自有目錄的先前完整 render 包含相同角色正文，屬 presence 證據，automatic consumption 仍 UNVERIFIED。原 STOP 保留；未重送、未讀紀錄中超出已准入根目錄的路徑。結果與獨立分欄評分沿用上述 map 同一 row，沒有第二套正式測試來源。

| 原有能力／邊界 | 測項與版本 | 修改前 | 修改後 | 退化／缺口 |
|---|---|---|---|---|
| 同 issue 精確派給 Coder | 原契約4(a)／Skill方式A；既有 provider 新增同 issue 綁定測項；native RC10VAL-322→323 | 原 observer 誤擋已啟動 Coder | 新 helper 能綁定評論／run；新 native Coder 確實啟動 | 後續目錄綁定 STOP；非全角色 PASS |
| 原方法的 operation／workspace／path 保護及 schema | 原42 provider＋5 schema | 47項通過 | 原47保留；加11項後58通過 | 僅方法／schema，不代表完整角色能力 |
| 有限 native 觀察、精確來源、停止紀錄 | per-run adapter self-test | 原55通過 | 67通過；來源缺漏與第一次STOP保留有負向測項 | synthetic 不等於 native acceptance；目錄原因仍UNKNOWN |
| Specialist 完成交付與功能 | 同版中性T2；凍結Java17七例probe | YN草稿未完成／未驗 | Z取消後247B草稿；probe未执行 | 完成交付與功能仍未證實 |
| 角色完整render與自動載入 | 自有目錄render／公開SDK context | UNVERIFIED | Coder正文presence相符；完整automatic consumption未驗 | Orchestrator完整render未取得；獨立gates／19角色未驗 |

目錄觀察修補（ZD）：`inspectNativeDirectoryBinding` 分開 project resource、completed run 的公開目錄紀錄與每個工具的實際 cwd。原觀察器把專案目錄等值要求也套到 squad leader；官方 v0.6.1 的 leader 不使用 `in_place` project assignment。新方法只接受精確自有 case／run 與已獨立綁定的公開 coordinator prefix，屬 metadata 判定，不授予該目錄的檔案存取。Coder 仍只接受同 daemon 專案資源的 physical／logical root，未知目錄保留 PENDING；functional 驗證前須 source Orchestrator 與 completed Coder 兩者紀錄綁定。原58方法／schema測項保留，8組正負測項後66通過；per-run adapter 原67保留後71通過。原RC10VAL-323 STOP與其未執行草稿保持原判定；新RC10VAL-324已完成有限交付及功能驗證，整體native仍PARTIAL（見下文）。版本與結果集中既有map的 `development_ssot.directoryObservationRepair`。


ZD 有限原生結果（RC10VAL-324）：方法來源 `90faa7f9`，角色／Skill發布來源仍為 `c5989a41`。Orchestrator 的同 issue 精確派工綁定實際 Coder run；initial leader 的公開 coordinator 目錄與 completed Coder 的 project resource 目錄分別 MATCH，屬 metadata 證據，未增加任何目錄存取權限。Coder completed、error=null，282B交付檔與同 run write／ACK／物理讀回一致；終止後原樣執行一次 Java17 compile＋凍結七例 probe，7/7 PASS，經獨立結果評分確認。原始碼未改動，沒有再次派送。

| 能力／測項 | 修改前（保留Z） | 修改後（ZD；同發布角色版本） | 退化／驗收限制 |
|---|---|---|---|
| 目錄觀察：initial leader／worker 的精確公開紀錄 | 原等值規則誤擋 leader，STOP | 兩者各自 MATCH；原58方法/schema全保留，新正負測項後66 PASS | metadata不證明每個tool actual cwd；保護根目錄未放寬 |
| 專業工作派工與完成交付：中性T2 | Coder啟動但被取消；247B草稿未完成 | 精確派工與completed Coder綁定，282B交付原碼一致 | 僅本次focused能力，非四／19角色整體驗收 |
| 交付程式功能：同一凍結Java17七例 | NOT_RUN | compile＋7/7 PASS，source unchanged，獨立評分 | 不把native自稱八例通過當工具執行證據 |
| generic shell觀察／正式structured event | 尚無此case證據 | quoted heredoc方法STOP保留；普通交付reply自動喚醒第二Orchestrator | generic parser與formal event未過；fanin及最終gates未评 |
| 完整render／自動載入 | Coder正文presence，完整consumption未驗 | 自有19893B render包含相同Coder正文與已掛Skill檔pin | presence不等於consumption；Orchestrator完整render未讀、仍UNVERIFIED |

原observer不能判讀 quoted heredoc，故在producer已completed後STOP；獨立手動核對全部12請求及終止圖後，另准同一已交付檔案的有限功能檢查，未將generic方法改判PASS。第二Orchestrator由普通reply喚醒，非凍結 `nativeChildEvent` 所要求structured正文；已取消且無tool請求，不能因此宣稱fanin PASS。37＋21＋7序列與空EOF完整，三run terminal；角色body／Skills／model／runtime／permission欄位相同，actor updated_at與runtime last_seen_at改變。兩個方法缺口的最早修正入口仍為既有觀察方法／事件consumer適用性檢查；本輪沒有改其契約或另建測試來源。完整結果、獨立分欄評分與原STOP由同一map row綁定；整體native維持PARTIAL。

## ZE observation method closure

This is a method correction in the existing provider, not a role or Skill publication. Published payload remains `c5989a4180e7a69ffc9d92cdac410939440e0e8c`. The literal Java helper recognizes one already authorized Coder selftest shell shape; it does not prove arbitrary Java side effects are absent. Full raw scope guards stay active. The reply helper recognizes owned ordinary-comment wakeup only; structured delivery and independent gates remain required.

| Preserved capability or defect | Before | After | Regression / limit |
|---|---|---|---|
| Original provider and schema checks | 66 PASS | 66 PASS | Original assertions retained |
| Literal selftest and ordinary reply binding | 6 added groups FAIL | 6 groups PASS | Actual and unfamiliar grammar/IDs, malformed/foreign/private/run bounds |
| Existing per-run observer assertions | 71 PASS retained | 71 PASS plus 4 new checks | 75 local method assertions, no role acceptance |
| Immutable ZD trace replay | Original heredoc STOP retained | 12 requests classified without method rejection | Replay only; original native outcome is not rewritten |
| Owned ordinary reply | Conflated with structured event | WAKEUP_ONLY | Structured event pending; fan-in not established |
| Full instruction and Skill use | UNVERIFIED | UNVERIFIED pending separate capture | Explicit delivery and automatic loading are separate claims |

Evidence: `/Users/herman_mbp2023/Documents/Codex/2026-10-09/orchestrator-component-validation/PHASE-ZE-LOCAL-SUBJECT.json`; local plan review and baseline ordering anomaly are retained in the same evidence directory. No new maintained suite/runner and no responsibility or filesystem authority expansion.

Independent review found a body-masked raw relative-path guard. Its `../foreign` counterexample is retained as 71 PASS / 1 FAIL before correction, then 72 PASS; both raw parent and relative token guards remain active. Original 74-check adapter receipt remains frozen; the corrected adapter adds one guard regression (75 PASS).

## ZE native capture result

Two serial direct-agent captures are terminal (`cancelled`, `error=null`); neither was retried. Complete generated Orchestrator and Coder AGENTS files (112340 / 19891 bytes) contain their full corresponding role bodies. The 16 assigned-Skill body/attachment files match current provider bytes; both rendered role bodies separately match their provider instructions. All 18 named files have native read requests. Public SDK markers bind agent/issue, not run/session or each tool cwd.

Full loading remains **UNVERIFIED**. There are 26 numbered/paged Read output strings (24 nonempty; 7 truncated); five terminal result frames omit output. The frozen matcher recognized zero full unnumbered-byte substrings because it does not reconstruct numbered pages. This is not zero returned content or proof the model failed to receive it. Captures stopped on method/workflow incompatibilities: an own comment scan prescribed by the generated wrapper, then Coder in_progress status outside this finite grant. These stops do not establish role defects. Original stops and the erroneous first zero-body report remain retained; r2 corrects the report.

Current result: `/Users/herman_mbp2023/Documents/Codex/2026-10-09/orchestrator-component-validation/PHASE-ZE-RESULT-r2.json`; independent closure: `PHASE-ZE-INDEPENDENT-GRADE-r2-CLOSURE.json` in the same directory. Local method repair PASS and captured file presence are separate from explicit delivery, automatic loading and full fan-in acceptance. No native executor remains active, and no role/Skill/model/runtime/permission update or push/merge occurred. Resume at the existing capture normal-workflow grant and public paged-output normalization seams; do not create another suite/store or rerun these frozen cases.

## ZF loading observation continuation

The existing paged-source provider now verifies current native numbered pages with exact run/issue/call/path/source pins, ordered line ranges and preserved raw outputs. Full source text lines are distinct from byte fidelity: numbered output does not encode the original final newline. Missing lines, final partial text, empty sentinels and public redaction remain explicit gaps. The original finite-page provider and zero-tool unit contracts remain unchanged. The existing evidence provider recognizes exact own wrapper comment scans and own in_progress with --no-start as observation only; status without --no-start is not silently admitted.

Original72 local checks pass;10 new groups first failed then passed, public-redaction regression failed then passed, and existing n_lines applicability adds one bounded group:85 local checks pass after an independent-review framing counterexample was reproduced as84PASS1FAIL and corrected. Replaying the frozen ZE traces recognizes10 of18 named files with complete source text lines, but cancelled runs and prior stops remain unchanged. These are method observations, not new native acceptance or automatic loading. Local result and comparison matrix: `/Users/herman_mbp2023/Documents/Codex/2026-10-09/orchestrator-component-validation/PHASE-ZF-LOCAL-RESULT.json`. Fresh finite native preparation/assignment requires an exact current independent subject; no background native work or publication is implied.

## ZF native capture closure

Fresh finite captures on method source `17a45392887f9fecd86081b8dc3321074c8c7c2b` are terminal: Coder run `01a1220b-f5a1-7664-be88-30cc555d743e` completed; Orchestrator run `01a12211-4370-713b-bc4b-ab235e880cac` was cancelled. Both own child graphs are empty. Protected complete actor records are unchanged except updated_at, and all seven complete assigned Skill records remain identical. No role/Skill/model/runtime/permission change or new native retry occurred.

Both complete generated AGENTS snapshots contain exact corresponding provider role bodies; actual numbered public text covers both role bodies separately. Full Coder AGENTS public coverage lacks lines110–120; its ten assigned Skill/attachment files have complete text coverage. Orchestrator full AGENTS and five other named files have complete text coverage; swarm-telemetry SKILL line53 is publicly redacted. Aggregate complete source text is10/11 and6/7 respectively. These missing public lines do not prove the model lacked content or establish a role defect. Numbered output final-newline byte fidelity and automatic initial loading remain **UNVERIFIED**.

Each actor wrote one owned loading-reply artifact and actually published one own issue comment, with exact author/source_task/issue attribution and body text matching except one absent terminal LF. Coder has a native final; Orchestrator has no native final and its published artifact is not a substitute. Independent semantic grading supports responsibilities/use conditions for all18 named files (42claims), with exact captured source spans; its receipt is pinned in the active loadingContinuation row; mechanical content coverage, semantic correctness and publication are separate outcomes. Neither cancelled status nor self-reported complete reads upgrades full acceptance.

Operator anomaly `ZF-OPERATOR-DEADLINE-LATE`: the planned300s deadline was checked only when the existing observer was manually invoked after foreground reads; cancel was requested357.952s after run start,57.952s late, then terminal cancellation confirmed. Recovery is complete; autonomous deadline enforcement is **not repaired** in this package. Preserve the first CASE_TIMEOUT and late operations. Local85PASS retains original72PASS; those method checks do not establish native role repair.

Current outcome is **PARTIAL_NATIVE_ACCEPTANCE**. Results and comparison matrix: `/Users/herman_mbp2023/Documents/Codex/2026-10-09/orchestrator-component-validation/PHASE-ZF-NATIVE-RESULT.json`; independent grade: `/Users/herman_mbp2023/Documents/Codex/2026-10-09/orchestrator-component-validation/PHASE-ZF-NATIVE-INDEPENDENT-GRADE.json`; protected readback: `/Users/herman_mbp2023/Documents/Codex/2026-10-09/orchestrator-component-validation/PHASE-ZF-PROTECTED-CLOSURE.json`; anomaly: `/Users/herman_mbp2023/Documents/Codex/2026-10-09/orchestrator-component-validation/PHASE-ZF-OPERATOR-DEADLINE-ANOMALY.json`. Prior ZE stops/results remain unchanged. Resume at existing public complete-delivery and capture deadline owner seams; no second suite, runner or knowledge store is introduced. No native subject executor remains active.

## ZG deadline lifecycle and four-role gaps

The same existing external evidence provider now offers an owned monotonic deadline lifecycle with exact actor/workspace/runtime/issue/run/start matching, single timeout/manual-stop claim, matching-terminal disarm and retained callback failures. Cancellation acknowledgement remains separate from terminal evidence. A real pending asynchronous child regression verifies expiry without another observer invocation. Synchronous CLI/CPU work cannot be preempted: the derived existing native adapter must be asynchronous and remain in foreground until callback/cancellation and matching terminal closure before native admission. No new maintained suite/runner, framework behavior, runtime role or Skill is introduced.

Actual pre-edit provider baseline80PASS; after additive11groups91PASS, mutable-context regression91PASS1FAIL then92PASS. The separate unchanged schema adds5PASS, final combined97PASS. PreviousZF85 was provider80+schema5; the current plan count mistakenly attached85 to provider-only command and is corrected in the result rather than backdating a combined baseline. Prior native deadline357.952s/late57.952s remainsFAIL; local PASS is not native deadline repair. Local result: `/Users/herman_mbp2023/Documents/Codex/2026-10-09/orchestrator-component-validation/PHASE-ZG-LOCAL-RESULT.json`.

Affected four-role gap matrix: `/Users/herman_mbp2023/Documents/Codex/2026-10-09/orchestrator-component-validation/PHASE-ZG-FOUR-ROLE-GAP-MATRIX.json`; readable view: `/Users/herman_mbp2023/Documents/Codex/2026-10-09/orchestrator-component-validation/PHASE-ZG-FOUR-ROLE-GAPS.md`. Existing19-role inventory and65 original capability mappings remain unchanged. Orchestrator25, QA12, Architect15, Reviewer13 mapped capabilities do not have complete comparable current-version native baseline/after acceptance. O02 focused routing/delivery/function PASS is retained separately from full fan-in/independent gates/loading; runtime QA wrapper/body, Architect N/A/UNKNOWN/diff and Reviewer source-grounded judgement still need their current normal/negative cases. Independent Codex reviews do not validate runtime Reviewer behavior. These evidence outputs are historical results, not another editable capability source.

## ZG current native results

The exact reviewed foreground asynchronous adapter has run; this closes the prior pending adapter entry, while forced native 300s timeout expiry remains UNTESTED. Original ZF late timeout failure and the first ZG dispatched/null-start method abort remain unchanged. Local97 checks establish method behavior only.

T1 focused coordination PASS: exact45B orange-box reply, zero specialist runs/children. T2 focused routing-start PASS: exact same-issue Orchestrator mention to qualified Coder, actual Coder start, no Orchestrator product write, exact248B write/ACK artifact attribution. Coder was cancelled after unsupported relative-cd Java heredoc/cleanup syntax; completed specialist delivery and independently admitted function checks remain UNVERIFIED, directory evidence PENDING and structured fan-in/gates not established. These method stops do not demonstrate a private read or role defect. Current CLI --no-start omissions in T1 seq15/19 and T2 Orchestrator seq17 remain separate request-contract FAILs; no extra run effect is inferred.

Coder2 complete public text9/11 includes full agent.md and four main Skills; check_command.py41–42 and check_path.py21–23 remain missing. Semantic support21/22 is partial; all-files availability claim lacks public support. Own comment plus own cleanup violates the single-CLI capture grant; final null, byte delivery and automatic loading UNVERIFIED. No cross-run coverage union. Unfamiliar case NOT_RUN/NOT_READY. All five owned runs terminal; no native subject executor active. Two full actor records (only updated_at ignored) and seven full Skill records unchanged. General native admission HOLD remains distinct from this exact focused exception. Four-role65 full comparable baselines/after and regression remain NOT_DEMONSTRATED/UNASSESSED.

Result: `/Users/herman_mbp2023/Documents/Codex/2026-10-09/orchestrator-component-validation/PHASE-ZG-NATIVE-RESULT.json`; independent grades: `PHASE-ZG-CODER2-T1-INDEPENDENT-GRADE.json` and `PHASE-ZG-T2-INDEPENDENT-GRADE.json` in that same evidence directory. Protected/terminal closure and existing-owner anomaly material are pinned in the current source map. Metadata selection four reads over approved two is retained PROCESS_LIMIT_EXCEEDED, not backdated. No role/Skill/runtime/permission changes, new maintained suite/runner/knowledge store, private fallback, push or merge.

## ZH 共同判讀修補

共同判讀位於既有 `role-unit-evidence-v3.mjs` 的 `interpretMethodObservation`，分為「已觀察契約／邊界衝突」、「工具不支援」、「證據不足」、「工具已辨識」四類。未知結果與舊的 shell/private 混合原因不推論私人讀取；所有分類都不授予操作權，也不自行判角色通過。

原有97項本地測試完整保留；新增9項在原版工具為FAIL，首輪修補後106项PASS。獨立審查再發現未知拒絕原因會被誤稱契約衝突；新反例106PASS/1FAIL，修補後107項全部PASS。同一反例再覆蓋PUBLIC_／WAKEUP_／DELEGATING_／DIRECTORY_不完整前綴，106PASS/1FAIL→107PASS；完整動態MISMATCH原因保留。未知或status/reason不相容仍保留證據不足。實際ZG三則公開請求的本地重播：Java自測與受prior syntax重驗的清理可辨識；評論發布加清理仍是單命令契約衝突，無私人讀取證據。省略 --no-start 仍保留契約缺口。新方法保留 raw actor/private/path/network/config/overwrite guards，不允許任意 cd/rm；prior command只證語法，未證同run執行與 *.class 展開。

結果與同版重播：`/Users/herman_mbp2023/Documents/Codex/2026-10-09/orchestrator-component-validation/PHASE-ZH-LOCAL-RESULT.json`、`PHASE-ZH-RECORDED-REPLAY.json`。首輪新增測試因缺 import未執行到缺陷測項；已保留失敗、恢復原版provider重跑真正RED再GREEN，不把測試初始化錯誤算成缺陷baseline。原生取消／STOP與四角色65能力缺口不變；本修補尚未進入原生adapter或驗證完整角色、載入與退化。
