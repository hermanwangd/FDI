# Swarm 1.1 Release Package Design

**Revision：v4，2026-10-04，with a 2026-10-05 user-approved installer-private addendum. 狀態：DRAFT_FOR_REVIEW；未凍結。使用者已批准此範圍內的離線實作、測試及既有非生產測試資源上的 bounded native acceptance；未批准 company production、deployment 或 release。**
**範圍：Admin 與 PO 安裝包、S01–S06 資產／契約，以及本機離線封裝與 per-Product binding contract。**
**本文件是 release packaging／installation design，不是 Swarm 架構重設。拓樸方向已核可；本版的 S01–S06/TestPlan 補充和 exact revision 仍待獨立審閱／凍結。Native provisioning 尚未實作或驗證。**

## 目標與審閱順序

交付可重複使用的 Swarm 安裝包：管理員先安裝共用平台，PO 再按 Product 建立並維護 ProductWorkspace。Product A、B 應可各自完成知識建立／維護、多 repo 分析、產品意圖與完整工程交付；共用程式升級不得覆寫產品知識或污染另一個 Product。

本文件的正式審批狀態彼此獨立：**DESIGN_REVIEW** → **DESIGN_FROZEN**（exact revision、scope amendment 決定及 master owner 記錄均可追溯）→ **PLAN_REVIEW** → **PLAN_APPROVED** → **READY_TO_IMPLEMENT**（另須選定執行方式，並備妥具體環境／權限／target 授權）→ implementation／驗證 → 獨立 release GO／NO-GO。使用者在 2026-10-04 批准有界離線實作；2026-10-05 又明確批准本 SW-01 範圍的 existing nonproduction native acceptance 和 exact-ID reuse/create config contract。這些授權不等同審閱／凍結本 v4、批准 Plan v6、記錄 master scope amendment、授予公司 production/deploy/release 或提供 provider-side operation grants。Master 仍沒有本 amendment 的 owner 記錄；DESIGN_FROZEN、PLAN_APPROVED、正式 READY_TO_IMPLEMENT 亦未記錄。每次 native mutation 仍須 fresh target-/operation-specific provider authority。凍結後的實質 design 變更須重審新 revision；plan 的實質變更亦須重審 exact plan revision。

**Revision record.** v4 延續 Design v3 對 Library Design v2（原始 SHA-256 `ea66190b6154e74bca20257ef1e6eea744009e76094fb07d03bfd4efb6a9ecb8`）的 topology 描述，補足 S01–S06 兩軸與 TestPlan 提前準備／S06 獨立執行邊界。核可 topology 方向：一個共用 PK／Knowledge Multica workspace，並在其中為每個 Product 設獨立治理 project；這不代表本文件已凍結。Master scope amendment 仍需 master owner 記錄；Implementation Plan v6 尚待審閱／批准。

**Approved target-resolution decision (2026-10-05, installer-private config/lifecycle).** The user approved exact-ID reuse as the default. A missing or mismatched binding blocks; it never silently creates or attaches by name. Creation requires an explicit create intent in the existing installer config and a fresh, operation-specific authority check. The plan must show the exact environment/workspace/project/repository/revision and whether each binding is reused or proposed for creation. Persist intent and before-state before a write; after a known or uncertain write, read back the exact returned ID and ownership before retry. Same-name/wrong-ID or ambiguous matches stop. A verified installer-owned receipt is required for later update, rollback or rerun; rerunning the same intent must not duplicate. Product installation never publishes or promotes PK content. Changing an installed binding goes through `product upgrade-plan` and a separate authorized execution. This decision updates the installer contract only; it does not create a public schema, role, service, or live provider grant, and it does not change the separate master amendment/design review status.

**現況證據。** 本工作分支的 topology-aware offline installer 在 2026-10-04 以 Java 17 通過 321/321 Maven tests；Admin／PO pair 的實際封裝與 determinism 證據記於本地 build record。這證明所列本機 contract、封裝、stage／prepare、receipt、path 與 lifecycle assertions，不證明 provider 資源建立、Multica adoption、provider ACL／readback、ProductKB／PK authority、native WK provenance、Cognee scope、actual loaded version 或 live S01–S06。Offline receipts 的 `actualLoadedVersion` 為 `null`，`nativeStatus` 為 `NOT_RUN`；native provisioning／verification／S01–S06 全部 `NOTRUN`。

