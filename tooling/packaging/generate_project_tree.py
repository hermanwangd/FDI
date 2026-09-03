from pathlib import Path
import sys
from release_metadata import is_excluded, repository_root
root=repository_root(__file__, sys.argv[1] if len(sys.argv)>1 else None)
out=root/'release/PROJECT-TREE.txt'
out.parent.mkdir(parents=True, exist_ok=True)
def visible(p): return not is_excluded(p)
lines=[root.name+'/']
def walk(d,prefix=''):
    items=[p for p in sorted(d.iterdir(), key=lambda p:(p.is_file(),p.name.lower())) if p != out and visible(p.relative_to(root))]
    # directories first due key; then files
    for i,p in enumerate(items):
        last=i==len(items)-1
        branch='└── ' if last else '├── '
        lines.append(prefix+branch+p.name+('/' if p.is_dir() else ''))
        if p.is_dir(): walk(p,prefix+('    ' if last else '│   '))
walk(root)
# The output is represented at its actual release path without reading itself.
if out.parent == root/'release':
    release_line=next((index for index,line in enumerate(lines) if line.endswith('release/')), None)
    if release_line is not None:
        lines.insert(release_line + 1, '│   ├── PROJECT-TREE.txt')
out.write_text('\n'.join(lines)+'\n')
print(f'wrote {out}: {len(lines)-1} entries')
