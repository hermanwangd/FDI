import hashlib
import json
import shutil
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

import pytest

from tooling.validation import pkb001_blind_review as generator


ROOT = Path(__file__).resolve().parents[1]
PACKET_DIR = ROOT / 'validation/pkb001/task6-blind-review'
PACKET_PATH = PACKET_DIR / 'blind-review-packet.json'
KEY_PATH = PACKET_DIR / 'sealed-blind-key.json'
MANIFEST_PATH = PACKET_DIR / 'manifest.json'
INSTRUCTIONS_PATH = PACKET_DIR / 'reviewer-instructions.md'
VALIDATOR_PATH = PACKET_DIR / 'public_validate.py'
REPORT_PATH = ROOT / '.superpowers/sdd/IMPLEMENTATION-PLAN/task-6-report.md'


def load(path):
    return json.loads(path.read_text())


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def copied_task6_root(tmp_path):
    paths = [
        Path('.superpowers/sdd/IMPLEMENTATION-PLAN/task-6-brief.md'),
        Path('validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json'),
        Path('validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-manifest.json'),
        Path('validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-witness.json'),
        Path('validation/pkb001/artifacts/petclinic-graph-818c413.json'),
        Path('validation/pkb001/reverse-task5-pkb001_reverse_run/capability-hypotheses.json'),
        Path('validation/pkb001/reverse-task5-pkb001_reverse_run/manifest.json'),
        Path('validation/pkb001/reverse-task5-pkb001_reverse_run/provenance-witness.json'),
        Path('validation/pkb001/schemas/evaluator-judgment-v0.1.schema.json'),
        Path('validation/pkb001/task6-blind-review'),
    ]
    forward_manifest = load(
        ROOT / 'validation/pkb001/artifacts/'
        'petclinic-pk-s1-forward-run-818c413-manifest.json'
    )
    reverse_manifest = load(
        ROOT / 'validation/pkb001/reverse-task5-pkb001_reverse_run/manifest.json'
    )
    paths.extend(Path(path) for path in forward_manifest['visible_input_sha256'])
    paths.extend(
        Path(entry['path']) for entry in reverse_manifest['visible_input_allowlist']
    )
    for relative in dict.fromkeys(paths):
        source = ROOT / relative
        target = tmp_path / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        if source.is_dir():
            shutil.copytree(source, target, dirs_exist_ok=True)
        else:
            shutil.copy2(source, target)
    return tmp_path


def schema_signature(value):
    if isinstance(value, dict):
        return ('object', tuple(sorted((key, schema_signature(item)) for key, item in value.items())))
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


def test_task6_packet_accounts_for_every_valid_run_item_without_judgments():
    packet = load(PACKET_PATH)

    assert packet['review_status'] == 'AWAITING_JUDGMENTS'
    assert len(packet['items']) == 15
    assert [item['blind_id'] for item in packet['items']] == [
        f'BR-{number:03d}' for number in range(1, 16)
    ]
    assert len({item['blind_id'] for item in packet['items']}) == 15
    assert sum(item['complete_realization_proposed'] for item in packet['items']) == 14
    assert sum(not item['complete_realization_proposed'] for item in packet['items']) == 1
    assert all(item['judgment'] == packet['empty_judgment'] for item in packet['items'])


def test_task6_key_binds_exact_packet_and_keeps_source_identity_sealed():
    packet = load(PACKET_PATH)
    key = load(KEY_PATH)
    rendered_packet = PACKET_PATH.read_text()

    assert key['sealed_packet_sha256'] == digest(PACKET_PATH)
    assert len(key['items']) == len(packet['items']) == 15
    assert {item['blind_id'] for item in key['items']} == {
        item['blind_id'] for item in packet['items']
    }
    assert all(item['source_arm'] in {'FORWARD', 'REVERSE'} for item in key['items'])
    for forbidden in ('FORWARD', 'REVERSE', 'PKS1-', 'PET-CAP-'):
        assert forbidden not in rendered_packet


