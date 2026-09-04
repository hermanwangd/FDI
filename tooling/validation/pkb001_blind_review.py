#!/usr/bin/env python3
"""Build deterministic, arm-blinded review material for PKB-001."""

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any, List, Optional


ACTIONS = ['ACCEPT', 'RENAME', 'MERGE', 'SPLIT', 'REJECT', 'ADD_MISSING']
OUTCOMES = ['SUPPORTED', 'PARTIALLY_SUPPORTED', 'UNSUPPORTED', 'DUPLICATE']
JUDGMENT_DIMENSIONS = ['evidence_validity', 'usefulness', 'unsupported_claims',
                       'precision', 'limitations', 'active_review_seconds']
TASK6_DIR = Path('validation/pkb001/task6-blind-review')
FORWARD_ARTIFACT = Path('validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json')
FORWARD_MANIFEST = Path('validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-manifest.json')
FORWARD_WITNESS = Path('validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-witness.json')
REVERSE_ARTIFACT = Path('validation/pkb001/reverse-task5-pkb001_reverse_run/capability-hypotheses.json')
REVERSE_MANIFEST = Path('validation/pkb001/reverse-task5-pkb001_reverse_run/manifest.json')
REVERSE_WITNESS = Path('validation/pkb001/reverse-task5-pkb001_reverse_run/provenance-witness.json')
TASK6_BRIEF = Path('.superpowers/sdd/IMPLEMENTATION-PLAN/task-6-brief.md')
JUDGMENT_SCHEMA = Path('validation/pkb001/schemas/evaluator-judgment-v0.1.schema.json')


class BindingError(ValueError):
    """A source run cannot safely be included in the blind comparison."""


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def sha256_file(path: Path) -> str:
    return sha256_bytes(path.read_bytes())


def json_bytes(value: Any) -> bytes:
    return (json.dumps(value, indent=2, ensure_ascii=False, sort_keys=True) + '\n').encode('utf-8')


def load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding='utf-8'))


def require(condition: bool, message: str) -> None:
    if not condition:
        raise BindingError(message)


def _verify_recorded_inputs(root: Path, entries: list[dict], label: str) -> None:
    for entry in entries:
        path = root / entry['path']
        require(path.is_file(), f'{label} input is missing: {entry["path"]}')
        require(sha256_file(path) == entry['sha256'], f'{label} input digest mismatch: {entry["path"]}')


def verify_forward_run(root: Path, artifact: dict, manifest: dict, witness: dict) -> None:
    artifact_path = root / FORWARD_ARTIFACT
    require(artifact.get('execution_kind') == 'SKILL_EXECUTION', 'forward execution kind is not a skill execution')
    require(artifact.get('run_status') == 'COMPLETED', 'forward run is not completed')
    require(artifact.get('authority') == 'PROPOSAL_ONLY', 'forward run is not proposal-only')
    require(manifest.get('authority') == 'PROPOSAL_ONLY', 'forward manifest is not proposal-only')
    require(manifest.get('artifact_sha256') == sha256_file(artifact_path), 'forward artifact digest is not bound')
    require(witness.get('outputs', {}).get('artifact', {}).get('sha256') == sha256_file(artifact_path), 'forward witness artifact digest is not bound')
    require(artifact.get('input_policy', {}).get('forbidden_inputs_accessed') is False, 'forward artifact did not attest non-access')
    require(manifest.get('forbidden_inputs_accessed') is False, 'forward manifest did not attest non-access')
    require(witness.get('forbidden_inputs_accessed') is False, 'forward witness did not attest non-access')
    require(witness.get('execution', {}).get('authority') == 'PROPOSAL_ONLY', 'forward witness authority differs')
    require(witness.get('assurance_level') == 'ATTESTATION_NOT_CRYPTOGRAPHIC_PROOF', 'forward witness assurance limit is missing')
    _verify_recorded_inputs(root, [{'path': path, 'sha256': digest} for path, digest in manifest.get('visible_input_sha256', {}).items()], 'forward manifest')
    bindings = artifact.get('bindings', {})
    for field in ('source_commit_sha', 'graph_sha256'):
        require(bindings.get(field) == manifest.get(field), f'forward {field} differs from manifest')
    results = artifact.get('capability_results', [])
    require(len(results) == manifest.get('capability_result_count') == 10, 'forward item count is not 10')
    require(sum(item.get('outcome') == 'MAPPING_PROPOSAL' for item in results) == 9, 'forward mapping count is not 9')
    require(sum(item.get('outcome') == 'UNRESOLVED' for item in results) == 1, 'forward unresolved count is not 1')
    require(all(item.get('mapping_status') == 'PROPOSAL_ONLY' for item in results), 'forward item authority is not proposal-only')


