import json
from pathlib import Path

from tooling.validation.graphify_runtime_probe import inspect_runtime


ROOT = Path(__file__).resolve().parents[1]


def test_four_active_truth_entries_exist_and_resolve():
    expected = {'PROJECT-OVERVIEW.md', 'FRAMEWORK-SPEC.md',
                'IMPLEMENTATION-PLAN.md', 'STATUS.json'}
    assert all((ROOT/name).is_file() for name in expected)
    status = json.loads((ROOT/'STATUS.json').read_text())
    assert status == {
        'current_focus': 'PKB-001',
        'phase': 'prototype',
        'framework_spec': 'FRAMEWORK-SPEC.md',
        'implementation_plan': 'IMPLEMENTATION-PLAN.md',
        'next_action': 'Verify real Graphify runtime and select an exact-revision calibration repository',
        'archived_documents_are_authority': False,
    }


def test_legacy_truth_surfaces_are_archived():
    for path in ('docs', 'governance', 'release', 'agent', 'README.md'):
        assert not (ROOT/path).exists()
    assert (ROOT/'archive/legacy-baseline/docs').is_dir()
    assert (ROOT/'archive/legacy-baseline/governance').is_dir()


def test_calibration_selection_is_exact_but_not_falsely_frozen():
    selection = json.loads((ROOT/'validation/pkb001/datasets/calibration-repository.json').read_text())
    assert len(selection['source_commit_sha']) == 40
    assert selection['source_ref_kind'] == 'IMMUTABLE_GIT_COMMIT'
    assert selection['status'] == 'SELECTED_NOT_FROZEN'


def test_graphify_discovery_does_not_assume_operations():
    discovery = json.loads((ROOT/'validation/pkb001/runtime/graphify-discovery.json').read_text())
    assert discovery['verification_status'] == 'NOT_VERIFIED'
    assert discovery['supported_operations'] == []
    assert discovery['api_assumptions'] == []


def test_runtime_probe_without_descriptor_makes_no_api_claim(tmp_path):
    executable = tmp_path/'graphify'
    executable.write_text('#!/bin/sh\n')
    executable.chmod(0o755)
    result = inspect_runtime(executable, None)
    assert result['runtime_found'] is True
    assert result['verification_status'] == 'DISCOVERED_NOT_VERIFIED'
    assert result['supported_operations'] == []
    assert result['api_assumptions'] == []