def test_task6_packet_record_shapes_do_not_uniquely_identify_source_arm():
    packet = load(PACKET_PATH)
    key = load(KEY_PATH)
    old_forward_shape = {
        'blind_id': 'BR-001', 'component_refs': ['src/Thing.java:L1'],
        'evidence_refs': {'structural': [{'graph_node_id': 'thing'}], 'delivery': []},
    }
    old_reverse_shape = {
        'blind_id': 'BR-002', 'component_refs': [],
        'evidence_refs': {'structural': {'graph_node_ids': ['thing']}, 'delivery': {'commit_shas': ['a']}},
    }

    assert uniquely_identifying_arm_signatures(
        [old_forward_shape, old_reverse_shape],
        [{'blind_id': 'BR-001', 'source_arm': 'FORWARD'}, {'blind_id': 'BR-002', 'source_arm': 'REVERSE'}],
    )
    assert not uniquely_identifying_arm_signatures(packet['items'], key['items'])
    assert all(item['component_refs'] for item in packet['items'])
    assert all(item['evidence_refs'] for item in packet['items'])


def test_task6_claims_only_deterministic_label_and_order_blinding():
    manifest = load(MANIFEST_PATH)
    instructions = INSTRUCTIONS_PATH.read_text()
    validator_report = load(PACKET_DIR / 'public-validation-report.json')
    active_truth = '\n'.join((ROOT / name).read_text() for name in (
        'PROJECT-OVERVIEW.md', 'FRAMEWORK-SPEC.md', 'BACKLOG.md',
        'IMPLEMENTATION-PLAN.md', 'STATUS.json',
    ))

    assert manifest['packet_sha256'] == digest(PACKET_PATH)
    assert manifest['blinding'] == {
        'scope': 'DETERMINISTIC_LABEL_AND_ORDER_BLINDING',
        'explicit_arm_labels_absent': True,
        'source_identifiers_absent': True,
        'content_level_arm_anonymity_claimed': False,
        'limitation': 'ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT',
    }
    assert 'ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT' in instructions
    assert 'ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT' in active_truth
    check_names = {row['name'] for row in validator_report['checks']}
    assert 'deterministic_label_and_order_blinding' in check_names
    assert 'content_arm_inference_limit_disclosed' in check_names
    assert 'arm_label_and_identity_blinding' not in check_names
    assert 'schema_signature_arm_blinding' not in check_names


def test_task6_packet_exposes_the_frozen_judgment_contract_and_non_human_limit():
    packet = load(PACKET_PATH)
    instructions = INSTRUCTIONS_PATH.read_text()

    assert packet['allowed_review_actions'] == [
        'ACCEPT', 'RENAME', 'MERGE', 'SPLIT', 'REJECT', 'ADD_MISSING'
    ]
    assert set(packet['judgment_dimensions']) == {
        'evidence_validity', 'usefulness', 'unsupported_claims', 'precision',
        'limitations', 'active_review_seconds',
    }
    assert 'expected realization scoring' in instructions.lower()
    assert 'Product Team' in instructions
    assert 'NON_HUMAN' in instructions
    assert 'cannot complete Product Team human review' in instructions


def test_task6_workspaces_retain_identical_inputs_and_isolation_after_review():
    packet = load(PACKET_PATH)
    workspace_paths = [
        PACKET_DIR / 'judgment-workspaces/reviewer-01',
        PACKET_DIR / 'judgment-workspaces/reviewer-02',
    ]

    for workspace in workspace_paths:
        workspace_packet = workspace / 'packet-input.json'
        template = load(workspace / 'judgment-template.json')
        assert workspace_packet.read_bytes() == PACKET_PATH.read_bytes()
        assert template['packet_sha256'] == digest(PACKET_PATH)
        assert len(template['judgments']) == len(packet['items']) == 15
        assert [row['blind_id'] for row in template['judgments']] == [
            item['blind_id'] for item in packet['items']
        ]
        assert all(row['review_action'] in packet['allowed_review_actions']
                   for row in template['judgments'])
        assert all(row['outcome'] in packet['allowed_outcomes']
                   for row in template['judgments'])
        assert template['reviewer_context']['actor_type'] == 'NON_HUMAN'
        assert template['reviewer_context']['can_complete_product_team_review'] is False
        assert template['reviewer_isolation']['other_workspace_future_judgments_accessible'] is False
        assert template['reviewer_isolation']['sealed_key_accessible'] is False
        assert template['entry_template']['blind_id'] == 'BR-###'
        assert template['entry_template']['review_action'] is None
        assert template['entry_template']['outcome'] is None
    assert packet['packet_id']


