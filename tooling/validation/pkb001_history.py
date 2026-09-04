#!/usr/bin/env python3
"""Reconstruct immutable, cutoff-bounded delivery history for PKB-001."""

import argparse
import json
import re
import subprocess
from datetime import datetime
from pathlib import Path
from typing import List, Optional


GIT_SHA = re.compile(r'^[0-9a-f]{40}$')


def _instant(value: str) -> datetime:
    parsed = datetime.fromisoformat(value.replace('Z', '+00:00'))
    if parsed.tzinfo is None:
        raise ValueError('timestamp must include a timezone')
    return parsed


def _git(repo: Path, *args: str) -> str:
    return subprocess.run(
        ['git', *args], cwd=repo, check=True, text=True,
        capture_output=True,
    ).stdout.strip()


def reconstruct_history(repo: Path, source_sha: str, cutoff: str, prs: list) -> dict:
    repo = Path(repo).resolve(strict=True)
    if not GIT_SHA.fullmatch(source_sha):
        raise ValueError('source SHA must be a full 40-character lowercase Git SHA')
    cutoff_instant = _instant(cutoff)
    resolved = _git(repo, 'rev-parse', '--verify', source_sha + '^{commit}')
    if resolved != source_sha:
        raise ValueError('source SHA does not resolve to the requested commit')

    commits = []
    included_shas = set()
    for commit_sha in _git(repo, 'rev-list', '--reverse', source_sha).splitlines():
        committed_at = _git(repo, 'show', '-s', '--format=%cI', commit_sha)
        if _instant(committed_at) > cutoff_instant:
            continue
        paths = _git(
            repo, 'diff-tree', '--root', '--no-commit-id', '--name-only', '-r',
            commit_sha,
        ).splitlines()
        commits.append({
            'commit_sha': commit_sha,
            'committed_at': committed_at,
            'subject': _git(repo, 'show', '-s', '--format=%s', commit_sha),
            'changed_paths': sorted(path for path in paths if path),
        })
        included_shas.add(commit_sha)

    pull_requests = []
    for pr in prs:
        if (not isinstance(pr, dict)
                or not isinstance(pr.get('createdAt'), str)
                or not isinstance(pr.get('updatedAt'), str)):
            continue
        matching = sorted({
            commit.get('oid') for commit in pr.get('commits', [])
            if isinstance(commit, dict) and commit.get('oid') in included_shas
        })
        if (_instant(pr['createdAt']) > cutoff_instant
                or _instant(pr['updatedAt']) > cutoff_instant
                or not matching):
            continue
        merge = pr.get('mergeCommit')
        pull_requests.append({
            'number': pr.get('number'),
            'title': pr.get('title'),
            'state': pr.get('state'),
            'url': pr.get('url'),
            'created_at': pr['createdAt'],
            'updated_at': pr.get('updatedAt'),
            'head_ref_oid': pr.get('headRefOid'),
            'merge_commit_sha': merge.get('oid') if isinstance(merge, dict) else None,
            'included_commit_shas': matching,
        })

    return {
        'dataset_id': 'pkb001-delivery-history-v1',
        'status': 'FROZEN',
        'source_commit_sha': source_sha,
        'history_cutoff': cutoff,
        'post_cutoff_knowledge_policy': 'EXCLUDE_AFTER_CUTOFF',
        'commits': commits,
        'pull_requests': sorted(pull_requests, key=lambda item: item['number']),
        'evidence_boundary': 'Git and pull-request evidence only; not Product truth.',
        'limitations': [
            'Pull requests are included only when supplied metadata links them to an included commit.',
            'Events created after the cutoff are excluded even when later repository state exposes them.',
        ],
    }


def main(argv: Optional[List[str]] = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', type=Path, required=True)
    parser.add_argument('--source-sha', required=True)
    parser.add_argument('--cutoff', required=True)
    parser.add_argument('--prs', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args(argv)
    try:
        raw_prs = json.loads(args.prs.read_text())
        prs = raw_prs if isinstance(raw_prs, list) else [raw_prs]
        result = reconstruct_history(args.repo, args.source_sha, args.cutoff, prs)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, indent=2) + '\n')
        return 0
    except (OSError, ValueError, json.JSONDecodeError, subprocess.CalledProcessError) as error:
        print(json.dumps({'status': 'ERROR', 'error': str(error)}))
        return 1


if __name__ == '__main__':
    raise SystemExit(main())
