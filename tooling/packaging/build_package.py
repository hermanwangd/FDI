from pathlib import Path
import zipfile,sys
root=Path(sys.argv[1]).resolve(); dest=Path(sys.argv[2]).resolve()
ignore={'.git','__pycache__','.pytest_cache'}
files=[]
for p in sorted(root.rglob('*')):
    if p.is_file() and not any(x in ignore for x in p.relative_to(root).parts): files.append(p)
with zipfile.ZipFile(dest,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as z:
    for p in files:
        rel=(Path(root.name)/p.relative_to(root)).as_posix()
        info=zipfile.ZipInfo(rel,date_time=(2026,9,3,0,0,0)); info.compress_type=zipfile.ZIP_DEFLATED; info.external_attr=0o100644<<16
        z.writestr(info,p.read_bytes())
print(f'wrote {dest}: {len(files)} entries')
