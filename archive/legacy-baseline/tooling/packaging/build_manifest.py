from pathlib import Path
import hashlib,json,sys
from release_metadata import included_files, repository_root
root=repository_root(__file__, sys.argv[1] if len(sys.argv)>1 else None)
out=root/'release/MANIFEST.json'
out.parent.mkdir(parents=True, exist_ok=True)
entries=[]
for p in included_files(root):
    b=p.read_bytes(); entries.append({'path':p.relative_to(root).as_posix(),'sha256':hashlib.sha256(b).hexdigest(),'size':len(b)})
out.write_text(json.dumps({'manifest_version':'0.4.8.3','files':entries},indent=2)+'\n')
print(f'wrote {out}: {len(entries)} files')
