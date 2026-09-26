#!/usr/bin/env python3
"""Graphify-level repository analyzer (stdlib-only core).

Builds symbol-level Observations from a repository checkout. Compared with the
previous shallow heuristic analyzer this implementation adds:

- stable source Module and CodeEntity identities
- Python AST parsing with class/function/method symbols and local call graph
- source-level interface definitions / implementations
- HTTP endpoint definitions and HTTP client calls
- JS/TS, Go and Java symbol/call/import extraction (parser-lite fallback)
- line-level provenance on every source-derived observation
- stable observation IDs derived from semantic content

The output remains Observation JSONL so the downstream correlation/synthesis
pipeline stays provider-neutral. Raw observations are always PROVISIONAL.
"""
from __future__ import annotations
import argparse, ast, hashlib, json, os, re, subprocess, sys
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path

SKIP_DIRS={'.git','node_modules','vendor','dist','build','__pycache__','.venv','target','.idea','.vscode','coverage'}
SOURCE_EXTS={'.py','.js','.jsx','.ts','.tsx','.go','.java'}
MAX_FILE_BYTES=3*1024*1024
HTTP_URL_RE=re.compile(r'https?://([A-Za-z0-9][-A-Za-z0-9.]*)(?::\d+)?([^\s"\'<>)]*)?')
SQL_TABLE_RE=re.compile(r'\b(?:from|join|into|update)\s+([A-Za-z_][\w.$-]*)',re.I)


def now_iso(): return datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
def slug(s): return re.sub(r'[^a-z0-9]+','-',(s or '').lower()).strip('-') or 'entity'
def norm_path(p): return p.replace('\\','/')

def detect_commit(repo):
    try:
        r=subprocess.run(['git','-C',repo,'rev-parse','HEAD'],capture_output=True,text=True,timeout=15)
        return r.stdout.strip() if r.returncode==0 else None
    except Exception: return None

def read_text(path):
    try:
        if os.path.getsize(path)>MAX_FILE_BYTES: return None
        return Path(path).read_text(encoding='utf-8',errors='replace')
    except OSError: return None

def line_of(text,offset): return text.count('\n',0,max(0,offset))+1

def module_id(repo,rel): return f"Module:{repo}/{norm_path(rel)}"
def entity_id(repo,rel,qual): return f"CodeEntity:{repo}/{norm_path(rel)}#{qual}"
def interface_id(repo,key): return f"Interface:{repo}/{key}"

def stable_obs_id(repo,otype,file,line,obj):
    raw=json.dumps([repo,otype,file,line,obj],ensure_ascii=False,sort_keys=True,separators=(',',':'))
    return 'obs:'+hashlib.sha1(raw.encode()).hexdigest()[:20]

class Emitter:
    def __init__(self,repo,commit,out):
        self.repo,self.commit,self.out=repo,commit,out
        self.ts=now_iso(); self.seen=set(); self.count=0
    def emit(self,file,otype,obj,line=None,end_line=None):
        file=norm_path(file)
        oid=stable_obs_id(self.repo,otype,file,line,obj)
        if oid in self.seen: return
        self.seen.add(oid); self.count+=1
        rec={'id':oid,'repo':self.repo,'commit':self.commit,'file':file,
             'line':line,'endLine':end_line,'observationType':otype,'observation':obj,
             'confidence':'auto-observation','validationState':'PROVISIONAL','observedAt':self.ts}
        self.out.write(json.dumps(rec,ensure_ascii=False)+'\n')

# ---------- structured engineering files ----------
def looks_openapi(t):
    h=t[:5000]; return any(x in h for x in ('openapi:','"openapi"','swagger:','"swagger"'))
def parse_openapi(t):
    title=None
    m=re.search(r'^\s*title:\s*(.+)$',t,re.M) or re.search(r'"title"\s*:\s*"([^"]+)"',t)
    if m:title=m.group(1).strip().strip('"\'')
    paths=[]
    for m in re.finditer(r'^\s{2}(/[\w./{}-]+):\s*$',t,re.M): paths.append(m.group(1))
    for m in re.finditer(r'"(/[\w./{}-]+)"\s*:',t): paths.append(m.group(1))
    servers=re.findall(r'^\s*-?\s*url:\s*(\S+)\s*$',t,re.M) or re.findall(r'"url"\s*:\s*"([^"]+)"',t)
    return {'title':title,'paths':sorted(set(paths))[:500],'servers':servers[:50]}
