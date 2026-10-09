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

From this directory, with Node heap limited to 256 MiB:

```sh
node --max-old-space-size=256 --test role-unit-evidence-v3.test.mjs role-unit-output-schema-r1.test.mjs
node --max-old-space-size=256 qa-own-issue-harness-r4.unit.mjs
node --max-old-space-size=256 qa-reused-fixture-scope-r3.unit.mjs
```

These are existing external test-provider assets, not a new FDI runner or framework CLI. Their self-tests validate local checker behavior only. Java runtime/tests stay in `engcim/swarm/src/`; native role runs and independent semantic grading retain their existing procedures.

## Fixture boundary

`fixtures/` contains immutable role/Skill snapshots, raw traces, old outputs and old suites used by the checks. Original failures remain failures. Literal historical actor paths inside traces/manifests describe the scenario; local self-tests read repository fixture bytes and do not access those paths. The dispatch driver is stored only as `.mjs.txt`; the existing guard test extracts two pure comparisons, never imports or executes the native driver. No commands here dispatch, publish or update roles.

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

目錄觀察修補（ZD）：`inspectNativeDirectoryBinding` 分開 project resource、completed run 的公開目錄紀錄與每個工具的實際 cwd。原觀察器把專案目錄等值要求也套到 squad leader；官方 v0.6.1 的 leader 不使用 `in_place` project assignment。新方法只接受精確自有 case／run 與已獨立綁定的公開 coordinator prefix，屬 metadata 判定，不授予該目錄的檔案存取。Coder 仍只接受同 daemon 專案資源的 physical／logical root，未知目錄保留 PENDING；functional 驗證前須 source Orchestrator 與 completed Coder 兩者紀錄綁定。原58方法／schema測項保留，8組正負測項後66通過；per-run adapter 原67保留後71通過。原RC10VAL-323 STOP與其未執行草稿保持原判定，新native尚未執行；版本與結果集中既有map的 `development_ssot.directoryObservationRepair`。
