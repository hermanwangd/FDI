import fnmatch, importlib.util, json, hashlib, re, subprocess
import pytest
from functools import lru_cache
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MOVED_OLD_PATHS=(
    "/".join(("docs", "FDI-PROJECT-OVERVIEW-FRAMEWORK-CENTERED.md")),
    "specs/product-intelligence", "specs/product-knowledge", "specs/source-integration",
    "specs/structural-intelligence", "specs/proposals", "DEVELOPMENT-BACKLOG.md",
    "STATUS.json", "governance/decisions", "MULTICA-HANDOFF.md", "MULTICA-PROJECT-PROMPT.txt",
    "specs/approved/layer1", "specs/approved/layer2", "specs/approved/ft-t2",
    "governance/approved-source-lock.json",
    "contracts/layer1", "contracts/layer2", "contracts/ft-t2",
    "contracts/source-integration", "contracts/structural-intelligence",
    "skills", "workflows",
    "validation/skill-behavior", "validation/grafel-binding-evidence-v0.1.schema.json",
    "validation/FDI-v0.4.7.1-ABLATION-PROTOCOL.md",
    "validation/OPTION-B-PAIRED-REPLAY-PROTOCOL-v0.1.md",
    "validation/REALIZATION-TRAVERSAL-GUARD-SPEC-v0.1.md",
    "scripts", "templates/product-intelligence",
)
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
APPROVED_FILE_DIGESTS={
    'governance/approved/layer1/fdi-layer1-markdown-io-profile-v0.1-approved.md': '6c98deeb883f6b468a14f87647e9df25fcfffb5814e66aeddb3dcfc5b3b0bb8c',
    'governance/approved/layer1/fdi-layer1-specification-v0.2-approved.md': '18fd5dac4196d01216454ec713d93fc5dd1f752f5db35a467d5fad0b16035928',
    'governance/approved/layer2/fdi-layer2-product-intelligence-framework-v0.1-approved.md': 'fe1ab08cb3ef288dc5bb1bf8fd72546f00948c0889dcb046e3c00bf5e012e112',
    'governance/approved/layer2/fdi-product-asset-maintenance-skill-contracts-v0.1-approved.md': 'c862a086eacba23ff7828743f78b7fc42c1eeda5d6a8a4e0ec06e08dbf910813',
    'governance/approved/layer2/fdi-product-asset-profile-specification-v0.1-approved.md': '6d87b6d9396fe3556f543fd44f3ffd4b3f6d94aa51147190c45948c75aed03dc',
    'governance/approved/ft-t2/FT-T2-GOVERNING-SURFACE.md': 'e54c4cf7ac5b35985a27b17c0ce85ef64f01698a04556ee50948de2f45861561',
}
def lock(): return json.loads(current_lock_path().read_text())
def current_values(root=ROOT):
    values={}
    for line in (root/'governance/CURRENT').read_text().splitlines():
        if '=' in line:
            key, value=line.split('=', 1)
            values.setdefault(key, []).append(value)
    return values
def current_lock_path(root=ROOT):
    values=current_values(root)['APPROVED_SOURCE_LOCK']
    assert len(values)==1
    return root/values[0]
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
        if old_path in {"skills", "workflows"}:
            yield r'(?<![A-Za-z0-9_./-])' + re.escape(old_path) + r'/'
            continue
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
def test_approved_sources_are_relocated_byte_identically():
    assert {m['local_path'] for m in lock()['modules']} == set(APPROVED_FILE_DIGESTS)
    for path, digest in APPROVED_FILE_DIGESTS.items():
        assert sha(ROOT/path) == digest
def test_current_pointer_resolves_lock_and_ft_t2_tree_digest_matches_baseline():
    lock_path=current_lock_path()
    assert lock_path == ROOT/'governance/locks/approved-source-lock.json'
    data=json.loads(lock_path.read_text())
    ft=next(m for m in data['modules'] if m['id']=='FT-T2')
    digest=hashlib.sha256()
    for relative in ft['tree_paths']:
        path=ROOT/relative
        digest.update(relative.encode())
        digest.update(b'\0')
        digest.update(bytes.fromhex(sha(path)))
    actual=digest.hexdigest()
    assert actual == ft['tree_sha256']
    baseline=(ROOT/'governance/baselines/GB-0001.yaml').read_text()
    baseline_digest=re.search(r'^\s*tree_sha256:\s*([0-9a-f]{64})\s*$', baseline, re.MULTILINE)
    assert baseline_digest
    assert actual == baseline_digest.group(1)


