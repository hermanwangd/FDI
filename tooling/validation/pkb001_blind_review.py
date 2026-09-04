#!/usr/bin/env python3
"""Build a deterministic, arm-blinded human review packet for PKB-001."""

import argparse
import hashlib
import json
from pathlib import Path
from typing import List, Optional


ACTIONS = ['ACCEPT', 'RENAME', 'MERGE', 'SPLIT', 'REJECT', 'ADD_MISSING']
OUTCOMES = ['SUPPORTED', 'PARTIALLY_SUPPORTED', 'UNSUPPORTED', 'DUPLICATE']
ZH_LABELS = {
    'Application structural responsibility': '應用程式入口與協調職責',
    'Feature structural responsibility': '功能交付流程職責',
    'Product structural responsibility': '產品語意與維護職責',
    'Shared structural responsibility': '共用執行元件職責',
    'Structural structural responsibility': '程式結構情報整合職責',
    'Validation structural responsibility': '驗證與評估職責',
    'Application delivery responsibility': '應用程式交付職責',
    'Feature delivery responsibility': '功能交付流程交付職責',
    'Product delivery responsibility': '產品語意交付職責',
    'Runtime delivery responsibility': '執行層交付職責',
    'Shared delivery responsibility': '共用元件交付職責',
    'Structural delivery responsibility': '程式結構情報交付職責',
    'Validation delivery responsibility': '驗證與評估交付職責',
    'Application implemented delivery capability': '應用程式實作與交付能力',
    'Feature implemented delivery capability': '功能流程實作與交付能力',
    'Product implemented delivery capability': '產品語意實作與交付能力',
    'Shared implemented delivery capability': '共用元件實作與交付能力',
    'Structural implemented delivery capability': '程式結構情報實作與交付能力',
    'Validation implemented delivery capability': '驗證與評估實作與交付能力',
    'Product Semantics Ownership': '產品語意所有權',
    'Exact-Revision Structural Intelligence': '精確版本程式結構情報',
    'Bounded structural code intelligence queries': '有範圍限制的程式結構查詢',
    'Structural binding evidence attestation': '程式結構版本綁定與證據證明',
    'Feature evidence discovery and realization planning': '功能證據探索與實作規劃',
    'Fail-closed validation and readiness gating': '失敗即阻擋的驗證與就緒檢查',
}


def build_blind_packet(run_id: str, outputs: list) -> tuple:
    proposals = []
    for output in outputs:
        for proposal in output.get('proposals', []):
            proposals.append(dict(
                proposal, arm=proposal.get('arm', output.get('arm')),
                source_kind='CODE_BASELINE'))
        for mapping in output.get('mappings', []):
            proposals.append({
                **mapping,
                'proposal_id': mapping['capability_id'],
                'label': mapping['capability_name'],
                'arm': None,
                'source_kind': 'FORWARD_SKILL',
            })
        for hypothesis in output.get('hypotheses', []):
            proposals.append({
                **hypothesis,
                'proposal_id': hypothesis['hypothesis_id'],
                'arm': None,
                'source_kind': 'REVERSE_SKILL',
            })
    ordered = sorted(proposals, key=lambda proposal: hashlib.sha256(
        (run_id + '\0' + proposal['proposal_id']).encode()).hexdigest())
    packet_items, key_items = [], []
    for index, proposal in enumerate(ordered, 1):
        blind_id = f'BR-{index:03d}'
        packet_items.append({
            'blind_id': blind_id,
            'candidate_capability': ZH_LABELS.get(proposal['label'], proposal['label']),
            'component_refs': proposal['component_refs'],
            'evidence_refs': proposal['evidence_refs'],
            'limitations': proposal['limitations'],
            'outcome': None,
            'review_action': None,
            'suggested_name': None,
            'reviewer_notes': None,
        })
        key_items.append({
            'blind_id': blind_id,
            'proposal_id': proposal['proposal_id'],
            'arm': proposal['arm'],
            'source_kind': proposal['source_kind'],
        })
    packet = {
        'packet_id': run_id + '-blind-review-v1',
        'review_status': 'AWAITING_HUMAN_INPUT',
        'instructions': 'Review each candidate without consulting evaluator ground truth.',
        'allowed_outcomes': OUTCOMES,
        'allowed_review_actions': ACTIONS,
        'items': packet_items,
    }
    key = {'key_id': run_id + '-blind-key-v1', 'visibility': 'EVALUATOR_ONLY',
           'items': key_items}
    return packet, key


def render_markdown(packet: dict) -> str:
    lines = [
        '# PKB-001 中文盲審表', '',
        '你的任務很簡單：逐項判斷「這是不是一個有用的產品能力，以及列出的程式是否真的支援它」。',
        '相似項目請各自判斷，因為它們來自不同但已隱藏的測試方法。不要查看 evaluator ground truth。', '',
        '結果怎麼填：', '',
        '- `SUPPORTED`：名稱與程式證據都合理。',
        '- `PARTIALLY_SUPPORTED`：部分合理，但證據或範圍不完整。',
        '- `UNSUPPORTED`：程式證據無法支持這個能力。',
        '- `DUPLICATE`：與另一項表達相同能力。', '',
        '動作怎麼填：', '',
        '- `ACCEPT`：接受這項候選。',
        '- `RENAME`：概念可用，但名稱要改。',
        '- `MERGE`：應與另一項合併。',
        '- `SPLIT`：範圍太大，應拆成多項。',
        '- `REJECT`：不是有用的產品能力。',
        '- `ADD_MISSING`：這裡反映出尚未列出的能力。', '',
    ]
    for item in packet['items']:
        short_components = [
            '/'.join(Path(path).parts[-2:]) for path in item['component_refs']]
        lines.extend([
            f"## {item['blind_id']} — {item['candidate_capability']}", '',
            f"- 主要程式（{len(item['component_refs'])}）：" +
            (', '.join(f'`{path}`' for path in short_components) or '無'),
            f"- 可追查證據：{len(item['evidence_refs'])} 項（完整內容在 JSON packet）",
            '- 注意：這只是候選，不能自行成為產品定義。',
            '- 結果（四選一）：',
            '- 動作（六選一）：',
            '- 建議名稱（只有 RENAME 時填）：',
            '- 備註：', '',
        ])
    return '\n'.join(lines)


def main(argv: Optional[List[str]] = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run-id', required=True)
    parser.add_argument('--input', action='append', type=Path, required=True)
    parser.add_argument('--packet-json', type=Path, required=True)
    parser.add_argument('--packet-markdown', type=Path, required=True)
    parser.add_argument('--key-json', type=Path, required=True)
    args = parser.parse_args(argv)
    outputs = [json.loads(path.read_text()) for path in args.input]
    packet, key = build_blind_packet(args.run_id, outputs)
    for path in (args.packet_json, args.packet_markdown, args.key_json):
        path.parent.mkdir(parents=True, exist_ok=True)
    args.packet_json.write_text(json.dumps(packet, indent=2, ensure_ascii=False) + '\n')
    args.packet_markdown.write_text(render_markdown(packet))
    args.key_json.write_text(json.dumps(key, indent=2) + '\n')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
