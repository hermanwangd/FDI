"""Bounded v0.3 validation only: never generation, publication or experiment READY."""
import copy
import hashlib
import json
import os
import re
import stat
import subprocess
from datetime import datetime
from pathlib import Path

from tooling.validation.pkb001_next_run_gate import (
    _snapshot_json, _canonical_relative, _forbidden_path,
)
from tooling.validation.pkb001_scenario_review import TECHNICAL_TEXT

try:
    from jsonschema import Draft202012Validator
except ImportError:
    Draft202012Validator = None

ROOT = Path(__file__).resolve().parents[2]
SCHEMA_PATH = 'validation/pkb001/schemas/realization-proposal-v0.3.schema.json'
SKILL_PATH = 'skills/pkb001/pk-s1-product-realization-v0.3/SKILL.md'
MAX_BYTES = 8 * 1024 * 1024
KINDS = frozenset({'PRODUCT_SEMANTICS', 'ACCEPTANCE_MANIFEST', 'REVIEW_DECISIONS',
                   'ORIGINAL_PROPOSAL', 'GRAPHIFY_BINDING_EVIDENCE', 'FROZEN_GRAPH',
                   'PROPOSAL_SCHEMA', 'PKS1_SKILL'})
GENERATION_KINDS = ('PRODUCT_SEMANTICS', 'FROZEN_GRAPH', 'PKS1_SKILL')


def _identity_key(identity):
    return tuple(identity[key] for key in ('source_revision', 'source_path', 'granularity', 'qualified_symbol'))


def _identity_valid(identity):
    path, granularity = identity['source_path'], identity['granularity']
    # Match the Java contract, including nullable symbols on file/repository identities.
    valid_path = (path == '.' and granularity == 'REPOSITORY') or (
        path != '.' and not path.startswith('/') and not re.match(r'^[A-Za-z]:', path)
        and '\\' not in path and all(part not in ('', '.', '..') for part in path.split('/')))
    symbol = identity['qualified_symbol']
    return valid_path and (granularity in ('FILE', 'REPOSITORY') or
                          (isinstance(symbol, str) and bool(re.search(r'[^\t\n\v\f\r \u001c-\u001f\u1680\u2000-\u2006\u2008-\u200a\u2028\u2029\u205f\u3000]', symbol))))


def validate_proposal_contract(proposal):
    """Validate the public JSON envelope against schema and Java record invariants."""
    try:
        return _contract(proposal, _read(ROOT, SCHEMA_PATH))
    except Exception:
        return ['SCHEMA_DEFINITION_INVALID']


