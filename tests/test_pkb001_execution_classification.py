import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def test_deterministic_outputs_are_classified_only_as_code_baseline():
    record = json.loads((
        ROOT/'validation/pkb001/baselines/code/PKB001-510a397/execution-record.json'
    ).read_text())
    assert record['status'] == 'CODE_BASELINE_EXECUTED'
    assert record['execution_kind'] == 'DETERMINISTIC_CODE_BASELINE'
    assert record['skill_execution_status'] == 'NOT_APPLICABLE'
    assert record['human_review_status'] == 'NOT_STARTED'
    assert record['claim_boundary'] == 'NOT_SKILL_EXECUTION_NOT_PRODUCT_TRUTH'


def test_leaked_pk_s1_is_invalidated_and_pk_s2_remains_proposal_only():
    pk_s1 = json.loads((
        ROOT/'validation/pkb001/baselines/leaked-forward/PK-S1-510a397/output.json'
    ).read_text())
    pk_s2 = json.loads((
        ROOT/'validation/pkb001/skill-runs/PK-S2-510a397/output.json'
    ).read_text())
    assert pk_s1['execution_kind'] == 'SKILL_EXECUTION'
    assert pk_s1['status'] == 'INVALIDATED_INPUT_LEAKAGE'
    assert pk_s1['evaluation_valid'] is False
    assert pk_s1['authority_status'] == 'PROPOSAL_ONLY'
    assert len(pk_s1['mappings']) == 2
    assert pk_s2['status'] == 'COMPLETED'
    assert pk_s2['authority_status'] == 'PROPOSAL_ONLY'
    assert len(pk_s2['hypotheses']) == 4


def test_invalidated_pk_s1_evidence_still_resolves_in_exact_graph():
    output = json.loads((
        ROOT/'validation/pkb001/baselines/leaked-forward/PK-S1-510a397/output.json'
    ).read_text())
    graph = json.loads((
        ROOT/'validation/pkb001/artifacts/graph-510a397.json'
    ).read_text())
    graph_refs = {
        f"graph-node:{node['id']}@{node.get('source_location', 'UNKNOWN')}"
        for node in graph['nodes']
    }
    graph_files = {node['source_file'] for node in graph['nodes']}
    for mapping in output['mappings']:
        assert set(mapping['evidence_refs']) <= graph_refs
        assert set(mapping['component_refs']) <= graph_files


def test_fresh_pk_s2_evidence_resolves_only_in_allowed_inputs():
    output = json.loads((
        ROOT/'validation/pkb001/skill-runs/PK-S2-510a397/output.json'
    ).read_text())
    graph = json.loads((
        ROOT/'validation/pkb001/artifacts/graph-510a397.json'
    ).read_text())
    history = json.loads((
        ROOT/'validation/pkb001/datasets/delivery-history.json'
    ).read_text())
    allowed = {
        f"graph-node:{node['id']}@{node.get('source_location', 'UNKNOWN')}"
        for node in graph['nodes']
    }
    allowed |= {'git-commit:' + commit['commit_sha'] for commit in history['commits']}
    allowed |= {'pull-request:' + str(pr['number']) for pr in history['pull_requests']}
    for hypothesis in output['hypotheses']:
        assert set(hypothesis['evidence_refs']) <= allowed
    assert output['visible_inputs'] == [
        'PK-S2-SKILL.md', 'input/graph-510a397.json',
        'input/delivery-history.json', 'input/graphify-discovery.json']
    verification = json.loads((
        ROOT/'validation/pkb001/skill-runs/PK-S2-510a397/verification.json'
    ).read_text())
    import hashlib
    assert verification['output_sha256'] == hashlib.sha256((
        ROOT/'validation/pkb001/skill-runs/PK-S2-510a397/output.json'
    ).read_bytes()).hexdigest()
    kinds = [ref.split(':', 1)[0] for hypothesis in output['hypotheses']
             for ref in hypothesis['evidence_refs']]
    assert verification['graph_evidence_references'] == kinds.count('graph-node')
    assert verification['commit_evidence_references'] == kinds.count('git-commit')
    assert verification['pull_request_evidence_references'] == kinds.count('pull-request')


def test_obsolete_code_baseline_review_packet_is_not_active():
    assert not (ROOT/'validation/pkb001/reviews/PKB001-510a397').exists()
    assert not (ROOT/'validation/pkb001/evaluator/PKB001-510a397-blind-key.json').exists()


