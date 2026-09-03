import fnmatch, json, hashlib, re, subprocess
from functools import lru_cache
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MOVED_OLD_PATHS=(
    "/".join(("docs", "FDI-PROJECT-OVERVIEW-FRAMEWORK-CENTERED.md")),
    "specs/product-intelligence", "specs/product-knowledge", "specs/source-integration",
    "specs/structural-intelligence", "specs/proposals", "DEVELOPMENT-BACKLOG.md",
    "STATUS.json", "governance/decisions", "MULTICA-HANDOFF.md", "MULTICA-PROJECT-PROMPT.txt",
)
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def lock(): return json.loads((ROOT/'governance/approved-source-lock.json').read_text())
def path_pattern_matches(path, pattern):
    path_parts=path.split('/')
    pattern_parts=pattern.split('/')
    @lru_cache(maxsize=None)
    def matches(pattern_index, path_index):
        if pattern_index == len(pattern_parts):
            return path_index == len(path_parts)
        if pattern_parts[pattern_index] == '**':
            return matches(pattern_index + 1, path_index) or (
                path_index < len(path_parts) and matches(pattern_index, path_index + 1)
            )
        return (
            path_index < len(path_parts)
            and fnmatch.fnmatchcase(path_parts[path_index], pattern_parts[pattern_index])
            and matches(pattern_index + 1, path_index + 1)
        )
    return matches(0, 0)
def stale_path_patterns():
    for old_path in MOVED_OLD_PATHS:
        prefix = r'(?<!/)' if '/' not in old_path else ''
        yield prefix + re.escape(old_path) + r'(?![A-Za-z0-9_.-])'
def local_markdown_links(path):
    text=path.read_text()
    for raw_target in re.findall(r'(?<!!)\[[^]]+\]\(([^)]+)\)', text):
        target=raw_target.strip().strip('<>')
        if target.startswith(('#', 'http://', 'https://', 'mailto:')):
            continue
        yield target.split('#', 1)[0].split('?', 1)[0]
def test_six_governing_modules_are_local():
    mods={m['id']:m for m in lock()['modules']}
    assert set(mods)=={'L1-SEM','L1-IO','L2-FWK','L2-PROFILE','L2-MAINT','FT-T2'}
    for m in mods.values(): assert (ROOT/m['local_path']).exists()
def test_governing_docs_have_real_content():
    for m in lock()['modules']:
        p=ROOT/m['local_path']; text=p.read_text()
        assert len(text)>1500
        assert 'TO_RESOLVE_FROM_EXACT' not in text
def test_five_single_source_hashes_match():
    for m in lock()['modules']:
        if 'sha256' in m: assert sha(ROOT/m['local_path'])==m['sha256']
def test_ft_t2_surface_counts():
    assert len(list((ROOT/'contracts/ft-t2').glob('*.md')))==6
    assert len(list((ROOT/'contracts/ft-t2').glob('*.schema.json')))==6
    assert len(list((ROOT/'skills/ft-t2').glob('*/SKILL.md')))==5
    assert (ROOT/'workflows/ft-t2/FEATURE-CLOSURE.md').exists()
def test_ft_t2_modern_vocabulary():
    paths=list((ROOT/'contracts/ft-t2').glob('*.md'))+list((ROOT/'skills/ft-t2').glob('*/SKILL.md'))+[ROOT/'workflows/ft-t2/FEATURE-CLOSURE.md',ROOT/'specs/approved/ft-t2/FT-T2-GOVERNING-SURFACE.md']
    text='\n'.join(p.read_text() for p in paths)
    assert 'CLOSED_WITHIN_DECLARED_SCOPE' in text
    assert 'ACCEPT_CLOSED_WITHIN_DECLARED_SCOPE' in text
    assert 'PROVISIONALLY_COMPLETE' not in text
def test_all_markdown_basename_is_in_project_tree():
    tree=(ROOT/'PROJECT-TREE.txt').read_text()
    for p in ROOT.rglob('*.md'): assert p.name in tree