def _contract(proposal, schema_bytes):
    try:
        proposal = _snapshot_json(proposal)
        if len(json.dumps(proposal).encode()) > MAX_BYTES:
            return ['REQUEST_INVALID']
        if Draft202012Validator is None:
            return ['SCHEMA_DEFINITION_INVALID']
        schema = json.loads(schema_bytes)
        Draft202012Validator.check_schema(schema)
        if not Draft202012Validator(schema).is_valid(proposal):
            return ['SCHEMA_INVALID']
        reasons = set()
        caps = set()
        for result in proposal['capability_results']:
            cap, revision = result['capability_id'], result['source_revision']
            if cap in caps: reasons.add('DUPLICATE_CAPABILITY')
            caps.add(cap)
            if revision != proposal['source_revision']: reasons.add('REVISION_BINDING_MISMATCH')
            expected = [s['scenario_id'] for s in result['bound_scenarios']]
            seen = [s['scenario_id'] for s in result['scenario_traces']]
            if len(expected) != len(set(expected)) or len(seen) != len(set(seen)) or set(expected) != set(seen):
                reasons.add('SCENARIO_MEMBERSHIP_INVALID')
            if any(s['capability_id'] != cap for s in result['bound_scenarios'] + result['scenario_traces']):
                reasons.add('SCENARIO_PARENT_MISMATCH')
            components = result['components']
            if result['outcome'] == 'UNRESOLVED' and components: reasons.add('OUTCOME_INVALID')
            if result['outcome'] == 'MAPPING_PROPOSAL' and not any(c['component']['role'] == 'PRIMARY' for c in components):
                reasons.add('OUTCOME_INVALID')
            refs, identities = set(), set()
            for item in components:
                ref, ident = item['component_ref'], item['component']['identity']
                if ref in refs or _identity_key(ident) in identities: reasons.add('DUPLICATE_COMPONENT')
                refs.add(ref); identities.add(_identity_key(ident))
                if not _identity_valid(ident): reasons.add('COMPONENT_IDENTITY_INVALID')
                if ident['source_revision'] != revision: reasons.add('COMPONENT_REVISION_MISMATCH')
                methods = set()
                for method in item['directly_evidenced_methods']:
                    key = _identity_key(method)
                    if key in methods or method['granularity'] != 'METHOD' or not _identity_valid(method):
                        reasons.add('DIRECT_METHOD_INVALID')
                    methods.add(key)
                    if method['source_revision'] != revision: reasons.add('COMPONENT_REVISION_MISMATCH')
                    symbol, qualified = ident['qualified_symbol'], method['qualified_symbol']
                    member = qualified[len(symbol) + 1:] if isinstance(symbol, str) and qualified.startswith(symbol + '.') else ''
                    method_name = member.split('(')[0]
                    containing = ident['granularity'] == 'FILE' or (ident['granularity'] == 'TYPE' and method_name and '.' not in method_name)
                    if ident['source_path'] == method['source_path'] and containing and item['containing_component_reason'] is None:
                        reasons.add('CONTAINING_COMPONENT_REASON_REQUIRED')
            used = set()
            for trace in result['scenario_traces']:
                for step in trace['steps']:
                    used.update(step['component_refs'])
                    state = step['state']
                    if state == 'EVIDENCED' and not step['evidence_refs']: reasons.add('STEP_EVIDENCE_REQUIRED')
                    if (state == 'EVIDENCE_GAP') != (step['evidence_gap'] is not None): reasons.add('STEP_GAP_INVALID')
                    if (state == 'NOT_APPLICABLE') != (step['not_applicable_reason'] is not None): reasons.add('STEP_NOT_APPLICABLE_INVALID')
                    if state == 'EVIDENCE_GAP' and result['evidence_status'] == 'COMPLETE': reasons.add('EVIDENCE_STATUS_INVALID')
            if used != refs: reasons.add('COMPONENT_REFERENCE_INVALID')
        return sorted(reasons)
    except Exception:
        return ['REQUEST_INVALID']


def _read(root, relative):
    """Read one regular file once, with no-follow directory traversal and byte bound."""
    if _canonical_relative(relative) is None:
        raise ValueError('INPUT_PATH_INVALID')
    fd = os.open(root, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW)
    try:
        parts = relative.split('/')
        for part in parts[:-1]:
            child = os.open(part, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=fd)
            os.close(fd); fd = child
        child = os.open(parts[-1], os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK, dir_fd=fd)
        with os.fdopen(child, 'rb') as handle:
            info = os.fstat(handle.fileno())
            if not stat.S_ISREG(info.st_mode) or info.st_size > MAX_BYTES:
                raise ValueError('INPUT_SIZE_INVALID')
            data = handle.read(MAX_BYTES + 1)
            if len(data) > MAX_BYTES: raise ValueError('INPUT_SIZE_INVALID')
            return data
    finally:
        os.close(fd)


def _json(data):
    def unique(pairs):
        result = {}
        for key, value in pairs:
            if key in result: raise ValueError('INPUT_JSON_INVALID')
            result[key] = value
        return result
    value = _snapshot_json(json.loads(data, object_pairs_hook=unique))
    if type(value) is not dict: raise ValueError('INPUT_JSON_INVALID')
    return value


def _sha(data):
    return hashlib.sha256(data).hexdigest()


def _decision_behavior(item, fields, revision, digest, reviewer):
    decision = item.get('decision')
    if decision is None: return None
    if type(decision) is not dict: raise ValueError('DECISION_INVALID')
    if (type(decision.get('proposal_revision')) is not int
            or decision.get('proposal_revision') != revision or decision.get('proposal_sha256') != digest
            or decision.get('reviewer_identity') != reviewer):
        raise ValueError('DECISION_BINDING_MISMATCH')
    if not all(isinstance(decision.get(key), str) and decision[key].strip()
               for key in ('reviewer_identity', 'reviewed_at', 'reason')):
        raise ValueError('DECISION_PROVENANCE_INVALID')
    try:
        timestamp = datetime.fromisoformat(decision['reviewed_at'].replace('Z', '+00:00'))
        if timestamp.tzinfo is None: raise ValueError()
    except ValueError:
        raise ValueError('DECISION_PROVENANCE_INVALID') from None
    action = decision.get('action')
    allowed = {'action', 'reviewer_identity', 'reviewed_at', 'reason', 'proposal_revision', 'proposal_sha256'}
    if action == 'EDIT':
        allowed |= {'replacement_behavior', 'edit_confirmed'}
        if set(decision) != allowed or type(decision.get('edit_confirmed')) is not bool:
            raise ValueError('DECISION_INVALID')
        if not decision['edit_confirmed']: return None
        behavior = decision.get('replacement_behavior')
        if type(behavior) is not dict or set(behavior) != set(fields):
            raise ValueError('EDIT_BEHAVIOR_INVALID')
        _behavior_valid(behavior)
        return behavior
    if set(decision) != allowed or action not in ('ACCEPT', 'REJECT'):
        raise ValueError('DECISION_INVALID')
    behavior = {key: item[key] for key in fields}
    _behavior_valid(behavior)
    return behavior if action == 'ACCEPT' else None


