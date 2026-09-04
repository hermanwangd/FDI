#!/usr/bin/env python3
"""Deterministic code baseline; this does not execute PK-S1 or PK-S2."""

import argparse
import json
import re
from collections import defaultdict
from pathlib import Path
from typing import Dict, List, Optional


ALLOWED = {
    'R1': frozenset({'structure'}),
    'R2': frozenset({'history'}),
    'R3': frozenset({'structure', 'history'}),
    'F1': frozenset({'structure', 'semantics'}),
}
GIT_SHA = re.compile(r'^[0-9a-f]{40}$')
SHA256 = re.compile(r'^[0-9a-f]{64}$')


def _area(path: str) -> str:
    parts = Path(path).parts
    if 'fdi' in parts and parts.index('fdi') + 1 < len(parts):
        candidate = parts[parts.index('fdi') + 1]
        return 'application' if Path(candidate).suffix else candidate
    return Path(path).parent.name


def _files_by_area(structure: dict) -> Dict[str, List[dict]]:
    grouped = defaultdict(list)
    seen = set()
    for node in structure.get('nodes', []):
        path = node.get('source_file')
        if not isinstance(path, str) or not path or not node.get('id'):
            continue
        key = (path, node['id'])
        if key in seen:
            continue
        seen.add(key)
        grouped[_area(path)].append(node)
    return {key: sorted(value, key=lambda row: (row['source_file'], row['id']))
            for key, value in grouped.items()}


def _history_by_area(history: dict) -> Dict[str, List[dict]]:
    grouped = defaultdict(list)
    for commit in history.get('commits', []):
        areas = {_area(path) for path in commit.get('changed_paths', [])
                 if isinstance(path, str) and path.startswith('src/')}
        for area in areas:
            grouped[area].append(commit)
    return {key: sorted({row['commit_sha']: row for row in value}.values(),
                        key=lambda row: row['commit_sha'])
            for key, value in grouped.items()}


def _identity(inputs: dict) -> tuple:
    revisions = {
        value.get('source_commit_sha') or value.get('applicable_source_commit_sha')
        for value in inputs.values()
    }
    if len(revisions) != 1 or not GIT_SHA.fullmatch(next(iter(revisions), '')):
        raise ValueError('all inputs must bind the same full source commit SHA')
    source_sha = next(iter(revisions))
    structure = inputs.get('structure', {})
    graph_sha = structure.get('graph_sha256', '0' * 64)
    if not SHA256.fullmatch(graph_sha):
        raise ValueError('structure graph SHA-256 is invalid')
    return source_sha, graph_sha


def _proposal(arm: str, identifier: str, label: str, components: list,
              evidence: list, confidence: float, limitations: list) -> dict:
    return {
        'proposal_id': f'{arm}-{identifier}',
        'arm': arm,
        'target_id': identifier,
        'relation_type': 'REALIZES' if arm == 'F1' else 'CAPABILITY_HYPOTHESIS',
        'operation': 'CREATE',
        'label': label,
        'component_refs': sorted(set(components)),
        'evidence_refs': sorted(set(evidence)),
        'confidence': confidence,
        'limitations': limitations,
        'authority_status': 'PROPOSAL_ONLY',
    }


def generate_arm(arm: str, inputs: dict) -> dict:
    if arm not in ALLOWED or frozenset(inputs) != ALLOWED[arm]:
        raise ValueError('input categories do not match arm allowlist')
    source_sha, graph_sha = _identity(inputs)
    proposals = []

    if arm == 'F1':
        semantics = inputs['semantics']
        if semantics.get('status') != 'FROZEN' or semantics.get('owner') != 'PRODUCT_TEAM':
            raise ValueError('F1 requires frozen Product Team semantics')
        nodes = _files_by_area(inputs['structure'])
        all_nodes = [node for rows in nodes.values() for node in rows]
        for capability in semantics.get('capabilities', []):
            boundaries = capability.get('expected_realization_boundary', [])
            matched = [node for node in all_nodes if any(
                node['source_file'].startswith(boundary) for boundary in boundaries)]
            proposals.append(_proposal(
                arm, capability['capability_id'], capability['name'],
                [node['source_file'] for node in matched],
                [f"graph-node:{node['id']}@{node.get('source_location', 'UNKNOWN')}"
                 for node in matched],
                0.9 if matched else 0.0,
                [] if matched else ['No graph node was found inside the declared boundary.'],
            ))
    elif arm == 'R1':
        for area, nodes in sorted(_files_by_area(inputs['structure']).items()):
            proposals.append(_proposal(
                arm, area, f'{area.title()} structural responsibility',
                [node['source_file'] for node in nodes],
                [f"graph-node:{node['id']}@{node.get('source_location', 'UNKNOWN')}"
                 for node in nodes], 0.45,
                ['Structure alone does not establish Product meaning.'],
            ))
    elif arm == 'R2':
        history = inputs['history']
        if (history.get('status') != 'FROZEN'
                or history.get('post_cutoff_knowledge_policy') != 'EXCLUDE_AFTER_CUTOFF'):
            raise ValueError('R2 requires frozen cutoff-bounded history')
        for area, commits in sorted(_history_by_area(history).items()):
            paths = [path for commit in commits for path in commit.get('changed_paths', [])
                     if _area(path) == area]
            proposals.append(_proposal(
                arm, area, f'{area.title()} delivery responsibility', paths,
                ['git-commit:' + commit['commit_sha'] for commit in commits], 0.4,
                ['Delivery history is evidence, not Product truth.'],
            ))
    else:
        structural = _files_by_area(inputs['structure'])
        historical = _history_by_area(inputs['history'])
        for area in sorted(set(structural).intersection(historical)):
            nodes, commits = structural[area], historical[area]
            proposals.append(_proposal(
                arm, area, f'{area.title()} implemented delivery capability',
                [node['source_file'] for node in nodes],
                (['git-commit:' + commit['commit_sha'] for commit in commits]
                 + [f"graph-node:{node['id']}@{node.get('source_location', 'UNKNOWN')}"
                    for node in nodes]), 0.65,
                ['Capability label requires Product Team review.'],
            ))

    return {
        'set_id': f'pkb001-{arm.lower()}-proposals-v1',
        'run_id': f'PKB001-{arm}-510a397',
        'arm': arm,
        'source_commit_sha': source_sha,
        'graph_artifact_sha256': graph_sha,
        'authority_status': 'PROPOSAL_ONLY',
        'proposals': proposals,
    }


def main(argv: Optional[List[str]] = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--arm', choices=sorted(ALLOWED), required=True)
    parser.add_argument('--input', action='append', default=[])
    parser.add_argument('--source-sha')
    parser.add_argument('--graph-sha')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args(argv)
    try:
        inputs = {}
        for binding in args.input:
            category, separator, filename = binding.partition('=')
            if not separator or category in inputs:
                raise ValueError('inputs must use unique category=path bindings')
            inputs[category] = json.loads(Path(filename).read_text())
        if 'structure' in inputs:
            inputs['structure']['source_commit_sha'] = args.source_sha
            inputs['structure']['graph_sha256'] = args.graph_sha
        result = generate_arm(args.arm, inputs)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, indent=2) + '\n')
        return 0
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(json.dumps({'status': 'ERROR', 'error': str(error)}))
        return 1


if __name__ == '__main__':
    raise SystemExit(main())
