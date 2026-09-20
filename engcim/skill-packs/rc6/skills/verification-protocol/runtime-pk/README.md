# pk/ — Structured PK Store（machine-readable authoritative knowledge）

本目錄是 Multica swarm 套件的 **Structured PK Store**：Product Knowledge 的機器可讀 authoritative store。它與 Multica ProductKB issues 的職責分離正式規格見 [`docs/pk-storage-and-governance.md`](../docs/pk-storage-and-governance.md)：

- **本 store（pk/）**：authoritative 知識本體——機器可查、git diff 可追蹤、schema 可驗證。
- **ProductKB issues**：governance 流程層——curation、conflict 裁決、gap 補齊、淘汰的人類工作流；不是知識本體。

## 目錄結構

```
pk/
  README.md            # 本檔
  semantics/           # Product Semantics 條目（每檔一個 capability 語意樹；YAML/JSON）
  architecture/        # Architecture Knowledge 條目（C4 模型；YAML/JSON）
  realization/         # Product Realization 映射（REALIZES 鏈；YAML/JSON）
  code-graph/
    nodes.jsonl        # 每行一個 node（Product/Capability/Component/Interface/Repository/Module/CodeEntity）
    edges.jsonl        # 每行一條 relation（9 種），每條必帶 evidence[]/confidence/validationState
  evidence/            # Evidence 記錄（source／provenance／retrieval timestamp；YAML/JSON）
  _schema/             # JSON Schema 檔 + validate_store.py
```

## Schema 索引

| 目錄 | Schema | 條目格式 |
|---|---|---|
| `semantics/` | [`_schema/semantics-entry.schema.json`](_schema/semantics-entry.schema.json) | 每檔一個條目（YAML 或 JSON） |
| `architecture/` | [`_schema/architecture-entry.schema.json`](_schema/architecture-entry.schema.json) | 每檔一個條目 |
| `realization/` | [`_schema/realization-entry.schema.json`](_schema/realization-entry.schema.json) | 每檔一個條目 |
| `code-graph/nodes.jsonl` | [`_schema/code-graph.schema.json`](_schema/code-graph.schema.json)（`$defs.node`） | JSONL，每行一個 node |
| `code-graph/edges.jsonl` | [`_schema/code-graph.schema.json`](_schema/code-graph.schema.json)（`$defs.edge`） | JSONL，每行一條 edge |
| `evidence/` | [`_schema/evidence-record.schema.json`](_schema/evidence-record.schema.json) | 每檔一筆 Evidence 記錄 |
| 共用欄位 | [`_schema/common.schema.json`](_schema/common.schema.json) | validationState／confidence／sourceClass 等 enum |

## 每個條目的必填 governance 欄位

- `id`：store 內唯一；前綴依類別（`sem:`／`arch:`／`real:`／`ev:`；code-graph node 用 `<NodeType>:`，edge 用 `<type>|<from>|<to>`）。
- `validationState`：`PROVISIONAL | VALIDATED | STALE | CONFLICTING`（生命週期規則見 `skills/pk-correlation-synthesis/SKILL.md`）。
- `confidence`：`official-document | meeting-consensus | personal-share | auto-observation`（對應操作手冊的「官方文件／會議共識／個人分享／中（自動觀察）」）。
- `evidence`：非空陣列，指向 `pk/evidence/` 記錄的 id。**無證據不入庫。**
- `governanceIssueRef`：對應的 Multica ProductKB governance issue（id 或 URL），雙向連結（issue 側以 `store_ref` 回指）；尚無 issue 時為 `null`。
- 另有 `sourceClass`、`updatedAt`、`supersedes`／`supersededBy`（STALE 取代鏈）等欄位，見各 schema。

## 驗證

```bash
python3 pk/_schema/validate_store.py            # 檢查必填欄位、enum、id 唯一、參照完整性
```

入庫鐵律：**條目必須通過 schema 驗證才允許入庫**（Curator 是 store 守門員，見 `agents/knowledge-curator.md`）；raw analyzer output（Observation）一律 PROVISIONAL 起，升級規則不變。

## 與管線的關係

Observation（`skills/pk-document-analysis`、`skills/pk-repository-analysis/scripts/analyze-repo.py` 產出）→ `pk-correlation-synthesis` 合併／衝突偵測 → **寫入本 store**（authoritative）→ **同時開／更新 governance issue**（流程層，curation／conflict／gap 工作流）。查詢路徑：機器（agent、scripts）直接讀本 store；人類流程（裁決、補齊、淘汰）走 ProductKB issues。細節見 `docs/pk-storage-and-governance.md`。
