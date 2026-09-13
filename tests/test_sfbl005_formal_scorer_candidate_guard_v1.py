import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys


TOOL = Path(__file__).resolve().parents[1] / "tools" / "sfbl005_formal_scorer_candidate_guard_v1.py"


def git(repo: Path, *args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["git", *args], cwd=repo, check=check, text=True,
        stdout=subprocess.PIPE, stderr=subprocess.PIPE,
    )


def commit_file(repo: Path, path: str, content: bytes, mode: int = 0o644) -> str:
    target = repo / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(content)
    target.chmod(mode)
    git(repo, "add", path)
    git(repo, "commit", "-m", path)
    return git(repo, "rev-parse", "HEAD").stdout.strip()


def repo_with_base(tmp_path: Path) -> tuple[Path, str]:
    repo = tmp_path / "repo"
    repo.mkdir()
    git(repo, "init", "-q")
    git(repo, "config", "user.email", "test@example.invalid")
    git(repo, "config", "user.name", "Test")
    base = commit_file(repo, "README.md", b"base\n")
    return repo, base


def run_guard(repo: Path, base: str, candidate: str, output: Path,
              profile: str = "prep") -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [sys.executable, str(TOOL), "--profile", profile,
         "--base", base, "--candidate", candidate,
         "--source-manifest-output", str(output)],
        cwd=repo, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
    )


def test_accepts_allowed_change_and_writes_canonical_object_manifest(tmp_path: Path) -> None:
    repo, base = repo_with_base(tmp_path)
    candidate = commit_file(
        repo,
        "src/main/java/com/featuredeliveryintelligence/fdi/product/realization/formalholdout/v1/Scorer.java",
        b"final class Scorer {}\n",
    )
    # A dirty tracked worktree must not affect object-based validation or output.
    (repo / "README.md").write_text("dirty worktree\n", encoding="utf-8")
    output = repo / "source-manifest.json"

    result = run_guard(repo, base, candidate, output)

    assert result.returncode == 0, result.stderr
    raw = output.read_bytes()
    assert raw.endswith(b"\n") and not raw.endswith(b"\n\n")
    assert raw == json.dumps(json.loads(raw), ensure_ascii=False, sort_keys=True,
                             separators=(",", ":")).encode("utf-8") + b"\n"
    document = json.loads(raw)
    assert document["schemaVersion"] == "sfbl005-source-set-manifest-v1"
    assert document["baseCommit"] == base
    assert document["candidateCommit"] == candidate
    entries = document["entries"]
    assert [entry["path"] for entry in entries] == sorted(
        (entry["path"] for entry in entries), key=lambda p: p.encode("utf-8")
    )
    scorer = next(entry for entry in entries if entry["path"].endswith("Scorer.java"))
    expected = b"final class Scorer {}\n"
    assert scorer["mode"] == "100644"
    assert scorer["bytes"] == len(expected)
    assert scorer["sha256"] == hashlib.sha256(expected).hexdigest()
    assert len(scorer["blob"]) == 40


def test_rejects_change_outside_exact_allowlist_and_writes_nothing(tmp_path: Path) -> None:
    repo, base = repo_with_base(tmp_path)
    candidate = commit_file(repo, "README.md", b"changed\n")
    output = repo / "source-manifest.json"

    result = run_guard(repo, base, candidate, output)

    assert result.returncode != 0
    assert "not allowlisted" in result.stderr
    assert not output.exists()


def test_rejects_legacy_prefix_even_if_an_allowed_path_also_changes(tmp_path: Path) -> None:
    repo, base = repo_with_base(tmp_path)
    commit_file(
        repo,
        "src/main/java/com/featuredeliveryintelligence/fdi/product/realization/formalholdout/v1/Scorer.java",
        b"ok\n",
    )
    candidate = commit_file(
        repo,
        "src/main/java/com/featuredeliveryintelligence/fdi/product/realization/methodpair/Legacy.java",
        b"changed\n",
    )
    output = repo / "source-manifest.json"

    result = run_guard(repo, base, candidate, output)

    assert result.returncode != 0
    assert "legacy path" in result.stderr
    assert not output.exists()


def test_rejects_abbreviated_or_non_ancestor_commit_ids(tmp_path: Path) -> None:
    repo, base = repo_with_base(tmp_path)
    candidate = commit_file(
        repo,
        "src/main/java/com/featuredeliveryintelligence/fdi/application/FormalHoldoutScoreCli.java",
        b"candidate\n",
    )
    output = repo / "source-manifest.json"
    short_result = run_guard(repo, base[:12], candidate, output)
    git(repo, "checkout", "-q", base)
    sibling = commit_file(
        repo,
        "src/main/java/com/featuredeliveryintelligence/fdi/application/FormalHoldoutScoreCli.java",
        b"sibling\n",
    )
    ancestor_result = run_guard(repo, candidate, sibling, output)

    assert short_result.returncode != 0
    assert "full 40-hex commit" in short_result.stderr
    assert ancestor_result.returncode != 0
    assert "ancestor" in ancestor_result.stderr
    assert not output.exists()


def test_rejects_existing_symlink_or_escaping_output(tmp_path: Path) -> None:
    repo, base = repo_with_base(tmp_path)
    candidate = commit_file(
        repo,
        "src/test/java/com/featuredeliveryintelligence/fdi/application/FormalHoldoutScoreCliTests.java",
        b"test\n",
    )
    existing = repo / "existing.json"
    existing.write_text("do not replace", encoding="utf-8")
    target = repo / "target.json"
    symlink = repo / "link.json"
    symlink.symlink_to(target)
    escape = repo / ".." / "escape.json"

    existing_result = run_guard(repo, base, candidate, existing)
    symlink_result = run_guard(repo, base, candidate, symlink)
    escape_result = run_guard(repo, base, candidate, escape)

    assert existing_result.returncode != 0
    assert symlink_result.returncode != 0
    assert escape_result.returncode != 0
    assert not target.exists()
    assert not escape.exists()


