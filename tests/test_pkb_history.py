import subprocess
from pathlib import Path

from tooling.validation.pkb001_history import reconstruct_history


def git(repo, *args):
    return subprocess.run(['git', *args], cwd=repo, check=True, text=True,
                          capture_output=True).stdout.strip()


def test_history_is_ancestor_and_cutoff_bounded(tmp_path):
    git(tmp_path, 'init', '-q')
    git(tmp_path, 'config', 'user.email', 'test@example.com')
    git(tmp_path, 'config', 'user.name', 'Test')
    commits = []
    for name, date in [('a.txt', '2026-01-01T00:00:00Z'),
                       ('b.txt', '2026-01-03T00:00:00Z')]:
        (tmp_path/name).write_text(name)
        git(tmp_path, 'add', name)
        subprocess.run(['git', 'commit', '-q', '-m', name], cwd=tmp_path, check=True,
                       env={'PATH': '/usr/bin:/bin', 'GIT_AUTHOR_DATE': date,
                            'GIT_COMMITTER_DATE': date})
        commits.append(git(tmp_path, 'rev-parse', 'HEAD'))
    result = reconstruct_history(tmp_path, commits[-1], '2026-01-02T00:00:00Z', [])
    assert [item['commit_sha'] for item in result['commits']] == [commits[0]]
    assert result['commits'][0]['changed_paths'] == ['a.txt']
    assert result['post_cutoff_knowledge_policy'] == 'EXCLUDE_AFTER_CUTOFF'


def test_history_excludes_pull_request_metadata_updated_after_cutoff(tmp_path):
    git(tmp_path, 'init', '-q')
    git(tmp_path, 'config', 'user.email', 'test@example.com')
    git(tmp_path, 'config', 'user.name', 'Test')
    (tmp_path/'a.txt').write_text('a')
    git(tmp_path, 'add', 'a.txt')
    subprocess.run(
        ['git', 'commit', '-q', '-m', 'a'], cwd=tmp_path, check=True,
        env={'PATH': '/usr/bin:/bin', 'GIT_AUTHOR_DATE': '2026-01-01T00:00:00Z',
             'GIT_COMMITTER_DATE': '2026-01-01T00:00:00Z'},
    )
    commit_sha = git(tmp_path, 'rev-parse', 'HEAD')
    pull_request = {
        'number': 1, 'title': 'late update', 'state': 'MERGED',
        'url': 'https://example.test/pr/1',
        'createdAt': '2026-01-01T01:00:00Z',
        'updatedAt': '2026-01-03T00:00:00Z',
        'commits': [{'oid': commit_sha}],
    }
    result = reconstruct_history(
        tmp_path, commit_sha, '2026-01-02T00:00:00Z', [pull_request])
    assert result['pull_requests'] == []