def parse_package_json(t):
    try:d=json.loads(t)
    except Exception:return None
    deps={}
    for sec in ('dependencies','devDependencies','peerDependencies'):
        deps.update(d.get(sec) or {})
    return {'packageName':d.get('name'),'dependencies':deps}
def parse_requirements(t):
    out={}
    for line in t.splitlines():
        line=line.split('#',1)[0].strip()
        m=re.match(r'([A-Za-z0-9_.-]+)\s*(?:==|>=|<=|~=|!=)?\s*([^;\s]*)',line)
        if m and not line.startswith('-'): out[m.group(1)]=m.group(2) or None
    return out
def parse_go_mod(t):
    m=re.search(r'^module\s+(\S+)',t,re.M); module=m.group(1) if m else None
    deps={}
    for m in re.finditer(r'^\s*([\w./-]+\.[A-Za-z]{2,}[\w./-]*)\s+(v\S+)',t,re.M): deps[m.group(1)]=m.group(2)
    return {'module':module,'dependencies':deps}
def parse_pom(t):
    out={}
    for m in re.finditer(r'<dependency>.*?<groupId>([^<]+)</groupId>.*?<artifactId>([^<]+)</artifactId>(?:.*?<version>([^<]+)</version>)?.*?</dependency>',t,re.S):
        out[f'{m.group(1)}:{m.group(2)}']=m.group(3)
    return out