def test_rejects_governance_artifacts_under_formal_scorer_root(tmp_path: Path) -> None:
    forbidden = (
        "independent-review.json",
        "h1-scorer-receipt.json",
        "execution-envelope.json",
        "command-ledger.jsonl",
    )
    for index, name in enumerate(forbidden):
        case = tmp_path / str(index)
        case.mkdir()
        repo, base = repo_with_base(case)
        candidate = commit_file(
            repo,
            f"validation/software-factory/sf-bl005/formal-holdout-scorer-001/{name}",
            b"forbidden\n",
        )
        output = repo / "source-manifest.json"

        result = run_guard(repo, base, candidate, output)

        assert result.returncode != 0, name
        assert "not allowlisted" in result.stderr
        assert not output.exists()


def test_implementation_profile_accepts_only_implementation_sources_and_tests(tmp_path: Path) -> None:
    repo, base = repo_with_base(tmp_path)
    commit_file(
        repo,
        "src/main/java/com/featuredeliveryintelligence/fdi/product/realization/formalholdout/v1/Scorer.java",
        b"final class Scorer {}\n",
    )
    candidate = commit_file(
        repo,
        "tools/sfbl005_formal_holdout_recompute_v1.py",
        b"# clean-room recomputer\n",
    )
    output = repo / "source-manifest.json"

    result = run_guard(repo, base, candidate, output, profile="implementation")

    assert result.returncode == 0, result.stderr
    assert output.exists()


def test_implementation_profile_accepts_each_exact_contract_path(tmp_path: Path) -> None:
    allowed = (
        "contracts/sfbl005-formal-holdout-scorer-v1.schema.json",
        "src/main/java/com/featuredeliveryintelligence/fdi/application/FormalHoldoutScoreCli.java",
        "src/test/java/com/featuredeliveryintelligence/fdi/application/FormalHoldoutScoreCliTests.java",
        "tests/test_sfbl005_formal_holdout_recompute_v1.py",
    )
    for index, path in enumerate(allowed):
        case = tmp_path / str(index)
        case.mkdir()
        repo, base = repo_with_base(case)
        candidate = commit_file(repo, path, b"implementation\n")
        output = repo / "source-manifest.json"

        result = run_guard(repo, base, candidate, output, profile="implementation")

        assert result.returncode == 0, f"{path}: {result.stderr}"
        assert output.exists()


def test_implementation_profile_rejects_prep_and_governance_paths(tmp_path: Path) -> None:
    forbidden = (
        "src/main/java/com/featuredeliveryintelligence/fdi/application/FdiApplication.java",
        "tools/sfbl005_formal_scorer_vector_verify_v1.py",
        "tests/test_sfbl005_formal_scorer_candidate_guard_v1.py",
        "validation/software-factory/sf-bl005/formal-holdout-scorer-001/conformance/catalog.json",
        "validation/software-factory/sf-bl005/formal-holdout-scorer-001/conformance/inputs/C01.json",
        "validation/software-factory/sf-bl005/formal-holdout-scorer-001/independent-review.json",
        "validation/software-factory/sf-bl005/execution-envelope-formal-holdout-scorer-001-r2.json",
    )
    for index, path in enumerate(forbidden):
        case = tmp_path / str(index)
        case.mkdir()
        repo, base = repo_with_base(case)
        candidate = commit_file(repo, path, b"forbidden\n")
        output = repo / "source-manifest.json"

        result = run_guard(repo, base, candidate, output, profile="implementation")

        assert result.returncode != 0, path
        assert "not allowlisted" in result.stderr, path
        assert not output.exists()


def test_base_object_guard_cannot_be_weakened_by_candidate_guard_change(tmp_path: Path) -> None:
    repo, _ = repo_with_base(tmp_path)
    base_tool = repo / "tools/sfbl005_formal_scorer_candidate_guard_v1.py"
    base_tool.parent.mkdir(parents=True)
    base_tool.write_bytes(TOOL.read_bytes())
    git(repo, "add", str(base_tool.relative_to(repo)))
    git(repo, "commit", "-m", "guard base")
    base = git(repo, "rev-parse", "HEAD").stdout.strip()
    base_program = git(repo, "show", f"{base}:tools/sfbl005_formal_scorer_candidate_guard_v1.py").stdout
    base_tool.write_text("raise SystemExit(0)\n", encoding="utf-8")
    forbidden = repo / "validation/software-factory/sf-bl005/formal-holdout-scorer-001/conformance/catalog.json"
    forbidden.parent.mkdir(parents=True)
    forbidden.write_text("{}\n", encoding="utf-8")
    git(repo, "add", ".")
    git(repo, "commit", "-m", "try to weaken guard")
    candidate = git(repo, "rev-parse", "HEAD").stdout.strip()
    output = repo / "source-manifest.json"

    result = subprocess.run(
        [sys.executable, "-", "--profile", "implementation", "--base", base,
         "--candidate", candidate, "--source-manifest-output", str(output)],
        input=base_program, cwd=repo, text=True,
        stdout=subprocess.PIPE, stderr=subprocess.PIPE,
    )

    assert result.returncode != 0
    assert "not allowlisted" in result.stderr
    assert not output.exists()
