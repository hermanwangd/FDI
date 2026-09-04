import sys
from release_metadata import included_files, repository_root
root=repository_root(__file__, sys.argv[1] if len(sys.argv)>1 else None)
out=root/'release/PROJECT-TREE.txt'
out.parent.mkdir(parents=True, exist_ok=True)
release_outputs={'MANIFEST.json','MARKDOWN-INVENTORY.txt','PROJECT-TREE.txt','VERIFICATION-SUMMARY.json'}
virtual_outputs={f'release/{name}' for name in release_outputs}
paths={p.relative_to(root).as_posix() for p in included_files(root)}-virtual_outputs
paths.update(virtual_outputs)
tree={}
for relative in sorted(paths):
    node=tree
    parts=relative.split('/')
    for part in parts[:-1]:
        node=node.setdefault(part,{})
    node[parts[-1]]=None
lines=[root.name+'/']
def walk(node,prefix=''):
    items=sorted(node.items(),key=lambda item:(item[1] is None,item[0].lower()))
    # directories first due key; then files
    for i,(name,child) in enumerate(items):
        is_dir=child is not None
        last=i==len(items)-1
        branch='└── ' if last else '├── '
        lines.append(prefix+branch+name+('/' if is_dir else ''))
        if is_dir: walk(child,prefix+('    ' if last else '│   '))
walk(tree)
out.write_text('\n'.join(lines)+'\n')
print(f'wrote {out}: {len(lines)-1} entries')