# ---------- Python AST ----------
class PyAnalyzer(ast.NodeVisitor):
    def __init__(self,repo,rel,text,emit):
        self.repo,self.rel,self.text,self.emit=repo,rel,text,emit
        self.scope=[]; self.class_stack=[]; self.imports={}; self.entities={}; self.calls=[]; self.variable_types={}
    def qual(self,name): return '.'.join(self.scope+[name]) if self.scope else name
    def preindex(self,tree):
        def walk(body,prefix=[]):
            for n in body:
                if isinstance(n,(ast.FunctionDef,ast.AsyncFunctionDef,ast.ClassDef)):
                    q='.'.join(prefix+[n.name]); self.entities[q]=entity_id(self.repo,self.rel,q)
                    if isinstance(n,ast.ClassDef): walk(n.body,prefix+[n.name])
        walk(tree.body)
    def visit_Import(self,node):
        for a in node.names:
            alias=a.asname or a.name.split('.')[0]; self.imports[alias]=a.name
            self.emit(self.rel,'import',{'module':a.name,'alias':alias,'moduleId':module_id(self.repo,self.rel)},node.lineno)
    def visit_ImportFrom(self,node):
        mod=node.module or ''
        for a in node.names:
            alias=a.asname or a.name; self.imports[alias]=f'{mod}.{a.name}'.strip('.')
            self.emit(self.rel,'import',{'module':mod,'symbol':a.name,'alias':alias,'moduleId':module_id(self.repo,self.rel)},node.lineno)
    def visit_ClassDef(self,node):
        q=self.qual(node.name); eid=entity_id(self.repo,self.rel,q)
        bases=[self.expr_name(b) for b in node.bases if self.expr_name(b)]
        kind='interface' if any(x.split('.')[-1] in {'ABC','Protocol','Interface'} or x.split('.')[-1].endswith(('Interface','Protocol')) for x in bases) or node.name.endswith(('Interface','Protocol')) else 'class'
        self.emit(self.rel,'code-entity',{'entityId':eid,'moduleId':module_id(self.repo,self.rel),'entityKind':kind,'name':node.name,'qualname':q,'language':'python','signature':f'class {node.name}({", ".join(bases)})','visibility':'private' if node.name.startswith('_') else 'public'},node.lineno,getattr(node,'end_lineno',None))
        if kind=='interface': self.emit(self.rel,'interface-definition',{'interfaceId':interface_id(self.repo,f'{norm_path(self.rel)}#{q}'),'name':node.name,'moduleId':module_id(self.repo,self.rel),'codeEntityId':eid,'language':'python'},node.lineno)
        for b in bases:
            if b and b.split('.')[-1] not in {'object','ABC'}:
                self.emit(self.rel,'implements-interface',{'implementerId':eid,'interfaceName':b.split('.')[-1],'interfaceRef':b,'language':'python'},node.lineno)
        old=self.scope[:]; self.scope.append(node.name); self.class_stack.append(node.name); self.generic_visit(node); self.class_stack.pop(); self.scope=old
    def visit_FunctionDef(self,node): self._function(node)
    def visit_AsyncFunctionDef(self,node): self._function(node)
    def _function(self,node):
        q=self.qual(node.name); eid=entity_id(self.repo,self.rel,q)
        args=[a.arg for a in node.args.args]; kind='method' if self.class_stack else 'function'
        self.emit(self.rel,'code-entity',{'entityId':eid,'moduleId':module_id(self.repo,self.rel),'entityKind':kind,'name':node.name,'qualname':q,'language':'python','signature':f'{node.name}({", ".join(args)})','visibility':'private' if node.name.startswith('_') else 'public'},node.lineno,getattr(node,'end_lineno',None))
        for dec in node.decorator_list:
            dname=self.expr_name(dec.func if isinstance(dec,ast.Call) else dec)
            if dname and dname.split('.')[-1].lower() in {'get','post','put','delete','patch','route'}:
                path=None; method=dname.split('.')[-1].upper()
                if isinstance(dec,ast.Call) and dec.args and isinstance(dec.args[0],ast.Constant): path=str(dec.args[0].value)
                if path:
                    iid=interface_id(self.repo,f'http:{method}:{path}')
                    self.emit(self.rel,'endpoint-definition',{'interfaceId':iid,'method':method,'path':path,'handlerId':eid,'framework':'python-decorator'},node.lineno)
        old=self.scope[:]; self.scope.append(node.name); self.generic_visit(node); self.scope=old
    def visit_Assign(self,node):
        # Simple local type inference: service = ChartService()
        if isinstance(node.value,ast.Call):
            ctor=self.expr_name(node.value.func)
            if ctor and ctor in self.entities:
                for target in node.targets:
                    if isinstance(target,ast.Name): self.variable_types[target.id]=ctor
        self.generic_visit(node)
    def visit_Call(self,node):
        if not self.scope:
            self.generic_visit(node); return
        caller=entity_id(self.repo,self.rel,'.'.join(self.scope)); name=self.expr_name(node.func)
        if name:
            target=None; external_module=None; symbol=name.split('.')[-1]
            if name in self.entities: target=self.entities[name]
            elif name.split('.')[-1] in self.entities: target=self.entities[name.split('.')[-1]]
            elif name.startswith('self.') and self.class_stack:
                q='.'.join(self.scope[:-1]+[name.split('.')[-1]])
                target=self.entities.get(q)
            elif '.' in name and name.split('.')[0] in self.variable_types:
                q=f"{self.variable_types[name.split('.')[0]]}.{name.split('.')[-1]}"
                target=self.entities.get(q)
            root=name.split('.')[0]
            if root in self.imports: external_module=self.imports[root]
            self.emit(self.rel,'code-call',{'callerId':caller,'callee':name,'calleeSymbol':symbol,'resolvedTargetId':target,'externalModule':external_module},node.lineno)
            # requests/httpx client calls
            low=name.lower()
            if any(x in low for x in ('requests.get','requests.post','requests.put','requests.delete','httpx.get','httpx.post','httpx.put','httpx.delete')) and node.args and isinstance(node.args[0],ast.Constant):
                url=str(node.args[0].value); self.emit(self.rel,'http-request',{'callerId':caller,'method':name.split('.')[-1].upper(),'url':url,'path':_url_path(url)},node.lineno)
        self.generic_visit(node)
    def expr_name(self,n):
        if isinstance(n,ast.Name): return n.id
        if isinstance(n,ast.Attribute):
            b=self.expr_name(n.value); return f'{b}.{n.attr}' if b else n.attr
        return None

def _url_path(url):
    m=re.match(r'https?://[^/]+(.*)',url); return (m.group(1) or '/') if m else url

def extract_sql_tables(text):
    tables=set()
    # Only inspect quoted strings that look like SQL statements; avoids Python/Java import 'from' false positives.
    for m in re.finditer(r'([\"\']{1,3})(.*?)(?:\1)', text, re.S):
        value=m.group(2)
        if not re.search(r'\b(select|insert|update|delete|merge)\b',value,re.I):
            continue
        tables.update(SQL_TABLE_RE.findall(value))
    return sorted(tables)

def analyze_python(repo,rel,text,emit):
    try: tree=ast.parse(text,filename=rel)
    except SyntaxError as e:
        emit(rel,'parse-warning',{'language':'python','message':str(e)},getattr(e,'lineno',None)); return
    a=PyAnalyzer(repo,rel,text,emit); a.preindex(tree); a.visit(tree)