def verify_reverse_run(root: Path, artifact: dict, manifest: dict, witness: dict) -> None:
    artifact_path = root / REVERSE_ARTIFACT
    require(artifact.get('authority') == 'PROPOSAL_ONLY', 'reverse run is not proposal-only')
    require(artifact.get('human_review_required') is True, 'reverse run does not require human review')
    require(manifest.get('authority') == 'PROPOSAL_ONLY', 'reverse manifest is not proposal-only')
    require(witness.get('authority') == 'PROPOSAL_ONLY', 'reverse witness is not proposal-only')
    require(manifest.get('forbidden_inputs_accessed') is False, 'reverse manifest did not attest non-access')
    require(witness.get('forbidden_inputs_accessed') is False, 'reverse witness did not attest non-access')
    require(witness.get('product_semantics_visible') is False, 'reverse witness permits Product Semantics')
    require(witness.get('post_cutoff_knowledge_used') is False, 'reverse witness permits post-cutoff knowledge')
    require(witness.get('attestation_class') == 'ATTESTATION_NOT_CRYPTOGRAPHIC_PROOF', 'reverse witness assurance limit is missing')
    _verify_recorded_inputs(root, manifest.get('visible_input_allowlist', []), 'reverse manifest')
    require(witness.get('allowed_input_digests') == {item['path']: item['sha256'] for item in manifest.get('visible_input_allowlist', [])}, 'reverse witness input digests differ from manifest')
    require(witness.get('primary_output_digests', {}).get(str(REVERSE_ARTIFACT)) == sha256_file(artifact_path), 'reverse witness artifact digest is not bound')
    require(witness.get('manifest_sha256') == sha256_file(root / REVERSE_MANIFEST), 'reverse witness manifest digest is not bound')
    hypotheses = artifact.get('hypotheses', [])
    require(len(hypotheses) == 5, 'reverse hypothesis count is not 5')
    require(all(item.get('authority') == 'PROPOSAL_ONLY' for item in hypotheses), 'reverse item authority is not proposal-only')


def empty_judgment() -> dict:
    return {'outcome': None, 'review_action': None, 'suggested_name': None,
            'evidence_validity': None, 'usefulness': None, 'unsupported_claims': None,
            'precision': None, 'limitations': None, 'active_review_seconds': None,
            'reviewer_notes': None}


def _forward_candidate(result: dict) -> dict:
    return {'source_identifier': result['capability_id'], 'source_arm': 'FORWARD',
            'candidate_capability': result['name'],
            'complete_realization_proposed': result['outcome'] == 'MAPPING_PROPOSAL',
            'component_refs': [item['source_location'] for item in result.get('proposed_components', [])],
            'evidence_refs': {'structural': [{'graph_node_id': item['graph_node_id'], 'source_location': item['source_location']} for item in result.get('evidence_refs', [])], 'delivery': []},
            'confidence_score': result['confidence'], 'limitations': result['limitations'],
            'candidate_basis': result.get('reasoning', result.get('unresolved_reason', ''))}


def _reverse_candidate(hypothesis: dict) -> dict:
    refs = hypothesis['evidence_refs']
    return {'source_identifier': hypothesis['proposal_id'], 'source_arm': 'REVERSE',
            'candidate_capability': hypothesis['label'], 'complete_realization_proposed': True,
            'component_refs': [],
            'evidence_refs': {'structural': {'graph_node_ids': refs['graph_node_ids'], 'graph_edges': refs['graph_edges']}, 'delivery': {'commit_shas': refs['commit_shas'], 'pull_request_numbers': refs['pull_request_numbers'], 'changed_path_refs': refs['changed_path_refs']}},
            'confidence_score': hypothesis['confidence']['score'], 'limitations': hypothesis['limitations'],
            'candidate_basis': hypothesis['structural_delivery_convergence']}


