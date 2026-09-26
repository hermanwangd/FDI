from pathlib import Path
import sys
root=Path(sys.argv[1] if len(sys.argv)>1 else '.').resolve()
out=root/'release'/'PROJECT-TREE.txt'
ignore={'.git','__pycache__','.pytest_cache','target'}
def visible(p): return (not (p.parts and p.parts[0] == '.claude')
                        and p.as_posix() != 'CLAUDE.md'
                        and p.as_posix() != 'release/RC10-CANDIDATE-PACKAGE.zip'
                        and not any(part in ignore for part in p.parts)
                        and not ('.mvn' in p.parts and any(part.startswith('apache-maven-') for part in p.parts)))
out.parent.mkdir(parents=True,exist_ok=True)
out.touch(exist_ok=True)
lines=[root.name+'/']
def walk(d,prefix=''):
    items=[p for p in sorted(d.iterdir(), key=lambda p:(p.is_file(),p.name.lower())) if visible(p.relative_to(root))]
    # directories first due key; then files
    for i,p in enumerate(items):
        last=i==len(items)-1
        branch='└── ' if last else '├── '
        lines.append(prefix+branch+p.name+('/' if p.is_dir() else ''))
        if p.is_dir(): walk(p,prefix+('    ' if last else '│   '))
walk(root)
out.write_text('\n'.join(lines)+'\n')
print(f'wrote {out}: {len(lines)-1} entries')
