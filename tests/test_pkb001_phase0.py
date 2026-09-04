import hashlib
import json
import subprocess
from pathlib import Path

import pytest

from tooling.validation.pkb001_gate import evaluate_readiness


ROOT = Path(__file__).resolve().parents[1]


def test_gate_blocks_when_any_prerequisite_is_missing(tmp_path):
    result = evaluate_readiness(tmp_path, {})
    assert result['status'] == 'BLOCKED'
    assert [item['id'] for item in result['prerequisites']] == [
        'P0-01', 'P0-02', 'P0-03', 'P0-04', 'P0-05', 'P0-06', 'P0-07']
    assert all(item['status'] == 'MISSING' for item in result['prerequisites'])


def test_gate_never_treats_rc4_as_rc9(tmp_path):
    framework = tmp_path/'framework.md'
    framework.write_text('framework rc4')
    evidence = {'framework': {
        'version': 'v0.1-rc4',
        'path': 'framework.md',
        'sha256': hashlib.sha256(framework.read_bytes()).hexdigest(),
    }}
    result = evaluate_readiness(tmp_path, evidence)
    assert result['prerequisites'][0]['status'] == 'MISMATCH'


def test_gate_is_ready_only_with_all_verified_evidence(tmp_path):
    framework = tmp_path/'framework.md'
    framework.write_text('authentic external rc9 fixture')
    (tmp_path/'pk-s1/SKILL.md').parent.mkdir(parents=True)
    (tmp_path/'pk-s1/SKILL.md').write_text('# PK-S1')
    (tmp_path/'pk-s2/SKILL.md').parent.mkdir(parents=True)
    (tmp_path/'pk-s2/SKILL.md').write_text('# PK-S2')
    evidence = {
        'framework': {'version': 'v0.1-rc9', 'path': 'framework.md',
                      'sha256': hashlib.sha256(framework.read_bytes()).hexdigest()},
        'graphify': {'result': 'EXACTLY_BOUND', 'queryable': True,
                     'runtime_version': '1.0.0', 'wire_version': 'mcp-1',
                     'adapter_version': '0.4.8.3', 'repository_revisions': {'repo': 'a'*40},
                     'graph_sha256': 'b'*64, 'input_policy_sha256': 'c'*64},
        'skills': {'pk_s1_path': 'pk-s1/SKILL.md', 'pk_s2_path': 'pk-s2/SKILL.md'},
        'acquisition': {'status': 'VALIDATED'},
        'isolation': {'status': 'VERIFIED'},
        'metrics': {'status': 'FROZEN'},
        'resource_security': {'status': 'FROZEN'},
    }
    assert evaluate_readiness(tmp_path, evidence)['status'] == 'READY'


def test_repository_phase0_remains_blocked_without_external_evidence():
    result = subprocess.run(
        ['python3', str(ROOT/'tooling/validation/pkb001_gate.py'), '--root', str(ROOT)],
        text=True, capture_output=True,
    )
    assert result.returncode == 2
    assert 'BLOCKED' in result.stdout
    assert 'P0-01' in result.stdout and 'P0-04' in result.stdout
