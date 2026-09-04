#!/usr/bin/env python3
"""Public-seam validation for the PKB-001 Task 6 blind comparison packet."""

import hashlib
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path


RUN_DIR = Path('validation/pkb001/task6-blind-review')
PACKET = RUN_DIR / 'blind-review-packet.json'
KEY = RUN_DIR / 'sealed-blind-key.json'
MANIFEST = RUN_DIR / 'manifest.json'
INSTRUCTIONS = RUN_DIR / 'reviewer-instructions.md'
FORWARD = Path('validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json')
FORWARD_MANIFEST = Path('validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-manifest.json')
FORWARD_WITNESS = Path('validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-witness.json')
REVERSE = Path('validation/pkb001/reverse-task5-pkb001_reverse_run/capability-hypotheses.json')
REVERSE_MANIFEST = Path('validation/pkb001/reverse-task5-pkb001_reverse_run/manifest.json')
REVERSE_WITNESS = Path('validation/pkb001/reverse-task5-pkb001_reverse_run/provenance-witness.json')
ACTIONS = ['ACCEPT', 'RENAME', 'MERGE', 'SPLIT', 'REJECT', 'ADD_MISSING']
DIMENSIONS = ['evidence_validity', 'usefulness', 'unsupported_claims', 'precision', 'limitations', 'active_review_seconds']


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load(path: Path):
    return json.loads(path.read_text(encoding='utf-8'))


def schema_signature(value):
    if isinstance(value, dict):
        return ('object', tuple(sorted(
            (key, schema_signature(item)) for key, item in value.items())))
    if isinstance(value, list):
        return ('array', tuple(sorted({schema_signature(item) for item in value})))
    if value is None:
        return 'null'
    return type(value).__name__


def uniquely_identifying_arm_signatures(items, key_items):
    arm_by_id = {item['blind_id']: item['source_arm'] for item in key_items}
    arms_by_signature = defaultdict(set)
    for item in items:
        arms_by_signature[schema_signature(item)].add(arm_by_id[item['blind_id']])
    return {
        signature: arms for signature, arms in arms_by_signature.items()
        if len(arms) == 1
    }


def complete_judgments(rows, packet_ids):
    return (
        isinstance(rows, list)
        and len(rows) == len(packet_ids)
        and [row.get('blind_id') for row in rows] == packet_ids
        and all(row.get('review_action') in ACTIONS for row in rows)
        and all(row.get('outcome') in {
            'SUPPORTED', 'PARTIALLY_SUPPORTED', 'UNSUPPORTED', 'DUPLICATE'
        } for row in rows)
        and all(isinstance(row.get('active_review_seconds'), int)
                and row['active_review_seconds'] >= 0 for row in rows)
    )