def test_task6_generation_refuses_to_overwrite_completed_judgments(tmp_path):
    root = copied_task6_root(tmp_path)
    judgment_paths = [
        root / 'validation/pkb001/task6-blind-review/judgment-workspaces'
        / reviewer / 'judgment-template.json'
        for reviewer in ('reviewer-01', 'reviewer-02')
    ]
    before = {path: path.read_bytes() for path in judgment_paths}

    with pytest.raises(
        generator.BindingError,
        match='completed judgments.*--output-dir',
    ):
        generator.write_task6_artifacts(root)

    assert {path: path.read_bytes() for path in judgment_paths} == before


def test_task6_cli_fails_closed_without_traceback_or_mutation(tmp_path):
    root = copied_task6_root(tmp_path)
    judgment_paths = [
        root / 'validation/pkb001/task6-blind-review/judgment-workspaces'
        / reviewer / 'judgment-template.json'
        for reviewer in ('reviewer-01', 'reviewer-02')
    ]
    before = {path: path.read_bytes() for path in judgment_paths}

    completed = subprocess.run(
        [
            sys.executable, str(ROOT / 'tooling/validation/pkb001_blind_review.py'),
            '--root', str(root),
        ],
        check=False,
        text=True,
        capture_output=True,
    )

    assert completed.returncode == 2
    assert 'refusing to overwrite completed judgments' in completed.stderr
    assert 'Traceback' not in completed.stderr
    assert {path: path.read_bytes() for path in judgment_paths} == before


def test_task6_generation_can_initialize_an_explicit_new_version(tmp_path):
    root = copied_task6_root(tmp_path)
    version_dir = Path('validation/pkb001/task6-label-order-review-v2')

    manifest = generator.write_task6_artifacts(root, version_dir)

    assert manifest['packet_id']
    assert load(root / version_dir / 'judgment-workspaces/reviewer-01/judgment-template.json')[
        'judgments'
    ] == []
    assert load(root / version_dir / 'judgment-workspaces/reviewer-02/judgment-template.json')[
        'judgments'
    ] == []


def test_task6_manifest_input_digests_and_public_validator_pass():
    manifest = load(MANIFEST_PATH)

    assert manifest['source_bindings']['shared_source_commit_sha'] == (
        '818c4136ea971c21674525f9053de0d9c7ad8cfe'
    )
    for entry in manifest['input_digests']:
        assert digest(ROOT / entry['path']) == entry['sha256']
    completed = subprocess.run(
        [sys.executable, str(VALIDATOR_PATH), str(ROOT)],
        check=False,
        text=True,
        capture_output=True,
    )
    assert completed.returncode == 0, completed.stdout + completed.stderr
    assert load(MANIFEST_PATH)['packet_sha256'] == digest(PACKET_PATH)


def test_task6_report_has_one_current_digest_set():
    manifest = load(MANIFEST_PATH)
    report = REPORT_PATH.read_text()
    stale_digests = {
        'bfb2e4da0bb4f975827f8fe9007f156f92717630f989330c6bb87873d5ea0fa2',
        '2db2b59e69ae6d4de565e5645a7ebf096c60d89b10c12c3c169f590132c5d25e',
    }

    assert report.count(manifest['packet_sha256']) == 1
    assert report.count(manifest['sealed_key_sha256']) == 1
    assert not any(digest in report for digest in stale_digests)
