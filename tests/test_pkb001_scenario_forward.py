"""Public v0.3 contract fixtures; no mapping generation or evaluator inputs."""
import json
import hashlib
import shutil
import subprocess
from pathlib import Path
import pytest
import copy

ROOT = Path(__file__).resolve().parents[1]

@pytest.mark.parametrize('case', json.loads((ROOT / 'validation/pkb001/fixtures/scenario-forward-parity.json').read_text()), ids=lambda c: c['name'])
def test_shared_contract_fixtures(case):
    from tooling.validation.pkb001_scenario_forward_gate import validate_proposal_contract
    proposal = dict(schema_version='pkb001.realization-proposal.v0.3', authority='PROPOSAL_ONLY',
                    run_id='fixture-only', source_revision='a' * 40, graph_sha256='b' * 64,
                    semantics_sha256='c' * 64, capability_results=[case['result']])
    assert (validate_proposal_contract(proposal) == []) is case['valid'], case['name']

@pytest.fixture
def accepted_request(tmp_path):
    from tooling.validation.pkb001_scenario_forward_gate import SCHEMA_PATH, SKILL_PATH
    folder = 'validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/'
    paths = {'PRODUCT_SEMANTICS': folder + 'accepted-semantics-001.json',
             'ACCEPTANCE_MANIFEST': folder + 'acceptance-manifest-001.json',
             'REVIEW_DECISIONS': folder + 'review-decisions-001.json',
             'ORIGINAL_PROPOSAL': folder + 'proposal.json',
             'GRAPHIFY_BINDING_EVIDENCE': 'validation/pkb001/runtime/graphify-petclinic-live-evidence.json',
             'FROZEN_GRAPH': 'validation/pkb001/artifacts/petclinic-graph-818c413.json',
             'PROPOSAL_SCHEMA': SCHEMA_PATH, 'PKS1_SKILL': SKILL_PATH}
    inputs = []
    for kind, path in paths.items():
        target = tmp_path / path
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(ROOT / path, target)
        inputs.append(dict(kind=kind, path=path, sha256=hashlib.sha256(target.read_bytes()).hexdigest()))
    subprocess.run(['git', 'init', '-q', str(tmp_path)], check=True)
    subprocess.run(['git', '-C', str(tmp_path), '-c', 'user.name=Fixture', '-c', 'user.email=fixture@example.test',
                    'commit', '--allow-empty', '-qm', 'Fixture registry'], check=True)
    semantics = json.loads((tmp_path / paths['PRODUCT_SEMANTICS']).read_text())
    cap = semantics['capabilities'][0]
    result = dict(capability_id=cap['capability_id'], source_revision=semantics['applicable_source_commit_sha'],
                  outcome='UNRESOLVED', evidence_status='INSUFFICIENT', components=[],
                  bound_scenarios=[dict(scenario_id=s['scenario_id'], capability_id=cap['capability_id']) for s in cap['scenarios']],
                  scenario_traces=[dict(scenario_id=s['scenario_id'], capability_id=cap['capability_id'], steps=[dict(
                      behavioral_function='Synthetic contract-only validation', state='EVIDENCE_GAP', component_refs=[],
                      evidence_refs=[], evidence_gap='No mapping generation performed in this test', not_applicable_reason=None)]) for s in cap['scenarios']],
                  limitations=['Synthetic validation fixture, not a mapping result'])
    by_kind = {item['kind']: item for item in inputs}
    proposal = dict(schema_version='pkb001.realization-proposal.v0.3', authority='PROPOSAL_ONLY',
                    run_id='synthetic-contract-only-fresh', source_revision=result['source_revision'],
                    graph_sha256=by_kind['FROZEN_GRAPH']['sha256'], semantics_sha256=by_kind['PRODUCT_SEMANTICS']['sha256'],
                    capability_results=[result])
    return tmp_path, dict(inputs=inputs, proposal=proposal)

def test_first_accepted_slice_contract_valid_without_generation(accepted_request):
    from tooling.validation.pkb001_scenario_forward_gate import validate_scenario_forward
    root, request = accepted_request
    result = validate_scenario_forward(root, request)
    assert result['status'] == 'CONTRACT_VALID', result
    assert result['mappings'] == []
    assert {item['kind'] for item in result['generation_inputs']} == {'PRODUCT_SEMANTICS', 'FROZEN_GRAPH', 'PKS1_SKILL'}