def _behavior_valid(behavior):
    for key, value in behavior.items():
        if key == 'scope':
            if value not in ('REQUIRED_ACCEPTANCE', 'ILLUSTRATIVE'): raise ValueError('BEHAVIOR_INVALID')
            continue
        if key in ('given', 'then', 'includes', 'excludes', 'non_goals'):
            if type(value) is not list or not value: raise ValueError('BEHAVIOR_INVALID')
            values = value
        else:
            values = [value]
        if any(type(text) is not str or not text.strip() for text in values): raise ValueError('BEHAVIOR_INVALID')
        if any(TECHNICAL_TEXT.search(text) for text in values): raise ValueError('TECHNICAL_IDENTIFIER_IN_BEHAVIOR')


def _review_semantics(documents, items):
    """Consistency of trusted reviewer artifacts; strings cannot authenticate a human."""
    semantics, manifest, review, original = (documents[k] for k in
        ('PRODUCT_SEMANTICS', 'ACCEPTANCE_MANIFEST', 'REVIEW_DECISIONS', 'ORIGINAL_PROPOSAL'))
    digest = items['ORIGINAL_PROPOSAL']['sha256']
    revision = original.get('proposal_revision')
    if type(revision) is not int or revision < 1: raise ValueError('PROPOSAL_BINDING_INVALID')
    if (semantics.get('status') != 'FROZEN' or manifest.get('status') != 'FROZEN'):
        raise ValueError('PRODUCT_SEMANTICS_NOT_FROZEN')
    if semantics.get('owner') != 'HUMAN_REVIEWER': raise ValueError('PRODUCT_SEMANTICS_OWNER_INVALID')
    if (semantics.get('authority') != 'REVIEWED_EXPERIMENT_SEMANTICS'
            or semantics.get('schema_version') != 'pkb001.reviewed-experiment-semantics.v0.1'
            or manifest.get('schema_version') != 'pkb001.acceptance-manifest.v0.1'
            or original.get('authority') != 'PROPOSAL_ONLY'
            or original.get('artifact_kind') != 'SCENARIO_PROPOSAL'
            or review.get('artifact_kind') != 'SCENARIO_REVIEW_SURFACE'):
        raise ValueError('AUTHORITY_INVALID')
    if (type(manifest.get('proposal_revision')) is not int
            or type(review.get('proposal_revision')) is not int
            or not isinstance(semantics.get('snapshot_id'), str) or not semantics['snapshot_id'].strip()
            or not isinstance(manifest.get('snapshot_id'), str) or not manifest['snapshot_id'].strip()
            or manifest['snapshot_id'] != semantics['snapshot_id']
            or manifest.get('proposal_sha256') != digest or review.get('proposal_sha256') != digest
            or manifest.get('proposal_revision') != revision or review.get('proposal_revision') != revision
            or review.get('proposal_artifact_path') != items['ORIGINAL_PROPOSAL']['path']):
        raise ValueError('ACCEPTANCE_BINDING_MISMATCH')
    for field, kind in [('semantics_artifact', 'PRODUCT_SEMANTICS'), ('decision_artifact', 'REVIEW_DECISIONS')]:
        if manifest.get(field) != {key: items[kind][key] for key in ('path', 'sha256')}:
            raise ValueError('ACCEPTANCE_BINDING_MISMATCH')
    expected = copy.deepcopy(original)
    actual = copy.deepcopy(review)
    for value in (expected, actual):
        for cap in value['capability_proposals']:
            if value is expected and cap.get('decision') is not None: raise ValueError('GENERATED_DECISION_FORBIDDEN')
            cap['decision'] = None
            for scenario in cap['scenarios']:
                if value is expected and scenario.get('decision') is not None: raise ValueError('GENERATED_DECISION_FORBIDDEN')
                scenario['decision'] = None
    for key in ('artifact_kind', 'proposal_artifact_path', 'proposal_sha256', 'resolved_evidence'):
        expected.pop(key, None); actual.pop(key, None)
    if actual != expected: raise ValueError('ORIGINAL_PROPOSAL_MISMATCH')
    cap_fields = ('title', 'description', 'includes', 'excludes', 'non_goals')
    scenario_fields = ('title', 'given', 'when', 'then', 'scope')
    accepted, cap_ids, scenario_ids = [], set(), set()
    all_caps, all_scenarios = set(), set()
    reviewer = manifest.get('reviewer_identity')
    for cap in review['capability_proposals']:
        cap_id = cap['capability_id']
        if not re.fullmatch('HYP-CAPABILITY-[A-Z0-9][A-Z0-9-]*', cap_id): raise ValueError('CAPABILITY_ID_INVALID')
        if cap_id in all_caps: raise ValueError('DUPLICATE_CAPABILITY')
        all_caps.add(cap_id)
        behavior = _decision_behavior(cap, cap_fields, revision, digest, reviewer)
        scenarios = []
        for scenario in cap['scenarios']:
            scenario_id = scenario['scenario_id']
            if not re.fullmatch('HYP-SCENARIO-[A-Z0-9][A-Z0-9-]*', scenario_id): raise ValueError('SCENARIO_ID_INVALID')
            if scenario_id in all_scenarios: raise ValueError('SCENARIO_MEMBERSHIP_INVALID')
            all_scenarios.add(scenario_id)
            selected = _decision_behavior(scenario, scenario_fields, revision, digest, reviewer)
            if selected is not None and behavior is not None:
                scenarios.append(dict(scenario_id=scenario_id, **selected)); scenario_ids.add(scenario_id)
        if behavior is not None:
            accepted.append(dict(capability_id=cap_id, **behavior, scenarios=scenarios)); cap_ids.add(cap_id)
    if not accepted or not scenario_ids: raise ValueError('ACCEPTED_SET_EMPTY')
    if semantics.get('capabilities') != accepted: raise ValueError('ACCEPTED_BEHAVIOR_MISMATCH')
    if (sorted(manifest.get('accepted_capability_ids', [])) != sorted(cap_ids)
            or sorted(manifest.get('accepted_scenario_ids', [])) != sorted(scenario_ids)):
        raise ValueError('ACCEPTED_SET_MISMATCH')
    if set(semantics) != {'schema_version', 'snapshot_id', 'status', 'authority', 'owner',
                          'applicable_source_commit_sha', 'capabilities'}:
        raise ValueError('SEMANTICS_FIELDS_INVALID')


