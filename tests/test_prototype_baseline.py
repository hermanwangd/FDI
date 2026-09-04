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
        'next_action': 'Execute isolated PKB-001 forward and reverse experiment arms',
        'archived_documents_are_authority': False,
    }


def test_legacy_truth_surfaces_are_archived():
    for path in ('docs', 'governance', 'release', 'agent', 'README.md'):
        assert not (ROOT/path).exists()
    assert (ROOT/'archive/legacy-baseline/docs').is_dir()
    assert (ROOT/'archive/legacy-baseline/governance').is_dir()


def test_calibration_selection_is_exact_and_source_frozen():
    selection = json.loads((ROOT/'validation/pkb001/datasets/calibration-repository.json').read_text())
    assert len(selection['source_commit_sha']) == 40
    assert selection['source_ref_kind'] == 'IMMUTABLE_GIT_COMMIT'
    assert selection['status'] == 'FROZEN_SOURCE_SNAPSHOT'
    assert selection['graphify_binding_status'] == 'EXACTLY_BOUND'
    assert len(selection['source_tree_sha256']) == 64


def test_graphify_discovery_does_not_assume_operations():
    discovery = json.loads((ROOT/'validation/pkb001/runtime/graphify-discovery.json').read_text())
    assert discovery['verification_status'] == 'EXACTLY_BOUND'
    assert discovery['supported_operations'] == [
        'query_graph', 'get_node', 'get_neighbors', 'get_community',
        'god_nodes', 'graph_stats', 'shortest_path']
    assert discovery['api_assumptions'] == []
    graph = ROOT/discovery['snapshot_binding']['graph_path']
    import hashlib
    assert hashlib.sha256(graph.read_bytes()).hexdigest() == discovery['snapshot_binding']['graph_sha256']


def test_runtime_probe_without_descriptor_makes_no_api_claim(tmp_path):
    executable = tmp_path/'graphify'
    executable.write_text('#!/bin/sh\n')
    executable.chmod(0o755)
    result = inspect_runtime(executable, None)
    assert result['runtime_found'] is True
    assert result['verification_status'] == 'DISCOVERED_NOT_VERIFIED'
    assert result['supported_operations'] == []
    assert result['api_assumptions'] == []


def test_phase0_is_ready_only_after_all_six_flags_pass():
    report = json.loads((ROOT/'validation/pkb001/reports/phase0-readiness.json').read_text())
    assert report['status'] == 'READY'
    assert report['readiness_state'] == 'READY'
    assert report['readiness_flags'] == {
        'PRODUCT_SEMANTICS_FROZEN': True,
        'LIVE_GRAPHIFY_INTERFACE_VERIFIED': True,
        'PK_S1_EXECUTION_READY': True,
        'PK_S2_EXECUTION_READY': True,
        'CALIBRATION_DATASET_FROZEN': True,
        'GROUND_TRUTH_SEALED': True,
    }
