import json, hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def lock(): return json.loads((ROOT/'governance/approved-source-lock.json').read_text())
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
    assert (ROOT/'MULTICA-HANDOFF.md').exists()
    assert (ROOT/'MULTICA-PROJECT-PROMPT.txt').exists()
def test_no_governing_placeholder_readmes():
    assert not (ROOT/'contracts/layer1/README.md').exists()
    assert not (ROOT/'contracts/ft-t2/README.md').exists()


def test_markdown_inventory_is_exact():
    actual=sorted(p.relative_to(ROOT).as_posix() for p in ROOT.rglob("*.md") if not any(x in {".pytest_cache","__pycache__",".git"} for x in p.relative_to(ROOT).parts))
    inv=[x.strip() for x in (ROOT/"MARKDOWN-INVENTORY.txt").read_text().splitlines() if x.strip()]
    assert inv==actual
