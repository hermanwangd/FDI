#!/usr/bin/env python3
"""Prove Graphify's live MCP stdio interface against the frozen PKB-001 graph."""

from __future__ import annotations

import argparse
import asyncio
import hashlib
import importlib.metadata
import json
import os
import re
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


SOURCE_ARCHIVE_SHA256 = '8d806aa861e0ffa2136eda227d79d290dfdb89bf0c63fd00a4e2b4ea59d445'
ZIP_REVISION_COMMENT = '91f4d120b630ee35c79bf3c75ccd186870a808f9'
PETCLINIC_COMMIT = '818c4136ea971c21674525f9053de0d9c7ad8cfe'
PETCLINIC_INPUT_TREE = 'f92df0b05c91c7d29d81e70cf86f8678b0545bd2'
PETCLINIC_GRAPH_SHA256 = 'e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e'
EXPECTED_OPERATIONS = [
    'query_graph', 'get_node', 'get_neighbors', 'get_community', 'god_nodes',
    'graph_stats', 'shortest_path',
]
HOPS = re.compile(r'^Shortest path \((\d+) hops\):')


class VerificationFailure(RuntimeError):
    """Raised when evidence cannot truthfully be marked EXACTLY_BOUND."""


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _load_json(path: Path) -> dict[str, Any]:
    data = json.loads(path.read_text())
    if not isinstance(data, dict):
        raise VerificationFailure(f'{path} must contain a JSON object')
    return data


def _text_result(result: dict[str, Any]) -> str:
    content = result.get('content')
    if not isinstance(content, list) or not content:
        raise VerificationFailure('MCP tool returned no content')
    first = content[0]
    if not isinstance(first, dict) or not isinstance(first.get('text'), str):
        raise VerificationFailure('MCP tool did not return text content')
    return first['text']


def _runtime_paths(root: Path) -> tuple[Path, Path, Path]:
    runtime_python = root/'.fdi-work/graphify-venv312/bin/python'
    graph = root/'validation/pkb001/artifacts/petclinic-graph-818c413.json'
    source = root/'.fdi-work/graphify-source/graphify-main'
    if not runtime_python.is_file():
        raise VerificationFailure(f'Graphify runtime is missing: {runtime_python}')
    if not graph.is_file():
        raise VerificationFailure(f'frozen Petclinic graph is missing: {graph}')
    if not source.is_dir():
        raise VerificationFailure(f'Graphify frozen source is missing: {source}')
    return runtime_python, graph.resolve(), source.resolve()


def _ensure_runtime(root: Path) -> None:
    runtime_python, _, _ = _runtime_paths(root)
    if Path(sys.prefix) != runtime_python.parent.parent:
        os.execv(str(runtime_python), [str(runtime_python), *sys.argv])


def _validate_frozen_inputs(root: Path, graph: Path) -> tuple[dict[str, Any], str]:
    candidate = _load_json(root/'validation/pkb001/datasets/petclinic-calibration-candidate.json')
    graphify_input = candidate.get('graphify_input')
    graphify = candidate.get('graphify')
    if not isinstance(graphify_input, dict) or not isinstance(graphify, dict):
        raise VerificationFailure('Petclinic calibration binding is incomplete')
    checks = {
        'candidate source commit': candidate.get('source_commit_sha') == PETCLINIC_COMMIT,
        'candidate input Git tree': graphify_input.get('git_tree_oid') == PETCLINIC_INPUT_TREE,
        'candidate graph path': graphify.get('artifact_path') == 'validation/pkb001/artifacts/petclinic-graph-818c413.json',
        'candidate graph SHA-256': graphify.get('artifact_sha256') == PETCLINIC_GRAPH_SHA256,
        'frozen graph SHA-256': _sha256(graph) == PETCLINIC_GRAPH_SHA256,
    }
    failed = [name for name, valid in checks.items() if not valid]
    if failed:
        raise VerificationFailure('frozen input validation failed: ' + ', '.join(failed))
    phase0 = _load_json(root/'validation/pkb001/datasets/phase0-evidence.json')
    existing = phase0.get('graphify')
    if not isinstance(existing, dict) or not isinstance(existing.get('input_policy_sha256'), str):
        raise VerificationFailure('Phase 0 Graphify input policy digest is absent')
    return candidate, existing['input_policy_sha256']