def _pk_s1_text():
    return (ROOT/'skills/pkb001/pk-s1-product-realization-v0.2/SKILL.md').read_text()


def test_historical_pk_s1_remains_bound_to_petclinic_manifest():
    skill_path = ROOT/'skills/pkb001/pk-s1-product-realization/SKILL.md'
    manifest = json.loads((
        ROOT/'validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-manifest.json'
    ).read_text())
    recorded = manifest['visible_input_sha256'][
        'skills/pkb001/pk-s1-product-realization/SKILL.md'
    ]
    assert recorded == 'f97d4e5b13605de81ab1b149e338031feea736666dc2c6f0b7635ce9131a2ca9'
    assert hashlib.sha256(skill_path.read_bytes()).hexdigest() == recorded


def test_pk_s1_component_output_contract_is_complete_and_typed():
    text = _pk_s1_text()
    contract = text.split('## Component output contract', 1)[1]
    required_fields = (
        'role', 'granularity', 'source_revision', 'source_path',
        'qualified_symbol', 'provider_node_id', 'selection_reason',
    )
    assert all(f'`{field}`' in contract for field in required_fields)
    assert 'repository-relative `source_path`' in contract
    assert '`PRIMARY`' in contract and '`SUPPORTING`' in contract
    assert all(f'`{value}`' in contract for value in (
        'REPOSITORY', 'FILE', 'TYPE', 'METHOD', 'TEMPLATE', 'CONFIGURATION',
    ))
    assert 'Every `MAPPING_PROPOSAL` MUST contain at least one `PRIMARY`' in contract
    assert '`UNRESOLVED` MUST emit no components' in contract


def test_pk_s1_preserves_exact_component_selection_and_binding_rules():
    text = _pk_s1_text()
    contract = text.split('## Component output contract', 1)[1]
    assert 'A containing class or file must not replace a directly evidenced method node' in contract
    assert 'Supporting evidence remains separate' in contract
    assert 'cannot count as a primary exact component' in contract
    assert 'full 40-character source revision' in contract
    assert 'exact Graphify binding' in contract


def test_pk_s1_forbidden_inputs_fail_closed_without_mappings():
    text = _pk_s1_text()
    assert 'PK-S1 **MUST NOT** read' in text
    for forbidden in (
        'evaluator gold',
        'sealed expected mappings',
        'reviewer judgments',
        'post-generation comparison or evaluation results',
        'current human-review decision packet',
    ):
        assert forbidden in text
    assert 'supplied or accessed' in text
    assert 'return `BLOCKED` with no mappings' in text


def test_pk_s1_retains_product_team_proposal_only_boundary():
    text = _pk_s1_text()
    assert 'Product Semantics remains owned by `PRODUCT_TEAM`' in text
    assert 'Mapping status is always `PROPOSAL_ONLY`' in text
    assert 'MUST NOT publish Product truth' in text


def test_active_spec_discovers_and_selects_versioned_pk_s1_for_next_run():
    text = (ROOT/'FRAMEWORK-SPEC.md').read_text()
    placement = text.split('### Planned project placement and verification', 1)[1]
    assert 'historical PK-S1 directory' in placement
    assert 'remain immutable for their existing experiments and contracts' in placement
    assert '`skills/pkb001/pk-s1-product-realization-v0.2/`' in placement
    assert 'separately versioned PK-S1 skill and proposal contract' in placement
    assert 'readiness gate MUST explicitly select and verify that version and its schema digest' in placement
    assert 'does not authorize experiment execution or bypass its remaining gates' in placement


def test_pk_s1_proposal_artifact_authority_is_immutable_and_review_is_separate():
    text = _pk_s1_text()
    assert 'generated proposal artifact remains permanently `PROPOSAL_ONLY`' in text
    assert 'Evaluator and Product Team review produce separate decision artifacts' in text
    assert 'only a separate explicit Product Team publication action can change Product Semantics' in text
    assert 'PK-S1 never marks a proposal accepted' in text


def test_plan_does_not_point_to_removed_next_run_python_consumer():
    plan = (ROOT / 'IMPLEMENTATION-PLAN.md').read_text()
    assert 'tooling/validation/pkb001_next_run_gate.py' not in plan
    assert 'tests/test_pkb001_next_run_gate.py' not in plan
    assert '| `PKB-BL-026` |' in plan
