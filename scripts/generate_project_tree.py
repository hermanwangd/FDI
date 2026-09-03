from pathlib import Path
import sys
root=Path(sys.argv[1] if len(sys.argv)>1 else '.').resolve()
out=root/'PROJECT-TREE.txt'
ignore={'.git','__pycache__','.pytest_cache'}
def visible(p): return not any(part in ignore for part in p.parts)
lines=[root.name+'/']
def walk(d,prefix=''):
    items=[p for p in sorted(d.iterdir(), key=lambda p:(p.is_file(),p.name.lower())) if p.name!='PROJECT-TREE.txt' and visible(p.relative_to(root))]
    # directories first due key; then files
    for i,p in enumerate(items):
        last=i==len(items)-1
        branch='└── ' if last else '├── '
        lines.append(prefix+branch+p.name+('/' if p.is_dir() else ''))
        if p.is_dir(): walk(p,prefix+('    ' if last else '│   '))
walk(root)
# PROJECT-TREE itself explicitly at root so all files are represented
lines.insert(1,'├── PROJECT-TREE.txt')
out.write_text('\n'.join(lines)+'\n')
print(f'wrote {out}: {len(lines)-1} entries')