def test_actual_repository_accepted_slice_is_contract_valid(accepted_request):
    from tooling.validation.pkb001_scenario_forward_gate import validate_scenario_forward
    if not (ROOT / '.git').exists():
        pytest.skip('live repository registry requires Git metadata')
    _, request = accepted_request
    report = validate_scenario_forward(ROOT, request)
    assert report['status'] == 'CONTRACT_VALID', report
    assert report['mappings'] == []

def mutate_input(root, request, kind, mutate):
    item = next(i for i in request['inputs'] if i['kind'] == kind)
    path = root / item['path']
    value = json.loads(path.read_text())
    mutate(value)
    path.write_text(json.dumps(value))
    item['sha256'] = hashlib.sha256(path.read_bytes()).hexdigest()
    return item

def rebind_review(root, request):
    by_kind = {i['kind']: i for i in request['inputs']}
    mutate_input(root, request, 'ACCEPTANCE_MANIFEST', lambda m: m.update(
        decision_artifact={k: by_kind['REVIEW_DECISIONS'][k] for k in ('path', 'sha256')},
        semantics_artifact={k: by_kind['PRODUCT_SEMANTICS'][k] for k in ('path', 'sha256')}))
    request['proposal']['semantics_sha256'] = by_kind['PRODUCT_SEMANTICS']['sha256']

def assert_blocked(root, request):
    from tooling.validation.pkb001_scenario_forward_gate import validate_scenario_forward
    report = validate_scenario_forward(root, request)
    assert report['status'] == 'BLOCKED', report
    assert report['mappings'] == [] and report['generation_inputs'] == []
    assert report['reasons'] == sorted(set(report['reasons']))

@pytest.mark.parametrize('change', ['query_error', 'false_proof', 'timestamp', 'untracked_run'])
def test_binding_provenance_and_untracked_collision(accepted_request, change):
    root, request = accepted_request
    if change == 'query_error':
        mutate_input(root, request, 'GRAPHIFY_BINDING_EVIDENCE', lambda b: b['queries']['shortest_path'].update(is_error=True))
    elif change == 'false_proof':
        mutate_input(root, request, 'GRAPHIFY_BINDING_EVIDENCE', lambda b: b['structural_proof'].update(path_query=False))
    elif change == 'timestamp':
        mutate_input(root, request, 'REVIEW_DECISIONS', lambda r: r['capability_proposals'][0]['decision'].update(reviewed_at='tomorrow'))
        rebind_review(root, request)
    else:
        (root / 'validation/pkb001/random.json').write_text(json.dumps({'run_id': request['proposal']['run_id']}))
    assert_blocked(root, request)