既定需求：安裝包、按 Product 初始化、共用 Code／service／Swarm／PK 能力、完整 S01–S06 資產／契約與驗收邊界。下列六項是建議設計決策，需依 exact revision 獨立審閱。公司環境未指定是 live 執行／驗收前置條件，不捏造 provider IDs。

## Release Packaging 範圍修訂提案

現行 Swarm 1.1 master 的核准交付範圍仍為 D1–D3；**Release Packaging／installer 尚未由 master owner 記錄為新交付項**。使用者已批准本機、有界的離線實作與測試：Admin 共用平台包、PO 按 Product 綁定包、共同 compatibility BOM，以及 S01–S06 資產／契約與本機驗收證據。這項批准不修改 D1–D3 的語義或完成狀態、不授權公司安裝／發布，也不代替 master owner 記錄 amendment、owner、exact revision 與 acceptance 邊界。S01–S03 是按需求獨立觸發的 Product Knowledge 軸；S04–S06 是 Software Delivery 軸，健康交付依序走 IntentSpec、development、independent verification。不可把 S01–S03 強制接成通用流水線；也不新增 service／store／public schema、重設 Product 知識治理或其他平台架構。

## 決策一 兩個受眾包使用同一版本基準

**建議：兩個 zip，同一 distributionId、相容 BOM 與 release receipt。**

- **Admin Platform Package：** 版本固定的 Java runtime／installer、共用 contracts、Skills／roles／scenario templates、既有 provider bindings 的配置及健康查核、依賴清單、安裝／升級／復原 runbook。只安裝一次共用 payload；不同 Product 可 pin 不同已安裝版本。
- **PO Product Package：** 指向相容平台版本的薄 launcher、Product installation config／manifest 範例、初始化／驗證／升級／backup／restore／detach 流程、S01–S06 quickstart 和驗收指南。不攜帶公司產品知識、credentials 或重複共用 service。
- 可另打包兩個 zip 為同一 distribution download，但不改兩者的 ownership 與 compatibility 邊界。兩包不能獨立任意升版。

PO 包提供 Product 綁定、操作入口、S01–S06 使用與驗收指南；scenario 的來源 adapters、Product repos／PK、Multica issue／provider、實際執行結果及審查證據仍來自授權的外部環境與既有流程。包內有角色／Skill／scenario 資產不等於六個 scenario 已在該 Product 跑通；不得以 installer verify 取代各 scenario 的 current-run receipt。

