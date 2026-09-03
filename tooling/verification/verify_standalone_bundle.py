from pathlib import Path
import hashlib,json,re,sys,subprocess
sys.path.insert(0, str(Path(__file__).resolve().parents[1]/'packaging'))
from release_metadata import included_files
root=Path(sys.argv[1] if len(sys.argv)>1 else '.').resolve()
release=root/'release'
errors=[]; checks=[]
def ok(name, cond, detail=''):
    checks.append((name,cond,detail));
    if not cond: errors.append(f'{name}: {detail}')
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def parse_project_tree_paths(text):
    lines=text.splitlines()
    if not lines or not lines[0].endswith('/'):
        raise ValueError('missing project-tree root')
    paths=set(); directories=[]
    for line in lines[1:]:
        match=re.fullmatch(r'((?:│   |    )*)(?:├── |└── )(.+)',line)
        if not match:
            raise ValueError(f'invalid project-tree line: {line!r}')
        depth=len(match.group(1))//4
        if depth>len(directories):
            raise ValueError(f'invalid project-tree depth: {line!r}')
        directories=directories[:depth]
        name=match.group(2)
        relative='/'.join([*directories,name.rstrip('/')])
        if relative in paths:
            raise ValueError(f'duplicate project-tree path: {relative}')
        paths.add(relative)
        if name.endswith('/'):
            directories.append(name[:-1])
    return paths
def resolve_approved_lock_path(project_root):
    current=project_root/'governance/CURRENT'
    if not current.is_file():
        raise ValueError('missing governance/CURRENT')
    values=[]
    for line in current.read_text().splitlines():
        if line.startswith('APPROVED_SOURCE_LOCK='):
            values.append(line.split('=',1)[1])
    if not values:
        raise ValueError('missing APPROVED_SOURCE_LOCK in governance/CURRENT')
    if len(values)>1:
        raise ValueError('duplicate APPROVED_SOURCE_LOCK in governance/CURRENT')
    relative=Path(values[0])
    if not values[0] or relative.is_absolute() or '..' in relative.parts:
        raise ValueError('unsafe APPROVED_SOURCE_LOCK in governance/CURRENT')
    return project_root/relative
# 1. authority lock
try:
    lock_path=resolve_approved_lock_path(root)
except ValueError as exc:
    ok('approved-source-lock pointer',False,str(exc))
    for n,c,d in checks: print(('PASS' if c else 'FAIL'),n,(f'— {d}' if d and not c else ''))
    print('RESULT: 0 PASS / 1 FAIL')
    sys.exit(1)
ok('approved-source-lock pointer',True,lock_path.relative_to(root).as_posix())
ok('approved-source-lock exists',lock_path.exists(),str(lock_path))
lock=json.loads(lock_path.read_text()) if lock_path.exists() else {'modules':[]}
ids={m.get('id') for m in lock.get('modules',[])}
required={'L1-SEM','L1-IO','L2-FWK','L2-PROFILE','L2-MAINT','FT-T2'}
ok('all governing IDs present',ids==required,f'got {sorted(ids)}')
for m in lock.get('modules',[]):
    lp=root/m['local_path']; ok(f"{m['id']} local_path exists",lp.exists(),m['local_path'])
    if lp.exists():
        text=lp.read_text(errors='replace')
        ok(f"{m['id']} non-placeholder content",len(text)>1500 and 'TO_RESOLVE_FROM_EXACT' not in text,f'len={len(text)}')
        if 'sha256' in m: ok(f"{m['id']} sha256",sha(lp)==m['sha256'],m['local_path'])
    if m['id']=='FT-T2':
        h=hashlib.sha256(); paths=m.get('tree_paths',[])
        for rel in paths:
            p=root/rel; ok('FT-T2 tree path '+rel,p.exists(),rel)
            if p.exists(): h.update(rel.encode()); h.update(b'\0'); h.update(bytes.fromhex(sha(p)))
        ok('FT-T2 tree digest',h.hexdigest()==m.get('tree_sha256'),h.hexdigest())
# 2. exact FT-T2 physical surface
ft_contract_md=list((root/'contracts/public/ft-t2').glob('*.md'))
ft_schemas=list((root/'contracts/public/ft-t2').glob('*.schema.json'))
ft_skills=list((root/'agent/skills/ft-t2').glob('*/SKILL.md'))
ok('FT-T2 six contract md',len(ft_contract_md)==6,str([p.name for p in ft_contract_md]))
ok('FT-T2 six schemas',len(ft_schemas)==6,str([p.name for p in ft_schemas]))
ok('FT-T2 five skills',len(ft_skills)==5,str([p.parent.name for p in ft_skills]))
ok('FT-T2 workflow exists',(root/'agent/workflows/ft-t2/FEATURE-CLOSURE.md').exists())
active_ft='\n'.join(p.read_text(errors='replace') for p in ft_contract_md+ft_skills+[root/'agent/workflows/ft-t2/FEATURE-CLOSURE.md',root/'governance/approved/ft-t2/FT-T2-GOVERNING-SURFACE.md'])
for banned in ['PROVISIONALLY_COMPLETE','ACCEPT_PROVISIONALLY_COMPLETE','closure_status: OPEN|PARTIAL|CLOSED\n']:
    ok('FT-T2 bans '+banned,banned not in active_ft,banned)