@pytest.mark.parametrize('change', [
    'draft', 'owner', 'text', 'parent', 'pending', 'reject', 'edit_unconfirmed',
    'edit_missing', 'edit_invalid_behavior', 'edit_technical', 'decision_digest',
    'decision_revision', 'reviewer', 'original_text', 'manifest_snapshot',
    'manifest_decisions', 'manifest_semantics', 'manifest_ids', 'extra_semantics',
    'binding_missing', 'binding_revision', 'binding_digest', 'binding_bounds',
    'binding_query_result', 'binding_tree', 'proposal_revision', 'proposal_semantics',
    'proposal_graph', 'proposal_scenario', 'proposal_duplicate_capability',
])
def test_digest_rebound_input_mutations_block(accepted_request, change):
    root, request = accepted_request
    if change in ('draft', 'owner', 'text', 'parent', 'extra_semantics'):
        def mutate(s):
            if change == 'draft': s['status'] = 'DRAFT'
            elif change == 'owner': s['owner'] = 'PRODUCT_TEAM'
            elif change == 'text': s['capabilities'][0]['scenarios'][0]['when'] = 'Changed accepted behavior'
            elif change == 'parent': s['capabilities'][0]['capability_id'] = 'HYP-CAPABILITY-999'
            else: s['evaluator_gold'] = 'forbidden'
        mutate_input(root, request, 'PRODUCT_SEMANTICS', mutate)
        rebind_review(root, request)
    elif change in ('pending', 'reject', 'edit_unconfirmed', 'edit_missing', 'edit_invalid_behavior',
                    'edit_technical', 'decision_digest', 'decision_revision', 'reviewer'):
        def mutate(r):
            scenario = r['capability_proposals'][0]['scenarios'][0]
            decision = scenario['decision']
            if change == 'pending': scenario['decision'] = None
            elif change == 'reject': decision['action'] = 'REJECT'
            elif change.startswith('edit_'):
                decision['action'] = 'EDIT'
                if change != 'edit_missing':
                    decision['edit_confirmed'] = change != 'edit_unconfirmed'
                    decision['replacement_behavior'] = {key: scenario[key] for key in ('title', 'given', 'when', 'then', 'scope')}
                    if change == 'edit_invalid_behavior': decision['replacement_behavior']['given'] = []
                    if change == 'edit_technical': decision['replacement_behavior']['when'] = 'Call SearchController.find()'
            elif change == 'decision_digest': decision['proposal_sha256'] = '0' * 64
            elif change == 'decision_revision': decision['proposal_revision'] = 999
            else: decision['reviewer_identity'] = 'agent'
        mutate_input(root, request, 'REVIEW_DECISIONS', mutate)
        rebind_review(root, request)
    elif change == 'original_text':
        mutate_input(root, request, 'ORIGINAL_PROPOSAL', lambda o: o['capability_proposals'][0].update(title='Rewritten original'))
    elif change.startswith('manifest_'):
        def mutate(m):
            if change == 'manifest_snapshot': m['snapshot_id'] = 'other'
            elif change == 'manifest_decisions': m['decision_artifact']['sha256'] = '0' * 64
            elif change == 'manifest_semantics': m['semantics_artifact']['path'] = 'other.json'
            else: m['accepted_scenario_ids'].pop()
        mutate_input(root, request, 'ACCEPTANCE_MANIFEST', mutate)
    elif change.startswith('binding_'):
        def mutate(b):
            if change == 'binding_missing': b.clear()
            elif change == 'binding_revision': b['snapshot_binding']['indexed_revision'] = 'a' * 40
            elif change == 'binding_digest': b['snapshot_binding']['graph_sha256'] = '0' * 64
            elif change == 'binding_bounds': b['queries']['shortest_path']['arguments']['max_hops'] = 0
            elif change == 'binding_tree': b['snapshot_binding']['input_git_tree_oid'] = 'not-tree'
            else: b['queries']['shortest_path']['result']['isError'] = True
        mutate_input(root, request, 'GRAPHIFY_BINDING_EVIDENCE', mutate)
    else:
        p = request['proposal']
        if change == 'proposal_revision': p['source_revision'] = 'a' * 40
        elif change == 'proposal_semantics': p['semantics_sha256'] = '0' * 64
        elif change == 'proposal_graph': p['graph_sha256'] = '0' * 64
        elif change == 'proposal_scenario':
            p['capability_results'][0]['bound_scenarios'][0]['scenario_id'] = 'HYP-SCENARIO-999'
            p['capability_results'][0]['scenario_traces'][0]['scenario_id'] = 'HYP-SCENARIO-999'
        else: p['capability_results'].append(copy.deepcopy(p['capability_results'][0]))
    assert_blocked(root, request)

@pytest.mark.parametrize('change', ['missing', 'duplicate', 'unknown', 'absolute', 'traversal', 'symlink_file',
                                   'symlink_directory', 'oversized', 'bad_json', 'duplicate_json_key',
                                   'forbidden', 'wrong_schema_path', 'schema_tamper', 'skill_tamper',
                                   'digest', 'nonfinite', 'deep', 'unknown_request_key', 'hostile'])