def validate(root: Path) -> dict:
    checks = []

    def check(name: str, condition: bool, detail: str) -> None:
        checks.append({'name': name, 'passed': bool(condition), 'detail': detail})

    packet = load(root / PACKET)
    key = load(root / KEY)
    manifest = load(root / MANIFEST)
    forward = load(root / FORWARD)
    forward_manifest = load(root / FORWARD_MANIFEST)
    forward_witness = load(root / FORWARD_WITNESS)
    reverse = load(root / REVERSE)
    reverse_manifest = load(root / REVERSE_MANIFEST)
    reverse_witness = load(root / REVERSE_WITNESS)

    check('input_digests', all(digest(root / entry['path']) == entry['sha256'] for entry in manifest['input_digests']), 'Every packet input matches its recorded SHA-256')
    check('shared_source_binding', forward['bindings']['source_commit_sha'] == reverse['source_binding']['source_commit_sha'] == manifest['source_bindings']['shared_source_commit_sha'] and forward['bindings']['graph_sha256'] == reverse['source_binding']['graph_sha256'] == manifest['source_bindings']['shared_graph_sha256'], 'Both runs bind the same source commit and graph digest')
    check('proposal_only_bindings', forward['authority'] == forward_manifest['authority'] == forward_witness['execution']['authority'] == 'PROPOSAL_ONLY' and reverse['authority'] == reverse_manifest['authority'] == reverse_witness['authority'] == 'PROPOSAL_ONLY', 'Every source record remains proposal-only')
    check('explicit_non_access_witness_booleans', forward['input_policy']['forbidden_inputs_accessed'] is False and forward_manifest['forbidden_inputs_accessed'] is False and forward_witness['forbidden_inputs_accessed'] is False and reverse_manifest['forbidden_inputs_accessed'] is False and reverse_witness['forbidden_inputs_accessed'] is False and reverse_witness['product_semantics_visible'] is False and reverse_witness['post_cutoff_knowledge_used'] is False, 'All source attestation booleans explicitly remain false')
    check('source_artifact_digests', forward_manifest['artifact_sha256'] == digest(root / FORWARD) and forward_witness['outputs']['artifact']['sha256'] == digest(root / FORWARD) and reverse_witness['primary_output_digests'][str(REVERSE)] == digest(root / REVERSE) and reverse_witness['manifest_sha256'] == digest(root / REVERSE_MANIFEST), 'Source artifact and manifest digests resolve')

    ids = [item['blind_id'] for item in packet['items']]
    arm_counts = Counter(item['source_arm'] for item in key['items'])
    check('exact_item_accounting', len(packet['items']) == 15 and ids == [f'BR-{number:03d}' for number in range(1, 16)] and len(set(ids)) == 15 and arm_counts == {'FORWARD': 10, 'REVERSE': 5} and sum(item['complete_realization_proposed'] for item in packet['items']) == 14, '9 forward mappings, 1 unresolved forward result, and 5 reverse hypotheses are all present')
    check('sealed_key_digest_binding', key['sealed_packet_sha256'] == digest(root / PACKET) and manifest['packet_sha256'] == digest(root / PACKET) and manifest['sealed_key_sha256'] == digest(root / KEY), 'Packet and separately sealed key digests bind exactly')
    rendered_packet = (root / PACKET).read_text(encoding='utf-8')
    check('arm_label_and_identity_blinding', all(value not in rendered_packet for value in ('FORWARD', 'REVERSE', 'PET-CAP-', 'PKS2-HYP-')), 'Packet contains neither arm labels nor source identifiers')
    check('schema_signature_arm_blinding', not uniquely_identifying_arm_signatures(packet['items'], key['items']) and all(item['component_refs'] and item['evidence_refs'] for item in packet['items']), 'No packet-only schema, type, or field-population signature identifies an arm')
    check('frozen_judgment_vocabulary', packet['allowed_review_actions'] == ACTIONS and packet['judgment_dimensions'] == DIMENSIONS and all(item['judgment'] == packet['empty_judgment'] for item in packet['items']), 'All 15 blank judgments use the frozen vocabulary and required dimensions')

    workspace_ok = True
    for workspace_id in manifest['reviewer_workspaces']:
        workspace = root / RUN_DIR / 'judgment-workspaces' / workspace_id
        template = load(workspace / 'judgment-template.json')
        workspace_ok &= (workspace / 'packet-input.json').read_bytes() == (root / PACKET).read_bytes()
        workspace_ok &= template['packet_sha256'] == digest(root / PACKET)
        workspace_ok &= (
            template['judgments'] == []
            or complete_judgments(template['judgments'], ids)
        )
        workspace_ok &= template['reviewer_context'] == {'actor_type': 'NON_HUMAN', 'authority': 'EVALUATOR_ONLY', 'can_complete_product_team_review': False}
        workspace_ok &= template['reviewer_isolation']['other_workspace_future_judgments_accessible'] is False
        workspace_ok &= template['reviewer_isolation']['sealed_key_accessible'] is False
    check('reviewer_isolation_contracts', workspace_ok, 'Both evaluator workspaces retain identical packet bytes, valid judgment state, and isolation contracts')
    instruction_text = (root / INSTRUCTIONS).read_text(encoding='utf-8')
    check('authority_instructions', 'Expected realization scoring is evaluator-only' in instruction_text and 'Only the human Product Team can finalize Product meaning' in instruction_text and 'cannot complete Product Team human review' in instruction_text, 'Instructions separate measurement from human Product meaning authority')
    check('task6_creation_boundary', manifest['decision_boundary'] == {'judgments_fabricated': False, 'product_team_human_review_completed': False, 'final_go_revise_stop_decision_made': False}, 'Task 6 creation manifest records no fabricated review or final decision')

    return {'schema_version': 'pkb001.task6.public-validation-report.v1', 'validation_scope': 'PACKET_AND_REVIEW_WORKSPACE_CONTRACT_ONLY', 'passed': all(check['passed'] for check in checks), 'checks': checks}


def main() -> int:
    arguments = [argument for argument in sys.argv[1:] if argument != '--write-report']
    root = Path(arguments[0]).resolve() if arguments else Path.cwd().resolve()
    report = validate(root)
    rendered = json.dumps(report, indent=2, sort_keys=True) + '\n'
    if '--write-report' in sys.argv[1:]:
        (root / RUN_DIR / 'public-validation-report.json').write_text(rendered, encoding='utf-8')
    print(rendered, end='')
    return 0 if report['passed'] else 1


if __name__ == '__main__':
    raise SystemExit(main())