def build_task6_packet(root: Path) -> tuple[dict, dict, dict]:
    """Build the public packet, sealed key, and non-recursive manifest."""
    forward, forward_manifest, forward_witness = (load_json(root / path) for path in (FORWARD_ARTIFACT, FORWARD_MANIFEST, FORWARD_WITNESS))
    reverse, reverse_manifest, reverse_witness = (load_json(root / path) for path in (REVERSE_ARTIFACT, REVERSE_MANIFEST, REVERSE_WITNESS))
    verify_forward_run(root, forward, forward_manifest, forward_witness)
    verify_reverse_run(root, reverse, reverse_manifest, reverse_witness)
    forward_binding, reverse_binding = forward['bindings'], reverse['source_binding']
    require(forward_binding['source_commit_sha'] == reverse_binding['source_commit_sha'], 'runs use different source commits')
    require(forward_binding['graph_sha256'] == reverse_binding['graph_sha256'], 'runs use different graph digests')
    candidates = [_forward_candidate(item) for item in forward['capability_results']]
    candidates.extend(_reverse_candidate(item) for item in reverse['hypotheses'])
    ordered = sorted(candidates, key=lambda item: hashlib.sha256(('pkb001-task6-blind-order-v1\0' + item['source_identifier']).encode('utf-8')).hexdigest())
    packet_items, key_items = [], []
    for number, candidate in enumerate(ordered, 1):
        blind_id = f'BR-{number:03d}'
        packet_items.append({'blind_id': blind_id, 'candidate_capability': candidate['candidate_capability'], 'complete_realization_proposed': candidate['complete_realization_proposed'], 'component_refs': candidate['component_refs'], 'evidence_refs': candidate['evidence_refs'], 'confidence_score': candidate['confidence_score'], 'limitations': candidate['limitations'], 'candidate_basis': candidate['candidate_basis'], 'judgment': empty_judgment()})
        key_items.append({'blind_id': blind_id, 'source_identifier': candidate['source_identifier'], 'source_arm': candidate['source_arm']})
    packet = {'schema_version': 'pkb001.task6.blind-review-packet.v1', 'packet_id': 'pkb001-task6-blind-comparison-v1', 'review_status': 'AWAITING_JUDGMENTS', 'allowed_outcomes': OUTCOMES, 'allowed_review_actions': ACTIONS, 'judgment_dimensions': JUDGMENT_DIMENSIONS, 'empty_judgment': empty_judgment(), 'items': packet_items}
    packet_digest = sha256_bytes(json_bytes(packet))
    key = {'schema_version': 'pkb001.task6.sealed-blind-key.v1', 'key_id': 'pkb001-task6-sealed-blind-key-v1', 'visibility': 'SEALED_KEY_CUSTODIAN_ONLY', 'sealed_packet_sha256': packet_digest, 'item_count': len(key_items), 'items': key_items}
    inputs = [TASK6_BRIEF, FORWARD_ARTIFACT, FORWARD_MANIFEST, FORWARD_WITNESS, REVERSE_ARTIFACT, REVERSE_MANIFEST, REVERSE_WITNESS, JUDGMENT_SCHEMA]
    manifest = {'schema_version': 'pkb001.task6.blind-review-manifest.v1', 'packet_id': packet['packet_id'], 'source_bindings': {'shared_source_commit_sha': forward_binding['source_commit_sha'], 'shared_graph_sha256': forward_binding['graph_sha256'], 'reverse_delivery_history_sha256': reverse_binding['delivery_history_sha256']}, 'input_digests': [{'path': str(path), 'sha256': sha256_file(root / path)} for path in inputs], 'packet_sha256': packet_digest, 'sealed_key_sha256': sha256_bytes(json_bytes(key)), 'item_accounting': {'forward_mapping_proposals': 9, 'forward_unresolved_results': 1, 'reverse_hypotheses': 5, 'total_packet_items': len(packet_items)}, 'reviewer_workspaces': ['reviewer-01', 'reviewer-02'], 'isolation': {'packet_contains_arm_identity': False, 'packet_contains_source_identifiers': False, 'sealed_key_in_reviewer_workspaces': False, 'future_judgments_shared_between_reviewers': False}, 'decision_boundary': {'judgments_fabricated': False, 'product_team_human_review_completed': False, 'final_go_revise_stop_decision_made': False}}
    return packet, key, manifest


def reviewer_instructions() -> str:
    return """# PKB-001 blind comparison instructions

This packet is a blinded comparison input. Do not infer an item source arm from its ID or position, and do not access the sealed blind key.

For each item, record one frozen review action (`ACCEPT`, `RENAME`, `MERGE`, `SPLIT`, `REJECT`, or `ADD_MISSING`) and an evidence outcome. Record evidence validity, usefulness, unsupported claims, precision, limitations, and active review time. Leave a clear note when a claim exceeds the supplied evidence.

Expected realization scoring is evaluator-only: it may compare a blinded item against separately sealed expected realizations for measurement. It must not expose those realizations to a Product Team meaning review and cannot create Product truth.

Product meaning judgment is different. Only the human Product Team can finalize Product meaning, accepted terminology, boundaries, merges, splits, or publication. Upcoming AI evaluator contexts are `NON_HUMAN`; they can assist with evaluator-only scoring but cannot complete Product Team human review.

Do not record a final GO / REVISE / STOP decision in either workspace.
"""


