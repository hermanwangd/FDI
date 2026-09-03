from pathlib import Path
import sys
from release_metadata import is_excluded, repository_root
root=repository_root(__file__, sys.argv[1] if len(sys.argv)>1 else None)
out=root/'release/PROJECT-TREE.txt'
out.parent.mkdir(parents=True, exist_ok=True)
release_outputs={'MANIFEST.json','MARKDOWN-INVENTORY.txt','PROJECT-TREE.txt','VERIFICATION-SUMMARY.json'}
def visible(p):
    if p.is_symlink() or is_excluded(p.relative_to(root)):
        return False
    try:
        p.resolve(strict=True).relative_to(root.resolve())
    except (FileNotFoundError, ValueError):
        return False
    return True
lines=[root.name+'/']
def walk(d,prefix=''):
    actual=[p for p in d.iterdir() if visible(p) and not (d == out.parent and p.name in release_outputs)]
    items=[(p.name,p,p.is_dir()) for p in actual]
    if d == out.parent:
        items.extend((name,None,False) for name in release_outputs)
    items.sort(key=lambda item:(not item[2],item[0].lower()))
    # directories first due key; then files
    for i,(name,p,is_dir) in enumerate(items):
        last=i==len(items)-1
        branch='└── ' if last else '├── '
        lines.append(prefix+branch+name+('/' if is_dir else ''))
        if is_dir: walk(p,prefix+('    ' if last else '│   '))
walk(root)
out.write_text('\n'.join(lines)+'\n')
print(f'wrote {out}: {len(lines)-1} entries')