# ---------- parser-lite languages ----------
def emit_entity(emit,repo,rel,lang,kind,name,qual,line,signature=''):
    eid=entity_id(repo,rel,qual)
    emit(rel,'code-entity',{'entityId':eid,'moduleId':module_id(repo,rel),'entityKind':kind,'name':name,'qualname':qual,'language':lang,'signature':signature,'visibility':'public' if not name.startswith('_') else 'private'},line)
    return eid

def analyze_js(repo,rel,text,emit):
    imports={}
    for m in re.finditer(r'import\s+(.*?)\s+from\s+["\']([^"\']+)["\']|require\(["\']([^"\']+)["\']\)',text):
        src=m.group(2) or m.group(3); spec=(m.group(1) or '').strip(); ln=line_of(text,m.start())
        emit(rel,'import',{'module':src,'specifier':spec,'moduleId':module_id(repo,rel)},ln)
        if spec:
            for n in re.findall(r'[A-Za-z_$][\w$]*',spec): imports[n]=src
    entities={}
    for m in re.finditer(r'\b(?:export\s+)?(?:async\s+)?function\s+([A-Za-z_$][\w$]*)\s*\(([^)]*)\)',text):
        n=m.group(1); entities[n]=emit_entity(emit,repo,rel,'javascript','function',n,n,line_of(text,m.start()),f'{n}({m.group(2)})')
    for m in re.finditer(r'\bclass\s+([A-Za-z_$][\w$]*)(?:\s+extends\s+([A-Za-z_$][\w$.]*))?',text):
        n=m.group(1); eid=emit_entity(emit,repo,rel,'javascript','class',n,n,line_of(text,m.start()),m.group(0)); entities[n]=eid
        if m.group(2): emit(rel,'implements-interface',{'implementerId':eid,'interfaceName':m.group(2).split('.')[-1],'interfaceRef':m.group(2),'language':'javascript'},line_of(text,m.start()))
    for m in re.finditer(r'\b(?:const|let|var)\s+([A-Za-z_$][\w$]*)\s*=\s*(?:async\s*)?\([^)]*\)\s*=>',text):
        n=m.group(1); entities[n]=emit_entity(emit,repo,rel,'javascript','function',n,n,line_of(text,m.start()),m.group(0).split('=>')[0].strip())
    # Express/Nest-ish routes
    for m in re.finditer(r'\b(?:app|router)\.(get|post|put|delete|patch)\s*\(\s*["\']([^"\']+)["\']\s*,\s*([A-Za-z_$][\w$]*)?',text,re.I):
        method,path,handler=m.group(1).upper(),m.group(2),m.group(3); hid=entities.get(handler) if handler else None
        emit(rel,'endpoint-definition',{'interfaceId':interface_id(repo,f'http:{method}:{path}'),'method':method,'path':path,'handlerId':hid,'framework':'express-like'},line_of(text,m.start()))
    for m in re.finditer(r'\b(?:fetch|axios\.(get|post|put|delete|patch))\s*\(\s*["\']([^"\']+)["\']',text,re.I):
        emit(rel,'http-request',{'callerId':None,'method':(m.group(1) or 'GET').upper(),'url':m.group(2),'path':_url_path(m.group(2))},line_of(text,m.start()))
    # call graph / imported calls
    for m in re.finditer(r'\b([A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)?)\s*\(',text):
        name=m.group(1)
        if name in {'if','for','while','switch','catch','function'}: continue
        root=name.split('.')[0]; target=entities.get(name) or entities.get(name.split('.')[-1])
        emit(rel,'code-call',{'callerId':None,'callee':name,'calleeSymbol':name.split('.')[-1],'resolvedTargetId':target,'externalModule':imports.get(root)},line_of(text,m.start()))