def test_overview_and_handoff_exist():
    assert (ROOT/'PROJECT-OVERVIEW.md').exists()
    assert (ROOT/'agent/handoff/MULTICA-HANDOFF.md').exists()
    assert (ROOT/'agent/handoff/MULTICA-PROJECT-PROMPT.txt').exists()
def test_no_governing_placeholder_readmes():
    assert not (ROOT/'contracts/layer1/README.md').exists()
    assert not (ROOT/'contracts/ft-t2/README.md').exists()


def test_repository_navigation_entry_points_exist():
    for path in ("AGENTS.md", "docs/README.md", "docs/FILE-CLASSIFICATION.md"):
        assert (ROOT/path).is_file()


def test_readme_links_to_current_authoritative_entry_points():
    readme=(ROOT/'README.md').read_text()
    for target in ("docs/overview/FDI-PROJECT-OVERVIEW.md", "governance/CURRENT", "docs/planning/STATUS.json"):
        assert f"]({target})" in readme


def test_reorganized_document_targets_exist_and_old_paths_are_absent():
    targets=(
        "docs/overview/FDI-PROJECT-OVERVIEW.md",
        "docs/specifications/framework/FDI-FRAMEWORK-SPECIFICATION-v0.1-rc4.md",
        "docs/specifications/framework/FRAMEWORK-CAPABILITY-FEATURE-CATALOG-v0.1-rc4.md",
        "docs/specifications/framework/SKILL-OWNERSHIP-MAP-v0.1-rc4.md",
        "docs/specifications/providers/graphify/GRAPHIFY-PROVIDER-PROFILE-v0.1-lean-rc4.md",
        "docs/reviews/RC4-REVIEW-FIX-NOTE.md",
        "docs/reviews/SPEC-VERIFICATION.json",
        "docs/specifications/framework/product-intelligence/PRODUCT-INTELLIGENCE-STORE.md",
        "docs/specifications/framework/product-knowledge/PA-01-MINIMAL-PRODUCT-SEMANTICS-PROFILE-v0.1-approval-candidate.md",
        "docs/specifications/framework/product-knowledge/PRODUCT-KNOWLEDGE-MAINTENANCE-PATH-v0.1.md",
        "docs/specifications/framework/source-integration/AZURE-REPOS-EXACT-SOURCE-BINDING.md",
        "docs/specifications/framework/structural-intelligence/FEATURE-DISCOVERY-INTEGRATION-v0.2.md",
        "docs/specifications/framework/structural-intelligence/GRAFEL-ADAPTER-CONTRACT-v0.2.md",
        "docs/specifications/framework/structural-intelligence/GRAFEL-BINDING-ATTESTOR-v0.2.md",
        "docs/specifications/framework/structural-intelligence/MAINTAIN-PRODUCT-INTEGRATION-v0.1.md",
        "docs/specifications/proposals/PA-01/PA-01-MINIMAL-PRODUCT-SEMANTICS-PROFILE-v0.1-approval-candidate.md",
        "docs/planning/DEVELOPMENT-BACKLOG.md",
        "docs/planning/STATUS.json",
        "docs/architecture/decisions/ADR-001-code-intelligence-provider.md",
        "docs/architecture/decisions/ADR-002-product-intelligence-store.md",
        "docs/architecture/decisions/ADR-003-azure-repos-acquisition.md",
        "docs/architecture/decisions/ADR-004-standalone-governing-content.md",
        "agent/handoff/MULTICA-HANDOFF.md",
        "agent/handoff/MULTICA-PROJECT-PROMPT.txt",
    )
    for path in targets:
        assert (ROOT/path).is_file(), path
    for path in MOVED_OLD_PATHS:
        assert not (ROOT/path).exists(), path


def test_project_overview_is_a_resolving_compatibility_pointer():
    overview=(ROOT/'PROJECT-OVERVIEW.md').read_text()
    target="docs/overview/FDI-PROJECT-OVERVIEW.md"
    assert f"]({target})" in overview
    assert (ROOT/target).is_file()


