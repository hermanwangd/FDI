import hashlib
import json
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PACKET_DIR = ROOT / 'validation/pkb001/task6-blind-review'
PACKET_PATH = PACKET_DIR / 'blind-review-packet.json'
KEY_PATH = PACKET_DIR / 'sealed-blind-key.json'
MANIFEST_PATH = PACKET_DIR / 'manifest.json'
INSTRUCTIONS_PATH = PACKET_DIR / 'reviewer-instructions.md'
VALIDATOR_PATH = PACKET_DIR / 'public_validate.py'


def load(path):
    return json.loads(path.read_text())


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


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


def test_task6_workspaces_receive_identical_packet_inputs_and_are_isolated():
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
        assert template['judgments'] == []
        assert template['reviewer_context']['actor_type'] == 'NON_HUMAN'
        assert template['reviewer_context']['can_complete_product_team_review'] is False
        assert template['reviewer_isolation']['other_workspace_future_judgments_accessible'] is False
        assert template['reviewer_isolation']['sealed_key_accessible'] is False
        assert template['entry_template']['blind_id'] == 'BR-###'
        assert template['entry_template']['review_action'] is None
        assert template['entry_template']['outcome'] is None
    assert packet['packet_id']


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