Admin 包管理既有共用程式與 service binding；本設計不新增 PK Service、資料庫、dashboard、dispatcher 或公共 schema。現有 app 使用 Java 17／Spring Boot 3.4.1；新 installer 邏輯沿用 Java，shell 僅作 launcher，不擴充既有 Python wrapper。[source policy](https://github.com/hermanwangd/FDI/blob/37c901fbdbc5dcc0fc858be489f0c9b17e68e83b/AGENTS.md)

## 決策二 共用能力與 Product 資料分開

**Topology v3 direction:** 一個共用 PK／Knowledge Multica workspace 承載多個 Product 的治理 project。每個 Product（包括 `SWARM` Product）有自己的 project，命名慣例為 `PRODUCT/<productRef>`；`SWARM` 對應 `PRODUCT/SWARM`。ProductKB 指該 Product governance project 的治理／publication role 與 mapping，不再另建一個重複的 ProductKB project。這是目標設計，並非已讀回的 native topology。

一個 Product 可對應多個工程執行 Multica workspaces。每個 execution workspace binding 明列同一 Product identity、該 workspace 的 work project、同一個 Product governance project、Product PK Git repository 與 exact governed revision。多個 execution workspaces 可共用該 Product PK authority；不可因共用 PK 把一個 workspace 的 WK 搬入另一 workspace。WK 仍 local 於 originating execution workspace，並保留 owner/project/applicability/version/decision/freshness provenance。

Admin package 管理共用 workspace 與 Product governance project 的受控 provision／verify。Product package 只 bind/reuse exact 已存在的 Product project、execution workspace/work project 與 Product PK identity；它可 read-only preview 並指出缺少的 mapping，不建立 native resource。缺少 project 時由已授權 Admin/operator provision，再由 Product package readback。任何 create/update 先要求真實 authenticated principal、明確 target、operation-specific authorization 與 supported readback；資源名稱、config、actorRef、folder、fixture 或 stub 都不授權寫入。

**Historical topology distinction.** `RC10-KNOWLEDGE-DESIGN.md` v0.8-r3 §2–§4、§17、§23 及 §32.1（明言沿用一個 Knowledge Workspace Project 的 logical namespaces，不按 Product 建實體 project/workspace）、以及舊 master-plan P2-04 clauses 描述的 proposed single-project topology，未定義 per-Product physical governance projects。這些文件亦未證明任何 topology 已部署。Installer Design v4 記錄的是需協調納入 canonical knowledge/master docs 的新目標方向；不得回寫說成舊文件或目前 runtime 已採用。

**可共用：**
- 同一版本的 Swarm runtime、PK 處理／檢索程式、contracts、角色與 Skills templates、installer／verification helpers
- 已有 provider／service 的部署與連線管理；若已有共享 derived retrieval provider，仍需 explicit dataset／source／actor scope
- 正式共享 Swarm knowledge 僅按既有跨 scope 治理；不由 installer 自行批准

**每 Product 指定並保護：**
- Product identity、owner、正式 PK／Product Intelligence repository 及 exact governed revision
- 工程 repo／source／branch／SHA、可操作範圍、治理／publication policy
- squad／agent／role settings、runtime／model／overlay／scenario bindings
- runtime state、Mission evidence、backup 與安裝 receipts

PK 正文保存在 Product-scoped Git-backed Product Intelligence repository，與 reusable framework repo 及 Multica governance project 分離。各 Product execution workspace 綁定同一 Product 的 exact PK repository／revision 和自己的工程 repos，而不是從共用包複製 PK。Multica `PRODUCT/<productRef>` project 提供該 Product 的治理／publication surface；ProductKB 是此 project role/mapping，不代表其正文自動成為 approved Git PK truth。PK 處理能力共用，不代表不同 Product 共用正式 PK 正文。Cognee 僅為可重建、按 Product/actor/dataset scope 限定的 derived retrieval index／registry，不能成為第二權威或繞過 scope。[既有 Product Intelligence 決定](https://github.com/hermanwangd/FDI/blob/37c901fbdbc5dcc0fc858be489f0c9b17e68e83b/docs/specifications/framework/product-intelligence/PRODUCT-INTELLIGENCE-STORE.md)

S01／S02 現有 scenario 使用 ProductKB entries／labels／index issues；安裝時須按已批准的治理 policy 將這些 workflow artifacts 映射到該 Product 的 governance project，並映射到 Git-backed PI store 的 exact authoritative revision。此處是待驗證的 compatibility seam，不增設第二個 writer、不改 scenario contract，也不把 ProductKB 初始化當成 PK 發布。歷史 validation workspace 中的 Validation／ProductKB／WorkspaceKnowledge project IDs 只供該 validation case，不得複用為 production target 或推定 IDs。

WK 按 originating engineering Workspace 保持 local scope、owning project、applicableProjectRefs、version／decision／freshness。不能因同屬 Product 就自動跨 Workspace 存取或移植；同樣不能把 WK 升格為 Product truth。來源批准不授予目的 Workspace 權限。[現有 Workspace Learning mapping](https://github.com/hermanwangd/FDI/blob/37c901fbdbc5dcc0fc858be489f0c9b17e68e83b/engcim/bootstrap/overlays/multica/RC10-WORKSPACE-LEARNING.md)

## 決策三 安裝輸入輸出依賴與相容性

**Proposed Admin UX：** plan、install、verify、upgrade；輸入平台配置、package identity、platformRoot、既有 provider／runtime bindings。  
**Proposed PO UX：** plan、init、verify、upgrade、backup、restore、detach；輸入 Product config、platformRoot、releaseRef、productRoot、state／receipt／backup roots。

Product config 引用現有 repo source、Product Intelligence、runtime-composition、environment-state、model-selection 等格式；產品到實際 provider 的組合為 installer-private config，不發明 ProductWorkspace public schema。任何 actorRef 只作 expected identity comparison，不成為權限 grant。[既有 state templates](https://github.com/hermanwangd/FDI/tree/37c901fbdbc5dcc0fc858be489f0c9b17e68e83b/engcim/bootstrap/state-templates)

**目標輸出布局（native path remains unimplemented/unverified）：**
- platformRoot/releases/<distributionId>：immutable 共用 payload
- platformRoot/state：installed versions 與 Product version binding inventory
- 指定 Product 的 PK／PI repo：正式產品資料，與 payload 分離
- Product stateRoot：只有 fresh-readback 後保存的 exact installer-owned provider IDs、journal、既有 environment／composition state；未驗證的 returned ID 保留 `UNVERIFIED`，不可當作 ownership
- receiptRoot/<attempt>、backupRoot/<attempt>：保留每次操作及復原證據

禁止 state／data root 落在 immutable payload 裏；path canonicalization、symlink containment、archive traversal／duplicate entry 必須檢查。company Claude profile 才在該 Product root materialize .claude overlay；Codex profile 使用其實際 state／evidence 路徑。

BOM 記 package kind、distributionId、source SHA、Java JAR／template／Skill／profile digests、依賴 floors、internal config format 與相容矩陣。Product pin exact installed release，Admin 新增版本不自動更新所有 Product。不相容兩包在 mutation 前拒絕。Java 17、Maven／CLI／bash／jq 及 sealed external helper 的 Python／PyYAML 依賴明列；不以平台默認 model 或旧 README count 宣稱 exact identity。

package 不含 secrets、live .claude state、產品 PK／WK、local controls、NotebookLM private-only material 或未授權 private evidence；只收必要 templates／overlay source 與依賴閉包。NotebookLM 不作為 Admin／PO 安裝、公開 source、共用 KB 或 release acceptance 的輸入。舊 full candidate ZIP 不能直接當安裝包。[既有 import exclusion](https://github.com/hermanwangd/FDI/blob/37c901fbdbc5dcc0fc858be489f0c9b17e68e83b/engcim/swarm/docs/rc10/RC10-COMPANY-REPO-IMPORT-FOLDER-MAP.md)

若此設計獲准納入 repository，預期會維護相關文檔、metadata／index、import manifest 與 import-test 的預期項目數；以當時實際 generated inventory 核對，不手填舊計數。這是後續 documentation／metadata／test-count 工作，不在本次設計修訂執行，也不表示為了計數而修改 runtime code。

## 決策四 權限 ownership 冪等與隔離

**Admin：** 管理共用版本、已有 service 配置、核准 runtime／provider access。  
**PO：** 在已授權產品範圍初始化、操作 Mission、review／feedback、執行允許的產品 lifecycle；不能因此管理其他 Product／平台 grants。  
**Curator／治理 owner：** 依既有 policy 產生 proposal、決策與正式 knowledge；installer 不自行 PUBLISHED／APPROVED。  
**Reviewer／Verifier：** 獨立查核 exact revision 與必要 evidence。

安裝前 first provider operation 是 harmless read；每次 Multica call 明確帶 workspace ID。credentials 僅沿已授權安全環境／secret reference，禁止放在 package、config、argv、log／backup；新增 credentials／OAuth、權限或 security 變更須其適用批准。environment/config values 不構成 authorization。

現有 installer 以名称找 Swarm／ProductKB、使用默認 Workspace、寫 package-local label map，不能原樣當 safe by-product installer。新流程保存 verified owned IDs；同名但 ownership／scope 不明即停止，不自動接管。重裝相同 Product／版本應保留 IDs，不重複建立、不改另一產品；共享 Skills 不被另一 Product 的 init 無聲更新。[實際 setup.sh](https://github.com/hermanwangd/FDI/blob/37c901fbdbc5dcc0fc858be489f0c9b17e68e83b/engcim/swarm/baselines/rc6/runtime/package/engcim-swarm-package-RC6/setup.sh)

plan／dry-run 可讀取必要狀態，禁止建立／修改資源或資料。partial／unknown write 先 reconcile native state，不 blind retry；local lock／journal 保障本機 writer，不宣稱 provider exactly-once。

隔離以 actual actor／provider ACL／runtime access boundary 驗證。A-only actor 不可 query／read／write B；admin 若本就獲准管理兩者，須與受限 worker 分開測試。different directories、同 Mac uid 下的 worktree、labels 或 prompt 均不單獨建立 isolation；能力不足時拒絕 shared mapping，提出待批准的受支持方案。

## 決策五 升級備份復原與解除安裝保留資料

- Admin shared upgrade：新 release side-by-side，檢查 identity／compatibility／loadability；不切換產品 pin。
- Product upgrade：先確認 active runs／concurrent drift、保存 exact known-good package／configuration、owned IDs、PK revisions 與必要資料／evidence，再 stage／readback／smoke／切換；不自動改 semantic truth 或知識 policy。
- Backup：可讀回並校驗所需產品資料／配置／evidence，不包含 credentials。provider export／restore 無法證明時，明列不支持並阻止依赖該承諾的 upgrade，不能只備份 shell 文件卻聲稱可完全恢复。
- Restore：以 exact backup／version／digest 恢復並重新 readback／smoke，保留失敗與修正 attempt；A 的復原不影響 B。
- Detach：解除或停用該 Product 的 owned runtime bindings，保留 PK、WK、governance issues、Mission history、evidence、backups；本設計沒有 purge。共用 release 不因 A detach 被刪除。

沿用既有 activation／last-known-good／rollback 規則；installer exit 0 不等於實際可用，不 blind unzip 或猜 restart method。[現有 Runtime Lifecycle](https://github.com/hermanwangd/FDI/blob/37c901fbdbc5dcc0fc858be489f0c9b17e68e83b/engcim/bootstrap/overlays/claude/engcim/skills/RUNTIME-LIFECYCLE-RUNBOOK-v0.1.md)

## 決策六 S01–S06 兩軸交付與下游 acceptance

**Product Knowledge 軸：S01–S03 按實際工程問題獨立觸發，不組成固定前置鏈。**
- S01：授權來源經各 channel 形成帶 provenance 的 PROVISIONAL Observation，再由 Curator 按治理產生 PK entry／index；未核准知識不當 trusted truth
- S02：以現有來源 adapters 重查 STALE／CONFLICTING／新版資料；每項有 disposition、supersession／conflict links、index 與 freshness 證據
- S03：manifest 中每個 repo 有 Observation／revision；cross-repo relation 只在具證據時建立。單一 repo、無證據的零 edge 是有效結果；不可為滿足圖形而捏造跨 repo 關係

**Software Delivery 軸：S04 → S05 → S06。Product Context 可由已核准的 S01／S02／S03 證據提供；scenario 不因這個資料來源關係而被強制派送。**
- S04：PM 在授權 Product Context／PK 上作 Intent Analysis／Challenge，留下 gap／clarification 與可驗收意圖，輸出 **Intention Spec**，不把 PRD 當必備輸出
- S05：以核准 requirement／IntentionSpec 與 exact repo／PK context 開始；適用時由 Architect 定義／確認 SPEC、interfaces、testability 與驗收條件。TestPlan v1 是供 S06 提前準備的測試規劃，不代表 S06 已執行。Selected Dev 以 incremental red-green-refactor 完成 implementation／integration／developer build、unit、local-smoke self-check；彙總 exact candidate、updated TestPlan、self-check、limitations／open findings，parent 停在 `in_review` 並交接 S06。
- S06：由獨立 QA／Verifier 執行 TestPlan、記錄 exact-candidate verification evidence、findings／coverage，Reviewer 依實際報告與 required evidence 作獨立 review，final QA 維持下游 gate。TestPlan v1 至少列 requirements、critical scenarios、expected results、environment、roles、open items；詳情可隨 S05 實作收斂。Material design／revision changes 使受影響測項／結果 stale；保留舊 receipts 並為 affected cases 重驗。只在 material correctness／security uncertainty 影響特定 work 時阻擋該 work，其餘獨立工作可繼續。

共通 scenario surface 是 Product-scoped、`[Sxx]` 前綴且指派 Swarm 的 Multica issue；REQUIRED children 完成及 revision-bound fan-in 後才交 human review，只有 human 設 `done`。每個 scenario 的角色／Skills、inputs、outputs、quality gates 以 pinned canonical scenarios 為準，PO quickstart 須逐一列明；adapter 可用性與 current-run receipts 須在實際 Product 驗收，不能由 packaging inventory 推定。Scenario Definition／TestPlan/TestReport 是既有治理文件；安裝包只引用 exact owner-controlled revisions，不複製或另立 SSOT。

**品質門與 scenario boundary 分開：** S05 developer self-check 不代替 S06 independent verification。canonical S05 的交付終點是 implementation／integration／self-check／`in_review` 與 S06 handoff；S06 的獨立 QA／Verifier execution、Reviewer verdict 與 final QA 是下游 gate。若選用既有 RC10 reviewed-delivery profile，依其 C1–C5／非作者 Design Review／Code Review、fixed candidate 及 S06 evidence 要求留下 exact-revision evidence；未選用不得默認該 profile 已運行。C1–C5 僅對明確選用的 profile 條件適用，不重解 sealed RC6。S01–S06 scenario 定義在封裝中是參考資產，不是 live PASS；native S01–S06 及 release GO 均需當前 exact-target evidence。[S01–S06 scenarios](https://github.com/hermanwangd/FDI/blob/37c901fbdbc5dcc0fc858be489f0c9b17e68e83b/engcim/swarm/baselines/rc6/runtime/package/engcim-swarm-package-RC6/docs/scenarios.md)、[conditional reviewed delivery profile](https://github.com/hermanwangd/FDI/blob/37c901fbdbc5dcc0fc858be489f0c9b17e68e83b/engcim/bootstrap/overlays/multica/RC10-S05-S06-ROLE-GUIDANCE.md)

**安裝／lifecycle acceptance：**
1. 同一 release 安裝 A、B，各自綁定與資料正確
2. 同版重装 A 不重複資源；同名不明資源拒絕接管
3. partial install／unknown response／restart 可 reconcile
4. A-only actor 的跨 Product query／read／write 在實際 boundary 被拒絕
5. Admin 新增版本，A／B pin 不變；升級 A 不改 B
6. upgrade A 失敗→restore exact known-good，知識及歷史保留
7. detach A 保留資料，B 仍可用
8. 不相容包、stale bindings、archive escape、secrets 均被拒絕
9. PO 能提出需求、看進度／阻塞、review 交付、給 revision feedback；Human DONE 不由 agent 自行設定

release GO 必須涵蓋：兩包 BOM／integrity、完整依賴、actual configured／loaded version、required tests／CI、A/B 全功能及負向／復原驗收、PO／Admin sign-off 與 support ownership。source、stored config、actual run、independent verification 分列；較早 runtime receipt 不轉稱新包 PASS。

## 假設 未決環境與發布限制

**設計假設：** 同一版本系列／兩個受眾包；一個共用 PK／Knowledge Multica workspace 內，每 Product 有一個治理 project；一個 Product 可綁多個 engineering execution workspaces；完整 S01–S06 資產與驗收邊界；沿用 supported Multica／Java 路徑及治理，不新建 UI。拓樸方向已獲使用者核可，但 exact design 仍待 review/freeze。

**執行前需指定：** Product A/B 的測試 repo／PK store／Multica mapping、Admin／PO／governance owner、部署 host／OS／runtime、實際權限與可用來源；需查證 supported isolation、provider exports、activation 與 exact loaded identity。可先完成設計審阅，不把未定值塞成假 facts。

現有 runtime 與最新 source identity 不一致；公司 target、隔離／authority／恢復能力尚未證明，因此 10/9 是目標 go／no-go，不是保證發布。Q1 的 strict admission、歷史 toolchain 與 causal efficacy 另行評估；不用 rehearsal 或單次 triplet 宣稱 complete v1.1 effectiveness。

**設計自檢：** 六項決策涵蓋 audiences、shared/Product/WK boundary、inputs/outputs/dependencies/version、authority/ownership/isolation、lifecycle/data preservation、S01–S06 兩軸與下游 gate。Topology 明定共用 PK／Knowledge workspace、各 Product governance project、多 execution workspace binding、單一 Product PK authority 與 workspace-local WK provenance；沒有加入新 service/store/public schema，沒有把環境變數當 grants，也沒有把 Product installation scope 等同單一 execution workspace。此 exact Design v4 及 Plan v5 仍待審閱；master scope amendment 尚無 owner 記錄。即使兩份文檔獲批，native operation 仍須另有 target-/operation-specific authority 和 live acceptance。