def _binding(documents, proposal, items):
    binding = documents['GRAPHIFY_BINDING_EVIDENCE']
    snapshot = binding.get('snapshot_binding', {})
    if not isinstance(snapshot.get('input_git_tree_oid'), str) or not re.fullmatch('[0-9a-f]{40}', snapshot['input_git_tree_oid']):
        raise ValueError('GRAPHIFY_SNAPSHOT_INVALID')
    if (binding.get('result') != 'EXACTLY_BOUND' or binding.get('queryable') is not True
            or binding.get('exact_revision_opened') is not True):
        raise ValueError('GRAPHIFY_BINDING_INVALID')
    queries = binding.get('queries', {})
    query = queries.get('shortest_path', {})
    for key, tool, arguments in (('node_query', 'get_node', ('label',)),
                                  ('shortest_path', 'shortest_path', ('source', 'target'))):
        observed = queries.get(key, {})
        content = observed.get('result', {}).get('content')
        if (observed.get('tool') != tool
                or not all(isinstance(observed.get('arguments', {}).get(arg), str)
                           and observed['arguments'][arg].strip() for arg in arguments)
                or not isinstance(content, list)
                or not any(isinstance(block, dict) and block.get('type') == 'text'
                           and isinstance(block.get('text'), str) and block['text'].strip()
                           for block in content)):
            raise ValueError('GRAPHIFY_OBSERVATION_MISSING')
    if not isinstance(query.get('returned_path'), str) or not query['returned_path'].strip():
        raise ValueError('GRAPHIFY_OBSERVATION_MISSING')
    if not any(block.get('type') == 'text' and isinstance(block.get('text'), str)
               and query['returned_path'] in block['text']
               for block in query['result']['content'] if isinstance(block, dict)):
        raise ValueError('GRAPHIFY_OBSERVATION_CONTRADICTORY')
    if binding.get('structural_proof') != {'node_query': True, 'path_query': True}:
        raise ValueError('GRAPHIFY_BINDING_INVALID')
    for observed in binding.get('queries', {}).values():
        if observed.get('is_error') is not False or observed.get('result', {}).get('isError') is not False:
            raise ValueError('GRAPHIFY_BINDING_INVALID')
    max_hops = query.get('arguments', {}).get('max_hops')
    hops = query.get('observed_hops')
    if type(max_hops) is not int or not 1 <= max_hops <= 100 or type(hops) is not int or not 0 <= hops <= max_hops:
        raise ValueError('GRAPHIFY_QUERY_BOUNDS_INVALID')
    revision = proposal['source_revision']
    if any(value != revision for value in (snapshot.get('requested_revision'), snapshot.get('indexed_revision'),
            documents['PRODUCT_SEMANTICS'].get('applicable_source_commit_sha'),
            documents['ACCEPTANCE_MANIFEST'].get('source_revision'),
            documents['ORIGINAL_PROPOSAL'].get('source_revision'), documents['REVIEW_DECISIONS'].get('source_revision'))):
        raise ValueError('REVISION_BINDING_MISMATCH')
    graph_digest = items['FROZEN_GRAPH']['sha256']
    if any(value != graph_digest for value in (binding.get('graph_sha256'), snapshot.get('graph_sha256'),
            proposal['graph_sha256'], documents['ORIGINAL_PROPOSAL'].get('graph_sha256'),
            documents['REVIEW_DECISIONS'].get('graph_sha256'))):
        raise ValueError('GRAPH_BINDING_DIGEST_MISMATCH')
    if proposal['semantics_sha256'] != items['PRODUCT_SEMANTICS']['sha256']:
        raise ValueError('SEMANTICS_DIGEST_MISMATCH')