def reviewer_template(packet_digest: str, workspace_id: str) -> dict:
    return {'schema_version': 'pkb001.task6.blind-judgment-workspace.v1', 'workspace_id': workspace_id, 'packet_sha256': packet_digest, 'reviewer_context': {'actor_type': 'NON_HUMAN', 'authority': 'EVALUATOR_ONLY', 'can_complete_product_team_review': False}, 'reviewer_isolation': {'other_workspace_future_judgments_accessible': False, 'sealed_key_accessible': False, 'ground_truth_accessible_from_packet_workspace': False}, 'judgments': [], 'entry_template': {'blind_id': 'BR-###', **empty_judgment()}}


def write_task6_artifacts(root: Path, output_dir: Path = TASK6_DIR) -> dict:
    packet, key, manifest = build_task6_packet(root)
    destination = root / output_dir
    destination.mkdir(parents=True, exist_ok=True)
    (destination / 'blind-review-packet.json').write_bytes(json_bytes(packet))
    (destination / 'sealed-blind-key.json').write_bytes(json_bytes(key))
    (destination / 'manifest.json').write_bytes(json_bytes(manifest))
    (destination / 'reviewer-instructions.md').write_text(reviewer_instructions(), encoding='utf-8')
    packet_bytes = (destination / 'blind-review-packet.json').read_bytes()
    for workspace_id in manifest['reviewer_workspaces']:
        workspace = destination / 'judgment-workspaces' / workspace_id
        workspace.mkdir(parents=True, exist_ok=True)
        (workspace / 'packet-input.json').write_bytes(packet_bytes)
        (workspace / 'judgment-template.json').write_bytes(json_bytes(reviewer_template(manifest['packet_sha256'], workspace_id)))
    return manifest


# Retained as the legacy baseline seam used by the pre-Task-6 unit tests.
def build_blind_packet(run_id: str, outputs: list) -> tuple:
    proposals = []
    for output in outputs:
        for proposal in output.get('proposals', []):
            proposals.append(dict(proposal, arm=proposal.get('arm', output.get('arm')), source_kind='CODE_BASELINE'))
        for mapping in output.get('mappings', []):
            proposals.append({**mapping, 'proposal_id': mapping['capability_id'], 'label': mapping['capability_name'], 'arm': None, 'source_kind': 'FORWARD_SKILL'})
        for hypothesis in output.get('hypotheses', []):
            proposals.append({**hypothesis, 'proposal_id': hypothesis['hypothesis_id'], 'arm': None, 'source_kind': 'REVERSE_SKILL'})
    ordered = sorted(proposals, key=lambda proposal: hashlib.sha256((run_id + '\0' + proposal['proposal_id']).encode()).hexdigest())
    packet_items, key_items = [], []
    for index, proposal in enumerate(ordered, 1):
        blind_id = f'BR-{index:03d}'
        packet_items.append({'blind_id': blind_id, 'candidate_capability': proposal['label'], 'component_refs': proposal['component_refs'], 'evidence_refs': proposal['evidence_refs'], 'limitations': proposal['limitations'], 'outcome': None, 'review_action': None, 'suggested_name': None, 'reviewer_notes': None})
        key_items.append({'blind_id': blind_id, 'proposal_id': proposal['proposal_id'], 'arm': proposal['arm'], 'source_kind': proposal['source_kind']})
    return ({'packet_id': run_id + '-blind-review-v1', 'review_status': 'AWAITING_HUMAN_INPUT', 'instructions': 'Review each candidate without consulting evaluator ground truth.', 'allowed_outcomes': OUTCOMES, 'allowed_review_actions': ACTIONS, 'items': packet_items}, {'key_id': run_id + '-blind-key-v1', 'visibility': 'EVALUATOR_ONLY', 'items': key_items})


def main(argv: Optional[List[str]] = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path.cwd())
    parser.add_argument('--output-dir', type=Path, default=TASK6_DIR)
    args = parser.parse_args(argv)
    manifest = write_task6_artifacts(args.root.resolve(), args.output_dir)
    print(json.dumps({'packet_id': manifest['packet_id'], 'packet_sha256': manifest['packet_sha256']}, sort_keys=True))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
