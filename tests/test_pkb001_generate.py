import pytest

import json

from tooling.validation.pkb001_generate import generate_arm, main


SHA = 'a' * 40
GRAPH_SHA = 'b' * 64


def structure():
    return {
        'source_commit_sha': SHA,
        'graph_sha256': GRAPH_SHA,
        'nodes': [
            {'id': 'product-semantics', 'label': 'ProductSemantics.java',
             'source_file': 'src/main/java/example/product/ProductSemantics.java',
             'source_location': 'L1'},
            {'id': 'graphify-adapter', 'label': 'GraphifyAdapter.java',
             'source_file': 'src/main/java/example/structural/GraphifyAdapter.java',
             'source_location': 'L1'},
        ],
        'links': [],
    }


def test_forward_maps_frozen_capability_only_to_in_boundary_graph_files():
    semantics = {
        'status': 'FROZEN', 'owner': 'PRODUCT_TEAM',
        'applicable_source_commit_sha': SHA,
        'capabilities': [{
            'capability_id': 'CAP-1', 'name': 'Product ownership',
            'description': 'Human owned meaning',
            'expected_realization_boundary': ['src/main/java/example/product/'],
        }],
    }
    result = generate_arm('F1', {'structure': structure(), 'semantics': semantics})
    assert result['authority_status'] == 'PROPOSAL_ONLY'
    assert result['proposals'][0]['target_id'] == 'CAP-1'
    assert result['proposals'][0]['component_refs'] == [
        'src/main/java/example/product/ProductSemantics.java']
    assert result['proposals'][0]['evidence_refs'] == [
        'graph-node:product-semantics@L1']


def test_reverse_generation_rejects_product_semantics():
    with pytest.raises(ValueError, match='input categories'):
        generate_arm('R1', {'structure': structure(), 'semantics': {}})


def test_r3_requires_structural_and_delivery_evidence_to_converge():
    history = {
        'status': 'FROZEN', 'source_commit_sha': SHA,
        'history_cutoff': '2026-01-01T00:00:00Z',
        'post_cutoff_knowledge_policy': 'EXCLUDE_AFTER_CUTOFF',
        'commits': [{
            'commit_sha': 'c' * 40,
            'subject': 'add product validation',
            'changed_paths': ['src/main/java/example/product/ProductSemantics.java'],
        }],
    }
    result = generate_arm('R3', {'structure': structure(), 'history': history})
    assert len(result['proposals']) == 1
    proposal = result['proposals'][0]
    assert proposal['authority_status'] == 'PROPOSAL_ONLY'
    assert proposal['evidence_refs'] == [
        'git-commit:' + 'c' * 40,
        'graph-node:product-semantics@L1',
    ]


def test_one_commit_can_supply_delivery_evidence_to_multiple_areas():
    history = {
        'status': 'FROZEN', 'source_commit_sha': SHA,
        'history_cutoff': '2026-01-01T00:00:00Z',
        'post_cutoff_knowledge_policy': 'EXCLUDE_AFTER_CUTOFF',
        'commits': [{
            'commit_sha': 'c' * 40, 'subject': 'cross-area change',
            'changed_paths': [
                'src/main/java/example/product/ProductSemantics.java',
                'src/main/java/example/structural/GraphifyAdapter.java',
            ],
        }],
    }
    result = generate_arm('R3', {'structure': structure(), 'history': history})
    assert [proposal['target_id'] for proposal in result['proposals']] == [
        'product', 'structural']


def test_root_fdi_java_files_are_grouped_as_application_not_filenames():
    history = {
        'status': 'FROZEN', 'source_commit_sha': SHA,
        'history_cutoff': '2026-01-01T00:00:00Z',
        'post_cutoff_knowledge_policy': 'EXCLUDE_AFTER_CUTOFF',
        'commits': [{
            'commit_sha': 'c' * 40, 'subject': 'application change',
            'changed_paths': ['src/main/java/example/fdi/Application.java'],
        }],
    }
    result = generate_arm('R2', {'history': history})
    assert [proposal['target_id'] for proposal in result['proposals']] == ['application']


def test_cli_binds_raw_graph_to_explicit_revision_and_digest(tmp_path):
    graph = tmp_path/'graph.json'
    graph.write_text(json.dumps({'nodes': structure()['nodes'], 'links': []}))
    semantics = tmp_path/'semantics.json'
    semantics.write_text(json.dumps({
        'status': 'FROZEN', 'owner': 'PRODUCT_TEAM',
        'applicable_source_commit_sha': SHA, 'capabilities': [],
    }))
    output = tmp_path/'result.json'
    assert main([
        '--arm', 'F1', '--input', 'structure=' + str(graph),
        '--input', 'semantics=' + str(semantics), '--source-sha', SHA,
        '--graph-sha', GRAPH_SHA, '--output', str(output),
    ]) == 0
    result = json.loads(output.read_text())
    assert result['source_commit_sha'] == SHA
    assert result['graph_artifact_sha256'] == GRAPH_SHA