def _graph_references(graph, proposal, semantics):
    nodes = graph.get('nodes')
    if type(nodes) is not list or not nodes: raise ValueError('GRAPH_SHAPE_INVALID')
    by_id = {}
    for node in nodes:
        if type(node) is not dict or type(node.get('id')) is not str or node['id'] in by_id:
            raise ValueError('GRAPH_SHAPE_INVALID')
        by_id[node['id']] = node
    accepted = {c['capability_id']: {s['scenario_id'] for s in c['scenarios']} for c in semantics['capabilities']}
    results = proposal['capability_results']
    if {r['capability_id'] for r in results} != set(accepted): raise ValueError('ACCEPTED_SET_MISMATCH')
    for result in results:
        if {s['scenario_id'] for s in result['bound_scenarios']} != accepted[result['capability_id']]:
            raise ValueError('SCENARIO_MEMBERSHIP_INVALID')
        for item in result['components']:
            for identity in [item['component']['identity']] + item['directly_evidenced_methods']:
                node = by_id.get(identity['provider_node_id'])
                if node is None or node.get('source_file') != identity['source_path']:
                    raise ValueError('GRAPH_COMPONENT_REFERENCE_INVALID')
                for key in ('qualified_symbol', 'granularity', 'source_revision'):
                    if key in node and node[key] != identity[key]: raise ValueError('GRAPH_COMPONENT_REFERENCE_INVALID')
        for trace in result['scenario_traces']:
            for step in trace['steps']:
                if any(ref not in by_id for ref in step['evidence_refs']): raise ValueError('GRAPH_EVIDENCE_REFERENCE_INVALID')