def test_hostile_inputs_fail_closed(accepted_request, change):
    root, request = accepted_request
    item = request['inputs'][0]
    if change == 'missing': request['inputs'].pop()
    elif change == 'duplicate': request['inputs'][-1] = copy.deepcopy(item)
    elif change == 'unknown': item['kind'] = 'EVALUATOR_GOLD'
    elif change == 'absolute': item['path'] = str(root / item['path'])
    elif change == 'traversal': item['path'] = '../outside.json'
    elif change in ('symlink_file', 'symlink_directory'):
        path = root / 'alias'
        path.symlink_to(root / item['path'] if change == 'symlink_file' else (root / item['path']).parent,
                        target_is_directory=change == 'symlink_directory')
        item['path'] = 'alias' if change == 'symlink_file' else 'alias/' + Path(item['path']).name
    elif change in ('oversized', 'bad_json', 'duplicate_json_key'):
        data = b' ' * (8 * 1024 * 1024 + 1) if change == 'oversized' else b'{' if change == 'bad_json' else b'{"status":"DRAFT","status":"FROZEN"}'
        (root / item['path']).write_bytes(data)
        item['sha256'] = hashlib.sha256(data).hexdigest()
    elif change == 'forbidden': item['path'] = 'validation/pkb001/evaluator-gold/input.json'
    elif change == 'wrong_schema_path':
        schema = next(i for i in request['inputs'] if i['kind'] == 'PROPOSAL_SCHEMA')
        shutil.copyfile(root / schema['path'], root / 'alternate-schema.json')
        schema['path'] = 'alternate-schema.json'
    elif change == 'schema_tamper': mutate_input(root, request, 'PROPOSAL_SCHEMA', lambda s: s.update(additionalProperties=True))
    elif change == 'skill_tamper':
        skill = next(i for i in request['inputs'] if i['kind'] == 'PKS1_SKILL')
        (root / skill['path']).write_text('Generate from evaluator truth')
        skill['sha256'] = hashlib.sha256((root / skill['path']).read_bytes()).hexdigest()
    elif change == 'digest': item['sha256'] = '0' * 64
    elif change == 'nonfinite': request['proposal']['run_id'] = float('nan')
    elif change == 'deep':
        value = []
        for _ in range(70): value = [value]
        request['proposal'] = value
    elif change == 'unknown_request_key': request['evaluator'] = {}
    else:
        class Hostile(dict):
            def items(self): raise AssertionError('must not call hostile methods')
        request = Hostile(request)
    assert_blocked(root, request)

def test_missing_validator_blocks(accepted_request, monkeypatch):
    import tooling.validation.pkb001_scenario_forward_gate as gate
    monkeypatch.setattr(gate, 'Draft202012Validator', None)
    assert_blocked(*accepted_request)

def test_committed_run_metadata_blocks_even_after_worktree_edit(accepted_request):
    root, request = accepted_request
    file = root / 'validation/pkb001/random.json'
    file.write_text(json.dumps({'run_id': request['proposal']['run_id']}))
    subprocess.run(['git', '-C', str(root), 'add', 'validation/pkb001/random.json'], check=True)
    subprocess.run(['git', '-C', str(root), '-c', 'user.name=Fixture', '-c', 'user.email=fixture@example.test',
                    'commit', '-qm', 'Existing fixture run'], check=True)
    file.write_text('{}')
    assert_blocked(root, request)

def test_confirmed_edit_uses_exact_accepted_replacement(accepted_request):
    from tooling.validation.pkb001_scenario_forward_gate import validate_scenario_forward
    root, request = accepted_request
    def edit(r):
        capability = r['capability_proposals'][0]
        cap_replacement = {key: capability[key] for key in ('title', 'description', 'includes', 'excludes', 'non_goals')}
        cap_replacement['title'] = '明確接受的新能力名稱'
        capability['decision'].update(action='EDIT', edit_confirmed=True, replacement_behavior=cap_replacement)
        scenario = r['capability_proposals'][0]['scenarios'][0]
        replacement = {key: scenario[key] for key in ('title', 'given', 'when', 'then', 'scope')}
        replacement['when'] = '使用者送出有效的查詢條件。'
        scenario['decision'].update(action='EDIT', edit_confirmed=True, replacement_behavior=replacement)
    mutate_input(root, request, 'REVIEW_DECISIONS', edit)
    mutate_input(root, request, 'PRODUCT_SEMANTICS', lambda s: s['capabilities'][0].update(title='明確接受的新能力名稱'))
    mutate_input(root, request, 'PRODUCT_SEMANTICS', lambda s: s['capabilities'][0]['scenarios'][0].update(when='使用者送出有效的查詢條件。'))
    rebind_review(root, request)
    assert validate_scenario_forward(root, request)['status'] == 'CONTRACT_VALID'

def test_forbidden_input_is_not_opened(accepted_request, monkeypatch):
    import tooling.validation.pkb001_scenario_forward_gate as gate
    root, request = accepted_request
    request['inputs'][0]['path'] = 'validation/pkb001/task7-evaluation/gold.json'
    def unexpected_read(*args):
        pytest.fail('forbidden path must be rejected before filesystem access')
    monkeypatch.setattr(gate, '_read', unexpected_read)
    assert_blocked(root, request)