@pytest.mark.parametrize(
    ('current_text', 'message'),
    (
        ('GOVERNING_BASELINE=GB-0001\n', 'missing APPROVED_SOURCE_LOCK'),
        ('APPROVED_SOURCE_LOCK=governance/locks/a.json\nAPPROVED_SOURCE_LOCK=governance/locks/b.json\n', 'duplicate APPROVED_SOURCE_LOCK'),
        ('APPROVED_SOURCE_LOCK=/tmp/outside.json\n', 'unsafe APPROVED_SOURCE_LOCK'),
        ('APPROVED_SOURCE_LOCK=../outside.json\n', 'unsafe APPROVED_SOURCE_LOCK'),
    ),
)
def test_verifier_rejects_invalid_current_lock_pointer(tmp_path, current_text, message):
    governance=tmp_path/'governance'
    governance.mkdir()
    (governance/'CURRENT').write_text(current_text)
    result=subprocess.run(
        ['python3', str(ROOT/'tooling/verification/verify_standalone_bundle.py'), str(tmp_path)],
        text=True, capture_output=True,
    )
    assert result.returncode != 0
    assert message in result.stdout
def test_ft_t2_surface_counts():
    assert len(list((ROOT/'contracts/public/ft-t2').glob('*.md')))==6
    assert len(list((ROOT/'contracts/public/ft-t2').glob('*.schema.json')))==6
    assert len(list((ROOT/'agent/skills/ft-t2').glob('*/SKILL.md')))==5
    assert (ROOT/'agent/workflows/ft-t2/FEATURE-CLOSURE.md').exists()
def test_ft_t2_modern_vocabulary():
    paths=list((ROOT/'contracts/public/ft-t2').glob('*.md'))+list((ROOT/'agent/skills/ft-t2').glob('*/SKILL.md'))+[ROOT/'agent/workflows/ft-t2/FEATURE-CLOSURE.md',ROOT/'governance/approved/ft-t2/FT-T2-GOVERNING-SURFACE.md']
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
    assert not (ROOT/'contracts/public/layer1/README.md').exists()
    assert not (ROOT/'contracts/public/ft-t2/README.md').exists()


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


def assert_numbered_sections_are_consistent(text):
    current_section=None
    top_level=[]
    subsections={}
    for line in text.splitlines():
        top_match=re.match(r"^# (\d+)\. ", line)
        if top_match:
            current_section=int(top_match.group(1))
            top_level.append(current_section)
            continue
        sub_match=re.match(r"^## (\d+)\.(\d+) ", line)
        if sub_match:
            section, subsection=map(int, sub_match.groups())
            assert current_section is not None
            assert section==current_section, f"{line!r} is under section {current_section}"
            subsections.setdefault(section, []).append(subsection)
    assert top_level, "no numbered top-level sections"
    assert top_level==list(range(top_level[0], max(top_level) + 1)), f"top-level sections: {top_level}"
    for section, numbers in subsections.items():
        assert numbers==list(range(1, max(numbers) + 1)), f"section {section} subsections: {numbers}"


def test_framework_spec_numbered_sections_are_consistent():
    path=ROOT/"docs/specifications/framework/FDI-FRAMEWORK-SPECIFICATION-v0.1-rc4.md"
    assert_numbered_sections_are_consistent(path.read_text())


def test_numbered_section_check_rejects_a_missing_subsection():
    markdown="\n".join(f"# {number}. Section" for number in range(1, 45))
    markdown += "\n## 44.1 First\n## 44.2 Second\n## 44.4 Fourth\n"
    with pytest.raises(AssertionError):
        assert_numbered_sections_are_consistent(markdown)


def test_numbered_section_check_accepts_a_future_contiguous_top_level_section():
    markdown="\n".join(f"# {number}. Section" for number in range(1, 46))
    assert_numbered_sections_are_consistent(markdown)


def test_overview_documents_runtime_implementation_and_evidence_readiness():
    overview=(ROOT/"docs/overview/FDI-PROJECT-OVERVIEW.md").read_text()
    assert "# 34. Runtime Implementation" in overview
    assert "Java 17" in overview
    assert "Spring Boot 3.4.1" in overview
    assert "Grafel-named provider integration" in overview
    assert "Graphify rename/migration is planned and separate" in overview
    assert "# 35. Readiness and Evidence Status" in overview