def _run_available(root, run_id, documents):
    # Query only registry metadata. Never decode evaluator artifacts or pass registry
    # data to generation. Git grep is bounded by the subprocess pipe's output file.
    import tempfile
    with tempfile.TemporaryFile() as output:
        result = subprocess.run(['git', 'grep', '-h', '-o', '-E', '"run_id"[[:space:]]*:[[:space:]]*"[^"[:cntrl:]]*"',
                                 'HEAD', '--', ':(glob)validation/pkb001/**/*.json'], cwd=root, stdout=output,
                                stderr=subprocess.DEVNULL, timeout=15)
        if result.returncode not in (0, 1): raise ValueError('RUN_ID_REGISTRY_UNAVAILABLE')
        if output.tell() > MAX_BYTES: raise ValueError('RUN_ID_REGISTRY_INVALID')
        output.seek(0)
        ids = [json.loads('{' + line.decode() + '}')['run_id'] for line in output if line.strip()]
    ids.extend(value.get('run_id') for value in documents.values())
    if run_id in ids: raise ValueError('RUN_ID_ALREADY_EXISTS')
    # Existing path identities (including untracked runs) cannot be reused.
    directory = root / 'validation/pkb001'
    count = 0
    for parent, dirs, files in os.walk(directory, followlinks=False):
        count += len(dirs) + len(files)
        if count > 10000: raise ValueError('RUN_ID_REGISTRY_INVALID')
        if run_id in dirs or run_id + '.json' in files: raise ValueError('RUN_ID_ALREADY_EXISTS')
        for filename in files:
            relative = str((Path(parent) / filename).relative_to(root))
            if not filename.endswith('.json') or _forbidden_path(relative): continue
            # Metadata only; do not decode or retain semantic/evaluator content.
            content = _read(root, relative)
            for match in re.finditer(rb'"run_id"\s*:\s*("(?:[^"\\]|\\.)*")', content):
                if json.loads(match.group(1)) == run_id: raise ValueError('RUN_ID_ALREADY_EXISTS')


def validate_scenario_forward(root, request):
    """Validate trusted-root review consistency; never authenticate human identity.

    The caller must supply a trusted repository and approved review artifacts.
    CONTRACT_VALID validates bindings only; it is neither experiment permission
    nor proof that a reviewer_identity string is authentic. The returned allowlist
    contains no review evidence. Consumers must use bound snapshots/reverify hashes.
    """
    reasons, items, documents, data = set(), {}, {}, {}
    run_id = None
    try:
        request = _snapshot_json(request)
        if len(json.dumps(request).encode()) > MAX_BYTES or type(request) is not dict or set(request) != {'inputs', 'proposal'}:
            raise ValueError('REQUEST_INVALID')
        inputs = request['inputs']
        if type(inputs) is not list or len(inputs) != len(KINDS): raise ValueError('REQUIRED_INPUT_SET_INVALID')
        for item in inputs:
            if type(item) is not dict or set(item) != {'kind', 'path', 'sha256'}: raise ValueError('INPUT_INVALID')
            kind = item['kind']
            if type(kind) is not str or kind not in KINDS or kind in items: raise ValueError('REQUIRED_INPUT_SET_INVALID')
            if _forbidden_path(item['path']): raise ValueError('FORBIDDEN_INPUT')
            if type(item['sha256']) is not str or not re.fullmatch('[0-9a-f]{64}', item['sha256']): raise ValueError('INPUT_DIGEST_INVALID')
            items[kind] = item
        root = Path(root).resolve(strict=True)
        for kind, item in items.items():
            data[kind] = _read(root, item['path'])
            if _sha(data[kind]) != item['sha256']: raise ValueError('INPUT_DIGEST_MISMATCH')
            if kind not in ('PKS1_SKILL',): documents[kind] = _json(data[kind])
        for kind, path in [('PROPOSAL_SCHEMA', SCHEMA_PATH), ('PKS1_SKILL', SKILL_PATH)]:
            if items[kind]['path'] != path: raise ValueError('VERSION_NOT_SELECTED')
            if data[kind] != _read(ROOT, path): raise ValueError('SELECTED_CONTRACT_DIGEST_MISMATCH')
        proposal = request['proposal']
        reasons.update(_contract(proposal, data['PROPOSAL_SCHEMA']))
        if reasons: return _report(reasons, None, [])
        run_id = proposal['run_id']
        if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._-]{0,127}', run_id): raise ValueError('RUN_ID_INVALID')
        _review_semantics(documents, items)
        _binding(documents, proposal, items)
        _graph_references(documents['FROZEN_GRAPH'], proposal, documents['PRODUCT_SEMANTICS'])
        _run_available(root, run_id, documents)
    except ValueError as error:
        code = str(error)
        reasons.add(code if re.fullmatch('[A-Z][A-Z0-9_]+', code) else 'REQUEST_INVALID')
    except Exception:
        reasons.add('REQUEST_INVALID')
    return _report(reasons, run_id, [items[k] for k in GENERATION_KINDS] if not reasons else [])


def _report(reasons, run_id, generation_inputs):
    return {'status': 'BLOCKED' if reasons else 'CONTRACT_VALID', 'reasons': sorted(reasons),
            'mappings': [], 'run_id': run_id, 'generation_inputs': generation_inputs}