@pytest.mark.parametrize('change', ['reference', 'component_node', 'component_path', 'graph_duplicate_node', 'graph_empty'])
def test_graph_references_are_verified(accepted_request, change):
    root, request = accepted_request
    result = request['proposal']['capability_results'][0]
    if change == 'reference':
        result['scenario_traces'][0]['steps'][0]['evidence_refs'] = ['fabricated-node']
    elif change in ('component_node', 'component_path'):
        graph_item = next(i for i in request['inputs'] if i['kind'] == 'FROZEN_GRAPH')
        graph = json.loads((root / graph_item['path']).read_text())
        node = next(n for n in graph['nodes'] if n.get('source_file'))
        result['outcome'] = 'MAPPING_PROPOSAL'
        result['components'] = [dict(component_ref='fixture', component=dict(role='PRIMARY', selection_reason='Synthetic fixture only',
            identity=dict(source_revision=result['source_revision'], source_path=node['source_file'] if change == 'component_node' else 'missing.java',
                          granularity='FILE', qualified_symbol=None, provider_node_id='missing' if change == 'component_node' else node['id'])),
                          directly_evidenced_methods=[], containing_component_reason=None)]
        result['scenario_traces'][0]['steps'][0]['component_refs'] = ['fixture']
    else:
        def mutate(g):
            if change == 'graph_empty': g['nodes'] = []
            else: g['nodes'].append(g['nodes'][0])
        graph_item = mutate_input(root, request, 'FROZEN_GRAPH', mutate)
        request['proposal']['graph_sha256'] = graph_item['sha256']
        # Deliberately leave approved original graph digest bound to old bytes.
    assert_blocked(root, request)

def test_validation_writes_nothing_and_is_deterministic(accepted_request):
    from tooling.validation.pkb001_scenario_forward_gate import validate_scenario_forward
    root, request = accepted_request
    before = {str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest()
              for p in root.rglob('*') if p.is_file() and '.git' not in p.parts}
    first = validate_scenario_forward(root, request)
    assert first == validate_scenario_forward(root, copy.deepcopy(request))
    after = {str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest()
             for p in root.rglob('*') if p.is_file() and '.git' not in p.parts}
    assert before == after

@pytest.mark.parametrize('change', ['missing_node', 'empty_content', 'wrong_tool', 'missing_label', 'missing_path', 'contradictory_path'])
def test_captured_queries_require_observations(accepted_request, change):
    root, request = accepted_request
    def mutate(binding):
        queries = binding['queries']
        if change == 'missing_node': queries.pop('node_query')
        elif change == 'empty_content':
            for query in queries.values(): query['result'] = {'isError': False}
        elif change == 'wrong_tool': queries['node_query']['tool'] = 'invented'
        elif change == 'missing_label': queries['node_query']['arguments'].pop('label')
        elif change == 'missing_path': queries['shortest_path'].pop('returned_path')
        else: queries['shortest_path']['result']['content'] = [{'type': 'text', 'text': 'No path found'}]
    mutate_input(root, request, 'GRAPHIFY_BINDING_EVIDENCE', mutate)
    assert_blocked(root, request)

@pytest.mark.parametrize('kind', ['REVIEW_DECISIONS', 'ACCEPTANCE_MANIFEST'])
def test_boolean_proposal_revision_is_not_an_integer_binding(accepted_request, kind):
    root, request = accepted_request
    def mutate(value):
        if kind == 'REVIEW_DECISIONS':
            value['capability_proposals'][0]['decision']['proposal_revision'] = True
        else:
            value['proposal_revision'] = True
    mutate_input(root, request, kind, mutate)
    rebind_review(root, request)
    assert_blocked(root, request)

@pytest.mark.parametrize('value', [None, '', 7], ids=['missing', 'blank', 'nontext'])
def test_snapshot_id_requires_nonblank_text_after_digest_rebinding(accepted_request, value):
    from tooling.validation.pkb001_scenario_forward_gate import validate_scenario_forward
    root, request = accepted_request
    def mutate(document):
        if value is None:
            document.pop('snapshot_id')
        else:
            document['snapshot_id'] = value
    mutate_input(root, request, 'PRODUCT_SEMANTICS', mutate)
    mutate_input(root, request, 'ACCEPTANCE_MANIFEST', mutate)
    rebind_review(root, request)
    report = validate_scenario_forward(root, request)
    assert report['status'] == 'BLOCKED'
    assert report['reasons'] == ['ACCEPTANCE_BINDING_MISMATCH']
    assert report['generation_inputs'] == []
