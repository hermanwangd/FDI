#!/usr/bin/env python3
"""Build deterministic FDI v0.4.7.3 handoff-hardening source overlay ZIP."""
from __future__ import annotations
import argparse, hashlib, json
from pathlib import Path
import zipfile

FIXED_ZIP_TIME=(2026,9,2,0,0,0)
EXCLUDED_DIRS={".git",".pytest_cache","__pycache__"}
EXCLUDED_SUFFIXES={".pyc",".pyo",".zip"}
EXCLUDED_ROOT={"MULTICA-HANDOFF-v0.4.7.3.md","HANDOFF-PROVENANCE-v0.4.7.3.json","BUNDLE-MANIFEST.json","MANIFEST.json"}
RELEASE="fdi-mvp-v0.4.7.3-handoff-hardening-overlay"

def include_file(path,source,output):
    rel=path.relative_to(source)
    if any(part in EXCLUDED_DIRS for part in rel.parts): return False
    if path.suffix in EXCLUDED_SUFFIXES: return False
    if len(rel.parts)==1 and rel.name in EXCLUDED_ROOT: return False
    try:
        if path.resolve()==output.resolve(): return False
    except FileNotFoundError: pass
    return path.is_file()

def build(source,output):
    source=source.resolve(); output=output.resolve(); output.parent.mkdir(parents=True,exist_ok=True)
    files=[]
    for path in sorted(source.rglob('*'),key=lambda p:p.as_posix()):
        if include_file(path,source,output): files.append((path.relative_to(source).as_posix(),path.read_bytes()))
    manifest={"format":"FDI_IMPLEMENTATION_OVERLAY_MANIFEST_V1","release":RELEASE,"entries":[{"path":r,"size":len(d),"sha256":hashlib.sha256(d).hexdigest()} for r,d in files]}
    mb=(json.dumps(manifest,sort_keys=True,indent=2)+'\n').encode()
    with zipfile.ZipFile(output,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as zf:
        for rel,data in [*files,("MANIFEST.json",mb)]:
            info=zipfile.ZipInfo(rel,FIXED_ZIP_TIME); info.compress_type=zipfile.ZIP_DEFLATED; info.external_attr=0o100644<<16; info.create_system=3
            zf.writestr(info,data,compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)
    return {"release":RELEASE,"output":str(output),"entries":len(files)+1,"manifest_entries":len(files),"sha256":hashlib.sha256(output.read_bytes()).hexdigest()}

def main():
    p=argparse.ArgumentParser(); p.add_argument('--source',required=True); p.add_argument('--output',required=True); a=p.parse_args(); print(json.dumps(build(Path(a.source),Path(a.output)),sort_keys=True))
if __name__=='__main__': main()
