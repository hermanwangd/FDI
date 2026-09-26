import json, hashlib, importlib.util, subprocess, sys, zipfile
from pathlib import Path
from tempfile import TemporaryDirectory
ROOT=Path(__file__).resolve().parents[1]
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def lock(): return json.loads((ROOT/'governance/locks/approved-source-lock.json').read_text())
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
    tree=(ROOT/'release/PROJECT-TREE.txt').read_text()
    for p in ROOT.rglob('*.md'):
        rel=p.relative_to(ROOT)
        if rel.as_posix() != 'CLAUDE.md' and rel.parts[0] != '.claude': assert p.name in tree
def test_overview_and_handoff_exist():
    assert (ROOT/'PROJECT-OVERVIEW.md').exists()
    assert (ROOT/'docs/overview/FDI-PROJECT-OVERVIEW.md').exists()
    assert (ROOT/'agent/handoff/MULTICA-HANDOFF.md').exists()
    assert (ROOT/'agent/handoff/MULTICA-PROJECT-PROMPT.txt').exists()
def test_no_governing_placeholder_readmes():
    assert not (ROOT/'contracts/public/layer1/README.md').exists()
    assert not (ROOT/'contracts/public/ft-t2/README.md').exists()

def test_repository_navigation_entrypoints():
    for relative in ('AGENTS.md', 'docs/README.md', 'docs/FILE-CLASSIFICATION.md'):
        assert (ROOT/relative).exists()
    readme=(ROOT/'README.md').read_text()
    for link in ('AGENTS.md', 'docs/README.md', 'docs/FILE-CLASSIFICATION.md',
                 'docs/overview/FDI-PROJECT-OVERVIEW.md', 'governance/CURRENT',
                 'docs/planning/STATUS.json'):
        assert link in readme

def test_file_classification_covers_active_path_families():
    classification=(ROOT/'docs/FILE-CLASSIFICATION.md').read_text()
    for family in ('governance/', 'governance/approved/', 'contracts/public/', 'agent/skills/',
                   'agent/workflows/', 'tooling/', 'tests/', 'docs/',
                   'validation/', 'engcim/swarm/', 'engcim/bootstrap/',
                   'engcim/targets/', 'release/', 'tmp/'):
        assert f'`{family}' in classification


def test_markdown_inventory_is_exact():
    actual=sorted(p.relative_to(ROOT).as_posix() for p in ROOT.rglob("*.md") if p.relative_to(ROOT).as_posix() != 'CLAUDE.md' and p.relative_to(ROOT).parts[0] != '.claude' and not any(x in {".pytest_cache","__pycache__",".git","target"} for x in p.relative_to(ROOT).parts))
    inv=[x.strip() for x in (ROOT/"release/MARKDOWN-INVENTORY.txt").read_text().splitlines() if x.strip()]
    assert inv==actual

def test_release_metadata_has_no_root_copies():
    for name in ('MANIFEST.json', 'MARKDOWN-INVENTORY.txt', 'PROJECT-TREE.txt', 'VERIFICATION-SUMMARY.json'):
        assert (ROOT/'release'/name).exists()
        assert not (ROOT/name).exists()

def test_package_and_release_indexes_exclude_local_control_and_candidate():
    with TemporaryDirectory() as tmp:
        root=Path(tmp)/'fixture'
        (root/'.claude/state').mkdir(parents=True)
        (root/'.claude/state/environment-state.json').write_text('{"workspace":"local"}\n')
        (root/'.claude/supervisor.md').write_text('active local entrypoint\n')
        (root/'CLAUDE.md').write_text('active local compatibility overlay\n')
        candidate=root/'release/RC10-CANDIDATE-PACKAGE.zip'
        candidate.parent.mkdir(parents=True)
        candidate.write_bytes(b'local archive with active state')
        nested=root/'engcim/bootstrap/supervisor/packages/runtime/.claude/engcim'
        nested.mkdir(parents=True)
        (nested/'supervisor.md').write_text('canonical runtime source\n')
        (root/'README.md').write_text('fixture\n')
        archive=Path(tmp)/'fixture.zip'

        subprocess.run([sys.executable, str(ROOT/'tooling/packaging/build_package.py'), str(root), str(archive)], check=True)
        with zipfile.ZipFile(archive) as bundle:
            names=set(bundle.namelist())
        assert 'fixture/.claude/state/environment-state.json' not in names
        assert 'fixture/CLAUDE.md' not in names
        assert 'fixture/release/RC10-CANDIDATE-PACKAGE.zip' not in names
        assert 'fixture/engcim/bootstrap/supervisor/packages/runtime/.claude/engcim/supervisor.md' in names

        subprocess.run([sys.executable, str(ROOT/'tooling/packaging/build_manifest.py'), str(root)], check=True)
        manifest=json.loads((root/'release/MANIFEST.json').read_text())
        manifest_paths={entry['path'] for entry in manifest['files']}
        assert '.claude/state/environment-state.json' not in manifest_paths
        assert 'CLAUDE.md' not in manifest_paths
        assert 'release/RC10-CANDIDATE-PACKAGE.zip' not in manifest_paths
        assert 'engcim/bootstrap/supervisor/packages/runtime/.claude/engcim/supervisor.md' in manifest_paths

        subprocess.run([sys.executable, str(ROOT/'tooling/packaging/generate_project_tree.py'), str(root)], check=True)
        tree=(root/'release/PROJECT-TREE.txt').read_text().splitlines()
        assert '├── .claude/' not in tree and '└── .claude/' not in tree
        assert not any(line.endswith('CLAUDE.md') for line in tree)
        assert not any('RC10-CANDIDATE-PACKAGE.zip' in line for line in tree)
        assert any(line.endswith('supervisor.md') for line in tree)

        subprocess.run([sys.executable, str(ROOT/'tooling/packaging/generate_markdown_inventory.py'), str(root)], check=True)
        inventory=(root/'release/MARKDOWN-INVENTORY.txt').read_text().splitlines()
        assert '.claude/supervisor.md' not in inventory
        assert 'CLAUDE.md' not in inventory
        assert 'engcim/bootstrap/supervisor/packages/runtime/.claude/engcim/supervisor.md' in inventory

def test_verification_summary_reads_module_surefire_reports():
    script=ROOT/'tooling/verification/write_verification_summary.py'
    spec=importlib.util.spec_from_file_location('verification_summary', script)
    module=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    with TemporaryDirectory() as tmp:
        root=Path(tmp)
        reports=root/'engcim/swarm/target/surefire-reports'
        reports.mkdir(parents=True)
        (reports/'TEST-example.xml').write_text('<testsuite tests="3" failures="1" errors="0" skipped="0"/>')
        assert module.java_test_summary(root)=='2 PASS / 1 FAIL'