def test_overview_preserves_not_executed_validation_states():
    overview=(ROOT/"docs/overview/FDI-PROJECT-OVERVIEW.md").read_text()
    readiness=overview.split("# 35. Readiness and Evidence Status", 1)[1]
    expected_rows=(
        "| Live Graphify integration | `NOT_EXECUTED` |",
        "| Real Product binding | `NOT_EXECUTED` |",
        "| DEV-204 | `NOT_EXECUTED` |",
        "| F001 | `NOT_EXECUTED` |",
    )
    for row in expected_rows:
        assert row in readiness


def test_validation_tooling_and_template_targets_exist_and_old_paths_are_absent():
    targets=(
        "validation/dev204/scenarios/SCENARIOS-v0.4.7.1.json",
        "validation/dev204/scenarios/VALIDATION-PLAN-v0.4.7.1.json",
        "validation/dev204/scenarios/EXECUTION-GUIDE-v0.4.7.1.md",
        "validation/dev204/schemas/execution-record-v0.2.schema.json",
        "validation/dev204/fixtures",
        "validation/f001/FDI-v0.4.7.1-ABLATION-PROTOCOL.md",
        "validation/deterministic/OPTION-B-PAIRED-REPLAY-PROTOCOL-v0.1.md",
        "validation/deterministic/REALIZATION-TRAVERSAL-GUARD-SPEC-v0.1.md",
        "validation/reports",
        "contracts/providers/graphify/grafel-binding-evidence-v0.1.schema.json",
        "tooling/packaging",
        "tooling/verification",
        "tooling/migration",
        "templates/product-instance/README.md",
    )
    for path in targets:
        assert (ROOT/path).exists(), path
    for path in ("validation/skill-behavior", "validation/grafel-binding-evidence-v0.1.schema.json", "scripts", "templates/product-intelligence"):
        assert not (ROOT/path).exists(), path


def test_dev204_cli_prepares_frozen_corpus_without_execution_claim(tmp_path):
    jar=ROOT/'target/fdi-0.4.8.3.jar'
    assert jar.is_file(), "package the application before running the CLI test"
    result=subprocess.run(
        [
            'java', '-jar', str(jar), 'dev204-prepare',
            '--scenario-pack', str(ROOT/'validation/dev204/scenarios/SCENARIOS-v0.4.7.1.json'),
            '--output-dir', str(tmp_path),
        ],
        text=True, capture_output=True,
    )
    assert result.returncode == 0, result.stderr
    summary=json.loads(result.stdout)
    assert summary['scenario_count'] == 12
    assert summary['packet_count'] == 24
    assert summary['claim_boundary'] == 'PACKETS_PREPARED_NOT_EXECUTED'
    outputs=list(tmp_path.glob('*.json'))
    assert len(outputs) == 36
    assert len(list(tmp_path.glob('*-packet.json'))) == 24
    assert len(list(tmp_path.glob('*-reviewer-rubric.json'))) == 12


@pytest.mark.parametrize(
    ('relative_script', 'argv', 'java_command'),
    (
        (
            'tooling/migration/prepare_dev204_execution.py',
            ['--scenario-pack', 'scenario.json', '--output-dir', 'prepared'],
            ['dev204-prepare', '--scenario-pack', 'scenario.json', '--output-dir', 'prepared'],
        ),
        (
            'tooling/verification/evaluate_dev204_pair.py',
            ['--red', 'red.json', '--green', 'green.json'],
            ['dev204-evaluate', '--red', 'red.json', '--green', 'green.json'],
        ),
    ),
)
def test_maven_wrappers_force_bounded_heap_and_preserve_cli_paths(monkeypatch, relative_script, argv, java_command):
    path=ROOT/relative_script
    spec=importlib.util.spec_from_file_location('wrapper_under_test', path)
    module=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    calls=[]
    monkeypatch.setenv('MAVEN_OPTS', '-Xmx12g')
    monkeypatch.setattr(module.subprocess, 'run', lambda args, **kwargs: calls.append((args, kwargs)))

    module.main(argv)

    assert len(calls) == 2
    maven_args, maven_kwargs=calls[0]
    assert maven_args == [str(ROOT/'mvnw'), '-q', '-DskipTests', 'package']
    assert maven_kwargs['cwd'] == ROOT
    assert maven_kwargs['check'] is True
    assert maven_kwargs['env']['MAVEN_OPTS'] == '-Xmx2g'
    assert maven_kwargs['env'] is not __import__('os').environ
    java_args, java_kwargs=calls[1]
    assert java_args == ['java', '-jar', str(ROOT/'target/fdi-0.4.8.3.jar'), *java_command]
    assert java_kwargs == {'check': True}
