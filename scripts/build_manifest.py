from pathlib import Path
import hashlib,json,sys
root=Path(sys.argv[1] if len(sys.argv)>1 else '.').resolve()
out=root/'MANIFEST.json'
ignore={'.git','__pycache__','.pytest_cache'}
def included(p):
    rel=p.relative_to(root)
    return p.is_file() and p.name!='MANIFEST.json' and not any(x in ignore for x in rel.parts)
entries=[]
for p in sorted(root.rglob('*')):
    if included(p):
        b=p.read_bytes(); entries.append({'path':p.relative_to(root).as_posix(),'sha256':hashlib.sha256(b).hexdigest(),'size':len(b)})
out.write_text(json.dumps({'manifest_version':'0.4.8.3','files':entries},indent=2)+'\n')
print(f'wrote {out}: {len(entries)} files')