for required_token in ['CLOSED_WITHIN_DECLARED_SCOPE','SPEC_READY | BLOCKED','ACCEPT_CLOSED_WITHIN_DECLARED_SCOPE']:
    ok('FT-T2 contains '+required_token,required_token in active_ft,required_token)
# 3. all markdowns listed in project tree
pt=release/'PROJECT-TREE.txt'; ok('PROJECT-TREE exists',pt.exists())
tree=pt.read_text() if pt.exists() else ''
md=sorted(p.relative_to(root).as_posix() for p in included_files(root) if p.suffix.lower()=='.md')
try:
    tree_paths=parse_project_tree_paths(tree)
except ValueError as exc:
    tree_paths=set()
    ok('PROJECT-TREE parses',False,str(exc))
else:
    ok('PROJECT-TREE parses',True)
missing=[rel for rel in md if rel not in tree_paths]
ok('all Markdown files appear in PROJECT-TREE',not missing,f'missing={missing[:20]}')
mdi=release/'MARKDOWN-INVENTORY.txt'
ok('MARKDOWN-INVENTORY exists',mdi.exists())
if mdi.exists():
    inv=[x.strip() for x in mdi.read_text().splitlines() if x.strip()]
    ok('MARKDOWN-INVENTORY exact path set',inv==md,f'actual={len(md)} inventory={len(inv)}')
summary_path=release/'VERIFICATION-SUMMARY.json'
ok('VERIFICATION-SUMMARY exists',summary_path.exists())
if summary_path.exists():
    summary=json.loads(summary_path.read_text())
    execution=summary.get('verification_execution',{})
    required_execution={'manifest_integrity','python_compile','standalone_verifier','unit_tests'}
    ok('VERIFICATION-SUMMARY records no unevidenced PASS',
       set(execution)==required_execution
       and set(execution.values()) <= {'NOT_RUN','NOT_RECORDED'}
       and 'PASS' not in json.dumps(summary),
       str(execution))
# Stronger count marker: every markdown basename line count can collide, so verifier also writes exact inventory elsewhere through manifest.
# 4. JSON/schema parse
for p in included_files(root):
    if p.suffix.lower()!='.json': continue
    try: json.loads(p.read_text())
    except Exception as e: errors.append(f'JSON parse {p.relative_to(root)}: {e}')
ok('all JSON parse',not any(e.startswith('JSON parse') for e in errors))
# 5. Java runtime identity and Python packaging-tool compile
pom=(root/'pom.xml').read_text() if (root/'pom.xml').exists() else ''
ok('Java 17 compiler release','<maven.compiler.release>17</maven.compiler.release>' in pom)
ok('Spring Boot 3.4.1','<version>3.4.1</version>' in pom)
ok('Python runtime removed',not (root/'src/fdi').exists())
py=[str(p) for p in root.rglob('*.py') if '__pycache__' not in p.parts]
r=subprocess.run([sys.executable,'-m','py_compile',*py],capture_output=True,text=True)
ok('Python packaging tools compile',r.returncode==0,r.stderr[-1000:])
# 6. manifest integrity
man=release/'MANIFEST.json'; ok('MANIFEST exists',man.exists())
if man.exists():
    m=json.loads(man.read_text()); listed={e['path']:e for e in m['files']}
    actual=[p.relative_to(root).as_posix() for p in included_files(root)]
    ok('manifest path set exact',set(listed)==set(actual),f'missing={set(actual)-set(listed)}, extra={set(listed)-set(actual)}')
    bad=[]
    for rel,e in listed.items():
        p=root/rel
        if not p.exists() or sha(p)!=e['sha256'] or p.stat().st_size!=e['size']: bad.append(rel)
    ok('manifest digests',not bad,f'bad={bad[:20]}')
# output
for n,c,d in checks: print(('PASS' if c else 'FAIL'),n,(f'— {d}' if d and not c else ''))
print(f'RESULT: {len(checks)-sum(not c for _,c,_ in checks)} PASS / {sum(not c for _,c,_ in checks)} FAIL')
sys.exit(1 if errors else 0)