def _direct_url() -> str:
    distribution = importlib.metadata.distribution('graphifyy')
    direct_url_file = next(
        (file for file in distribution.files or () if str(file).endswith('direct_url.json')),
        None,
    )
    if direct_url_file is None:
        raise VerificationFailure('graphifyy direct_url.json is absent')
    data = _load_json(Path(distribution.locate_file(direct_url_file)))
    url = data.get('url')
    if not isinstance(url, str) or not url:
        raise VerificationFailure('graphifyy direct URL is absent')
    return url


def _require_tool_schema(catalog: dict[str, dict[str, Any]], name: str, fields: set[str]) -> None:
    schema = catalog.get(name)
    properties = schema.get('properties') if isinstance(schema, dict) else None
    if not isinstance(properties, dict) or not fields.issubset(properties):
        raise VerificationFailure(f'live MCP tool {name} does not expose required arguments')


async def verify_live_interface(root: Path) -> dict[str, Any]:
    runtime_python, graph, source = _runtime_paths(root)
    _, input_policy_sha256 = _validate_frozen_inputs(root, graph)
    runtime_version = importlib.metadata.version('graphifyy')
    mcp_version = importlib.metadata.version('mcp')
    direct_url = _direct_url()
    expected_direct_url = source.as_uri()
    if direct_url != expected_direct_url:
        raise VerificationFailure('installed graphifyy direct URL does not bind the frozen source')
    if runtime_version != '0.1.14' or mcp_version != '1.29.1':
        raise VerificationFailure('installed Graphify or MCP version does not match the frozen runtime')

    from mcp import ClientSession, StdioServerParameters
    from mcp.client.stdio import stdio_client

    with tempfile.TemporaryDirectory(prefix='pkb001-graphify-mcp-', dir=root/'.fdi-work') as workdir:
        workdir_path = Path(workdir)
        # Upstream accepts only paths below graphify-out/.  Point that directory
        # at the immutable artifact directory, then invoke the server through
        # that path without copying or modifying graph bytes.
        (workdir_path/'graphify-out').symlink_to(graph.parent, target_is_directory=True)
        server_graph_argument = Path('graphify-out')/graph.name
        server_args = ['-m', 'graphify.serve', str(server_graph_argument)]
        server = StdioServerParameters(
            command=str(runtime_python), args=server_args, cwd=workdir_path)
        async with stdio_client(server) as (reader, writer):
            async with ClientSession(reader, writer) as session:
                initialization = await session.initialize()
                listed = await session.list_tools()
                tool_catalog = [tool.model_dump(mode='json') for tool in listed.tools]
                operations = [tool['name'] for tool in tool_catalog]
                if operations != EXPECTED_OPERATIONS:
                    raise VerificationFailure('live MCP tool list differs from frozen expected operations')
                schemas = {tool['name']: tool['inputSchema'] for tool in tool_catalog}
                _require_tool_schema(schemas, 'get_node', {'label'})
                _require_tool_schema(schemas, 'shortest_path', {'source', 'target', 'max_hops'})

                node_arguments = {'label': 'PetClinicApplication.java'}
                node_result = (await session.call_tool('get_node', node_arguments)).model_dump(mode='json')
                node_text = _text_result(node_result)
                if node_result.get('isError') or 'Node: PetClinicApplication.java' not in node_text:
                    raise VerificationFailure('live get_node query did not resolve the frozen Petclinic node')

                path_arguments = {
                    'source': 'PetClinicApplication.java',
                    'target': 'PetClinicApplication',
                    'max_hops': 1,
                }
                path_result = (await session.call_tool('shortest_path', path_arguments)).model_dump(mode='json')
                path_text = _text_result(path_result)
                match = HOPS.match(path_text)
                if path_result.get('isError') or match is None:
                    raise VerificationFailure('live shortest_path query did not return a path')
                observed_hops = int(match.group(1))
                if not 0 < observed_hops <= path_arguments['max_hops']:
                    raise VerificationFailure('live shortest_path query exceeded requested max_hops')
                expected_path = 'PetClinicApplication.java --contains [EXTRACTED]--> PetClinicApplication'
                if expected_path not in path_text:
                    raise VerificationFailure('live shortest_path query returned an unexpected path')

    return {
        'verification_id': 'pkb001-graphify-live-818c413',
        'captured_at': datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace('+00:00', 'Z'),
        'result': 'EXACTLY_BOUND',
        'queryable': True,
        'runtime_identity': 'graphifyy',
        'runtime_version': runtime_version,
        'runtime_python': str(runtime_python),
        'transport': 'MCP stdio',
        'mcp_version': mcp_version,
        'wire_version': f'MCP {initialization.protocolVersion}',
        'server_info': initialization.serverInfo.model_dump(mode='json'),
        'server_command': [str(runtime_python), *server_args],
        'resolved_graph_path': str(graph),
        'server_exit_status': 'CLEAN_SESSION_CLOSE',
        'server_error': None,
        'source_provenance': {
            'source_archive_sha256': SOURCE_ARCHIVE_SHA256,
            'zip_revision_comment': ZIP_REVISION_COMMENT,
            'installed_direct_url': direct_url,
        },
        'supported_operations': operations,
        'tool_catalog': tool_catalog,
        'queries': {
            'node_query': {
                'tool': 'get_node', 'arguments': node_arguments,
                'result': node_result, 'is_error': False,
            },
            'shortest_path': {
                'tool': 'shortest_path', 'arguments': path_arguments,
                'result': path_result, 'is_error': False,
                'returned_path': expected_path, 'observed_hops': observed_hops,
            },
        },
        'exact_revision_opened': True,
        'source_location_provenance': (
            f'git tree {PETCLINIC_INPUT_TREE} at immutable commit {PETCLINIC_COMMIT}'),
        'snapshot_binding': {
            'requested_revision': PETCLINIC_COMMIT,
            'indexed_revision': PETCLINIC_COMMIT,
            'input_git_tree_oid': PETCLINIC_INPUT_TREE,
            'graph_sha256': PETCLINIC_GRAPH_SHA256,
        },
        'graph_sha256': PETCLINIC_GRAPH_SHA256,
        'input_policy_sha256': input_policy_sha256,
        'structural_proof': {'node_query': True, 'path_query': True},
        'limitations': [
            'Graphify graph.json does not embed Git revision metadata; the binding is verified against the frozen calibration candidate and graph digest.',
            'The upstream graph path guard requires graphify-out/; a temporary symlink targets the immutable artifact without copying its bytes.',
            'Only deterministic Java AST extraction was used; no semantic LLM extraction was executed.',
        ],
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path.cwd())
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args(argv)
    root = args.root.resolve()
    try:
        _ensure_runtime(root)
        evidence = asyncio.run(verify_live_interface(root))
        exit_code = 0
    except (
        importlib.metadata.PackageNotFoundError,
        ModuleNotFoundError,
        OSError,
        RuntimeError,
        ValueError,
        json.JSONDecodeError,
    ) as error:
        evidence = {
            'verification_id': 'pkb001-graphify-live-818c413',
            'result': 'NOT_BOUND', 'queryable': False,
            'server_exit_status': 'ERROR', 'server_error': str(error),
        }
        exit_code = 2
    rendered = json.dumps(evidence, indent=2) + '\n'
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(rendered)
    print(rendered, end='')
    return exit_code


if __name__ == '__main__':
    raise SystemExit(main())