def test_active_non_governing_text_has_no_stale_moved_paths():
    excluded_prefixes=("specs/approved/", "governance/approved/", "docs/superpowers/")
    candidates=subprocess.run(
        ["git", "ls-files", "--cached", "--others", "--exclude-standard"],
        cwd=ROOT, check=True, text=True, capture_output=True,
    ).stdout.splitlines()
    for relative in candidates:
        if relative.startswith(excluded_prefixes) or relative in {"MANIFEST.json", "MARKDOWN-INVENTORY.txt", "PROJECT-TREE.txt", "tests/test_standalone_governance.py"}:
            continue
        path=ROOT/relative
        try:
            text=path.read_text()
        except (UnicodeDecodeError, IsADirectoryError):
            continue
        for old in stale_path_patterns():
            assert not re.search(old, text), f"stale path pattern {old!r} in {relative}"


def test_stale_path_patterns_reject_bare_and_slash_suffixed_directory_references():
    patterns=tuple(stale_path_patterns())
    assert any(re.search(pattern, "See governance/decisions for details") for pattern in patterns)
    assert any(re.search(pattern, "See governance/decisions/ADR-001.md") for pattern in patterns)


def test_stale_path_patterns_do_not_match_sibling_or_filename_suffixes():
    patterns=tuple(stale_path_patterns())
    lookalikes=(
        "specs/proposals-archive",
        "specs/proposals_archive",
        "STATUS.json.bak",
        "MULTICA-PROJECT-PROMPT.txt.old",
    )
    for lookalike in lookalikes:
        assert not any(re.search(pattern, lookalike) for pattern in patterns), lookalike


def test_classification_globs_are_segment_aware_and_double_star_is_recursive():
    source="src/main/java/com/example/App.java"
    assert not path_pattern_matches(source, "src/main/*")
    assert path_pattern_matches(source, "src/main/**")


def test_every_tracked_or_pending_path_has_exactly_one_documented_classification():
    classification=(ROOT/'docs/FILE-CLASSIFICATION.md').read_text()
    match=re.search(r"```classification-rules\n(.*?)\n```", classification, re.DOTALL)
    assert match, "missing machine-readable classification-rules block"
    rules=[]
    for line in match.group(1).splitlines():
        pattern, category=line.split("\t", 1)
        rules.append((pattern, category))
    paths=subprocess.run(
        ["git", "ls-files", "--cached", "--others", "--exclude-standard"],
        cwd=ROOT, check=True, text=True, capture_output=True,
    ).stdout.splitlines()
    for path in paths:
        matches=[category for pattern, category in rules if path_pattern_matches(path, pattern)]
        assert len(matches)==1, f"{path}: expected exactly one classification, got {matches}"


def test_repository_navigation_local_links_resolve_and_dated_links_are_centralized():
    navigation_paths=(ROOT/'README.md', ROOT/'AGENTS.md', ROOT/'docs/README.md')
    for source in navigation_paths:
        for target in local_markdown_links(source):
            assert (source.parent/target).resolve().exists(), f"broken link in {source}: {target}"
    agents=(ROOT/'AGENTS.md').read_text()
    docs=(ROOT/'docs/README.md').read_text()
    assert 'docs/superpowers/specs/2026-' not in agents
    assert '(superpowers/specs/2026-09-03-graphify-provider-migration-design.md)' in docs
    assert '(superpowers/specs/2026-09-04-project-folder-reorganization-design.md)' in docs
    assert '(superpowers/plans/2026-09-04-project-folder-reorganization.md)' in docs


def test_markdown_inventory_is_exact():
    actual=sorted(p.relative_to(ROOT).as_posix() for p in ROOT.rglob("*.md") if not any(x in {".pytest_cache","__pycache__",".git"} for x in p.relative_to(ROOT).parts))
    inv=[x.strip() for x in (ROOT/"MARKDOWN-INVENTORY.txt").read_text().splitlines() if x.strip()]
    assert inv==actual