def analyze_go(repo,rel,text,emit):
    imports={}
    for block in re.finditer(r'import\s*(?:\((.*?)\)|"([^"]+)")',text,re.S):
        body=block.group(1) or f'"{block.group(2)}"'
        for m in re.finditer(r'(?:(\w+)\s+)?"([^"]+)"',body):
            alias=m.group(1) or m.group(2).split('/')[-1]; imports[alias]=m.group(2)
            emit(rel,'import',{'module':m.group(2),'alias':alias,'moduleId':module_id(repo,rel)},line_of(text,block.start()+m.start()))
    entities={}
    for m in re.finditer(r'\btype\s+(\w+)\s+interface\s*\{',text):
        n=m.group(1); eid=emit_entity(emit,repo,rel,'go','interface',n,n,line_of(text,m.start()),m.group(0)); entities[n]=eid
        emit(rel,'interface-definition',{'interfaceId':interface_id(repo,f'{norm_path(rel)}#{n}'),'name':n,'moduleId':module_id(repo,rel),'codeEntityId':eid,'language':'go'},line_of(text,m.start()))
    for m in re.finditer(r'\bfunc\s*(?:\(\s*\w+\s+\*?(\w+)\s*\)\s*)?(\w+)\s*\(([^)]*)\)',text):
        recv,name=m.group(1),m.group(2); q=f'{recv}.{name}' if recv else name
        entities[q]=emit_entity(emit,repo,rel,'go','method' if recv else 'function',name,q,line_of(text,m.start()),m.group(0))
    for m in re.finditer(r'\b([A-Za-z_]\w*(?:\.[A-Za-z_]\w*)?)\s*\(',text):
        name=m.group(1); root=name.split('.')[0]
        emit(rel,'code-call',{'callerId':None,'callee':name,'calleeSymbol':name.split('.')[-1],'resolvedTargetId':entities.get(name),'externalModule':imports.get(root)},line_of(text,m.start()))
    for m in re.finditer(r'http\.(?:HandleFunc|Handle)\s*\(\s*"([^"]+)"',text):
        emit(rel,'endpoint-definition',{'interfaceId':interface_id(repo,f'http:ANY:{m.group(1)}'),'method':'ANY','path':m.group(1),'handlerId':None,'framework':'go-net-http'},line_of(text,m.start()))

def analyze_java(repo,rel,text,emit):
    imports={}
    for m in re.finditer(r'^\s*import\s+([\w.]+);',text,re.M):
        full=m.group(1); imports[full.split('.')[-1]]=full; emit(rel,'import',{'module':'.'.join(full.split('.')[:-1]),'symbol':full.split('.')[-1],'moduleId':module_id(repo,rel)},line_of(text,m.start()))
    entities={}; interfaces=set()
    for m in re.finditer(r'\b(interface|class)\s+(\w+)(?:\s+extends\s+([\w., <>]+))?(?:\s+implements\s+([\w., <>]+))?',text):
        kind,name=m.group(1),m.group(2); eid=emit_entity(emit,repo,rel,'java','interface' if kind=='interface' else 'class',name,name,line_of(text,m.start()),m.group(0)); entities[name]=eid
        if kind=='interface': interfaces.add(name); emit(rel,'interface-definition',{'interfaceId':interface_id(repo,f'{norm_path(rel)}#{name}'),'name':name,'moduleId':module_id(repo,rel),'codeEntityId':eid,'language':'java'},line_of(text,m.start()))
        for raw in (m.group(3),m.group(4)):
            if raw:
                for b in re.findall(r'\b[A-Z]\w*',raw): emit(rel,'implements-interface',{'implementerId':eid,'interfaceName':b,'interfaceRef':imports.get(b,b),'language':'java'},line_of(text,m.start()))
    # Methods (approximate)
    for m in re.finditer(r'(?:public|protected|private|static|final|synchronized|abstract|\s)+[\w<>\[\],.?]+\s+(\w+)\s*\(([^)]*)\)\s*(?:throws[^\{]+)?\{',text):
        n=m.group(1); entities[n]=emit_entity(emit,repo,rel,'java','method',n,n,line_of(text,m.start()),m.group(0).split('{')[0].strip())
    # Spring mappings near following method
    for m in re.finditer(r'@(Get|Post|Put|Delete|Patch)Mapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']+)["\']\s*\)',text):
        method,path=m.group(1).upper(),m.group(2); tail=text[m.end():m.end()+500]; mm=re.search(r'\b(\w+)\s*\([^)]*\)\s*(?:throws[^\{]+)?\{',tail); handler=entities.get(mm.group(1)) if mm else None
        emit(rel,'endpoint-definition',{'interfaceId':interface_id(repo,f'http:{method}:{path}'),'method':method,'path':path,'handlerId':handler,'framework':'spring'},line_of(text,m.start()))
    for m in re.finditer(r'\b([A-Za-z_]\w*(?:\.[A-Za-z_]\w*)?)\s*\(',text):
        name=m.group(1); root=name.split('.')[0]
        emit(rel,'code-call',{'callerId':None,'callee':name,'calleeSymbol':name.split('.')[-1],'resolvedTargetId':entities.get(name),'externalModule':imports.get(root)},line_of(text,m.start()))

def analyze_textual(repo,rel,text,ext,emit):
    if ext=='.py': return analyze_python(repo,rel,text,emit)
    if ext in {'.js','.jsx','.ts','.tsx'}: return analyze_js(repo,rel,text,emit)
    if ext=='.go': return analyze_go(repo,rel,text,emit)
    if ext=='.java': return analyze_java(repo,rel,text,emit)


def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--repo-dir',required=True); ap.add_argument('--repo-name',required=True)
    ap.add_argument('--commit'); ap.add_argument('--out',required=True)
    args=ap.parse_args(); repo=os.path.abspath(args.repo_dir)
    if not os.path.isdir(repo): print(f'repo 目錄不存在：{repo}',file=sys.stderr); return 1
    commit=args.commit or detect_commit(repo); Path(args.out).parent.mkdir(parents=True,exist_ok=True)
    with open(args.out,'w',encoding='utf-8') as out:
        E=Emitter(args.repo_name,commit,out); top_dirs=set()
        for dp,dns,fns in os.walk(repo):
            dns[:]=[d for d in dns if d not in SKIP_DIRS]
            for fn in fns:
                path=os.path.join(dp,fn); rel=norm_path(os.path.relpath(path,repo)); low=fn.lower(); ext=os.path.splitext(low)[1]
                if '/' in rel: top_dirs.add(rel.split('/')[0])
                text=read_text(path)
                if text is None: continue
                if low=='package.json':
                    info=parse_package_json(text)
                    if info:
                        if info.get('packageName'): E.emit(rel,'package-identity',{'ecosystem':'npm','name':info['packageName']})
                        for n,v in sorted(info['dependencies'].items()): E.emit(rel,'dependency',{'ecosystem':'npm','name':n,'version':v})
                elif low=='requirements.txt':
                    for n,v in sorted(parse_requirements(text).items()): E.emit(rel,'dependency',{'ecosystem':'pip','name':n,'version':v})
                elif low=='go.mod':
                    info=parse_go_mod(text)
                    if info.get('module'): E.emit(rel,'package-identity',{'ecosystem':'go','name':info['module']})
                    for n,v in sorted(info['dependencies'].items()): E.emit(rel,'dependency',{'ecosystem':'go','name':n,'version':v})
                elif low=='pom.xml':
                    for n,v in sorted(parse_pom(text).items()): E.emit(rel,'dependency',{'ecosystem':'maven','name':n,'version':v})
                if ext in {'.yaml','.yml','.json'} and looks_openapi(text): E.emit(rel,'openapi-server',parse_openapi(text))
                if fn=='Chart.yaml':
                    m=re.search(r'^name:\s*(.+)$',text,re.M); E.emit(rel,'helm-chart',{'chartName':m.group(1).strip() if m else None})
                if fn=='values.yaml':
                    for m in re.finditer(r'^\s*([\w.-]*(?:url|host|endpoint|address)[\w.-]*):\s*(\S.*)$',text,re.M|re.I): E.emit(rel,'helm-value-reference',{'key':m.group(1),'value':m.group(2).strip().strip('"\'')},line_of(text,m.start()))
                if ext=='.proto':
                    names=sorted(set(re.findall(r'^\s*message\s+(\w+)',text,re.M)))
                    if names:E.emit(rel,'event-schema',{'format':'protobuf','names':names})
                if ext in SOURCE_EXTS:
                    E.emit(rel,'module-source',{'moduleId':module_id(args.repo_name,rel),'path':rel,'language':ext.lstrip('.')})
                    analyze_textual(args.repo_name,rel,text,ext,E.emit)
                    for m in HTTP_URL_RE.finditer(text): E.emit(rel,'http-endpoint-reference',{'host':m.group(1),'path':m.group(2) or '/'},line_of(text,m.start()))
                    tables=extract_sql_tables(text)
                    for table in tables: E.emit(rel,'db-table-reference',{'table':table},None)
        for d in sorted(top_dirs): E.emit(d,'module-dir',{'path':d})
    print(f'[graphify-analyzer] {args.repo_name}@{commit or "unknown"}: {E.count} observations -> {args.out}',file=sys.stderr)
    return 0
if __name__=='__main__': sys.exit(main())
