#!/usr/bin/env python3
"""Materialize repository observations and correlated edges into pk/code-graph.

Graphify-level materialization:
- Repository / Component / source Module nodes
- CodeEntity nodes (class/function/method/interface)
- CONTAINS Repository->Module->CodeEntity
- CALLS for symbol-resolved local and cross-repo calls
- Interface nodes for source interfaces and HTTP endpoints
- IMPLEMENTS CodeEntity->Interface
- EXPOSES Component->Interface
- USES Component/CodeEntity->Interface for DB contracts when evidenced
- incremental idempotent upsert + STALE marking
"""
import argparse, json, os, re, sys
from datetime import datetime, timezone
AUTO='auto-observation'; PROV='PROVISIONAL'

def now_iso(): return datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
def slug(t): return re.sub(r'[^a-z0-9]+','-',(t or '').lower()).strip('-') or 'api'
def load_jsonl(path):
    out={}
    if not os.path.exists(path): return out
    with open(path,encoding='utf-8') as f:
        for n,line in enumerate(f,1):
            line=line.strip()
            if not line: continue
            try:r=json.loads(line)
            except Exception as e:
                print(f'[graph-update] skip {path}:{n}: {e}',file=sys.stderr); continue
            if 'id' in r: out[r['id']]=r
    return out
def write_jsonl(path,items):
    tmp=path+'.tmp'
    with open(tmp,'w',encoding='utf-8') as f:
        for k in sorted(items): f.write(json.dumps(items[k],ensure_ascii=False)+'\n')
    os.replace(tmp,path)

class Graph:
    def __init__(self,nodes,edges):
        self.nodes,self.edges=nodes,edges; self.added_nodes=self.updated_nodes=self.added_edges=self.updated_edges=0
    def ensure_node(self,nid,name=None,attrs=None,evidence=None):
        ntype=nid.split(':',1)[0]
        if nid in self.nodes:
            n=self.nodes[nid]; changed=False
            if attrs:
                n.setdefault('attrs',{})
                for k,v in attrs.items():
                    if v is not None and n['attrs'].get(k)!=v: n['attrs'][k]=v; changed=True
            if evidence:
                ev=n.setdefault('evidence',[])
                for x in evidence:
                    if x not in ev: ev.append(x); changed=True
            if n.get('validationState')=='STALE': n['validationState']=PROV; n.pop('staleAt',None); changed=True
            if changed:self.updated_nodes+=1
            return n
        n={'id':nid,'nodeType':ntype,'name':name or nid.split(':',1)[1],
           'validationState':PROV,'confidence':AUTO,'evidence':list(evidence or []),
           'governanceIssueRef':None,'attrs':dict(attrs or {})}
        self.nodes[nid]=n; self.added_nodes+=1; return n
    def upsert_edge(self,e):
        if not e.get('evidence'):
            print(f"[graph-update] reject edge without evidence: {e.get('id')}",file=sys.stderr); return
        eid=e['id']
        if eid in self.edges:
            old=self.edges[eid]; seen={(x.get('evidenceType'),x.get('detail'),x.get('commit')) for x in old.get('evidence',[])}
            for x in e['evidence']:
                k=(x.get('evidenceType'),x.get('detail'),x.get('commit'))
                if k not in seen: old.setdefault('evidence',[]).append(x);seen.add(k)
            if old.get('validationState')=='STALE': old['validationState']=PROV;old.pop('staleAt',None)
            self.updated_edges+=1
        else:
            ne=dict(e);ne.setdefault('confidence',AUTO);ne.setdefault('validationState',PROV);ne.setdefault('governanceIssueRef',None)
            self.edges[eid]=ne;self.added_edges+=1
        self.ensure_node(e['from']);self.ensure_node(e['to'])
    def edge(self,etype,src,dst,ev):
        self.upsert_edge({'id':f'{etype}|{src}|{dst}','type':etype,'from':src,'to':dst,'evidence':[ev],
                          'confidence':AUTO,'validationState':PROV,'governanceIssueRef':None})

def ev(rec,detail,etype='code-analysis'):
    loc=rec.get('file') or '?'
    if rec.get('line'): loc+=f":{rec['line']}"
    return {'evidenceType':etype,'detail':f'{loc}: {detail}','sourceRepo':rec.get('repo'),
            'commit':rec.get('commit'),'observedAt':rec.get('observedAt')}

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--graph',default='pk/code-graph');ap.add_argument('--observations',nargs='*',default=[]);ap.add_argument('--edges',nargs='*',default=[]);ap.add_argument('--stale-scope',default='')
    a=ap.parse_args();os.makedirs(a.graph,exist_ok=True)
    np=os.path.join(a.graph,'nodes.jsonl');ep=os.path.join(a.graph,'edges.jsonl');g=Graph(load_jsonl(np),load_jsonl(ep))
    observations=[]
    for path in a.observations:
        with open(path,encoding='utf-8') as f:
            for line in f:
                if line.strip(): observations.append(json.loads(line))
    # Interface index permits same-repo implementation resolution.
    iface_by_repo_name={}
    for r in observations:
        if r.get('observationType')=='interface-definition':
            o=r['observation']; iface_by_repo_name[(r['repo'],o.get('name'))]=o.get('interfaceId')
    refreshed=set(); repos=set(); impl_done=set()
    for r in observations:
        repo=r.get('repo');
        if not repo: continue
        repos.add(repo);t=r.get('observationType');o=r.get('observation') or {}
        rid=f'Repository:{repo}';cid=f'Component:{repo}'
        g.ensure_node(rid,name=repo,attrs={'commit':r.get('commit')});g.ensure_node(cid,name=repo)
        if repo not in impl_done:
            impl_done.add(repo);eid=f'IMPLEMENTED_IN|{cid}|{rid}';refreshed.add(eid);g.edge('IMPLEMENTED_IN',cid,rid,ev(r,f'{repo} implementation repository'))
        if t=='module-source':
            mid=o['moduleId'];g.ensure_node(mid,name=o.get('path'),attrs={'repository':repo,'path':o.get('path'),'language':o.get('language')});eid=f'CONTAINS|{rid}|{mid}';refreshed.add(eid);g.edge('CONTAINS',rid,mid,ev(r,'source module'))
        elif t=='module-dir':
            mid=f"Module:{repo}/{o.get('path')}";g.ensure_node(mid,name=o.get('path'),attrs={'repository':repo,'path':o.get('path')});eid=f'CONTAINS|{rid}|{mid}';refreshed.add(eid);g.edge('CONTAINS',rid,mid,ev(r,'top-level module directory'))
        elif t=='code-entity':
            eid=o['entityId'];mid=o['moduleId'];g.ensure_node(mid,name=o.get('moduleId','').split('/',1)[-1],attrs={'repository':repo,'path':r.get('file')});g.ensure_node(eid,name=o.get('name'),attrs={'kind':o.get('entityKind'),'qualname':o.get('qualname'),'language':o.get('language'),'signature':o.get('signature'),'file':r.get('file'),'line':r.get('line'),'endLine':r.get('endLine'),'visibility':o.get('visibility')});edgeid=f'CONTAINS|{mid}|{eid}';refreshed.add(edgeid);g.edge('CONTAINS',mid,eid,ev(r,f"contains {o.get('entityKind')} {o.get('qualname') or o.get('name')}"))
        elif t=='code-call' and o.get('callerId') and o.get('resolvedTargetId'):
            edgeid=f"CALLS|{o['callerId']}|{o['resolvedTargetId']}";refreshed.add(edgeid);g.edge('CALLS',o['callerId'],o['resolvedTargetId'],ev(r,f"call {o.get('callee')}"))
        elif t=='interface-definition':
            iid=o['interfaceId'];g.ensure_node(iid,name=o.get('name'),attrs={'kind':'code-interface','module':o.get('moduleId'),'language':o.get('language'),'file':r.get('file'),'line':r.get('line')})
            if o.get('codeEntityId'):
                edgeid=f"IMPLEMENTS|{o['codeEntityId']}|{iid}";refreshed.add(edgeid);g.edge('IMPLEMENTS',o['codeEntityId'],iid,ev(r,f"interface declaration {o.get('name')}"))
        elif t=='implements-interface':
            iid=iface_by_repo_name.get((repo,o.get('interfaceName')))
            if iid and o.get('implementerId'):
                edgeid=f"IMPLEMENTS|{o['implementerId']}|{iid}";refreshed.add(edgeid);g.edge('IMPLEMENTS',o['implementerId'],iid,ev(r,f"implements {o.get('interfaceRef') or o.get('interfaceName')}"))
        elif t=='endpoint-definition':
            iid=o['interfaceId'];g.ensure_node(iid,name=f"{o.get('method')} {o.get('path')}",attrs={'kind':'http-endpoint','method':o.get('method'),'path':o.get('path'),'framework':o.get('framework'),'file':r.get('file'),'line':r.get('line')})
            e1=f'EXPOSES|{cid}|{iid}';refreshed.add(e1);g.edge('EXPOSES',cid,iid,ev(r,f"source endpoint {o.get('method')} {o.get('path')}"))
            if o.get('handlerId'):
                e2=f"IMPLEMENTS|{o['handlerId']}|{iid}";refreshed.add(e2);g.edge('IMPLEMENTS',o['handlerId'],iid,ev(r,f"handler implements endpoint {o.get('method')} {o.get('path')}"))
        elif t=='openapi-server':
            title=o.get('title') or r.get('file','api');iid=f'Interface:{repo}/{slug(title)}';g.ensure_node(iid,name=title,attrs={'kind':'openapi','specRef':f"{repo}/{r.get('file')}",'paths':o.get('paths',[])})
            edgeid=f'EXPOSES|{cid}|{iid}';refreshed.add(edgeid);g.edge('EXPOSES',cid,iid,ev(r,f"OpenAPI {title}", 'code-analysis'))
        elif t=='db-table-reference':
            iid=f"Interface:{repo}/db:{o.get('table')}";g.ensure_node(iid,name=o.get('table'),attrs={'kind':'db-contract','table':o.get('table')});edgeid=f'USES|{cid}|{iid}';refreshed.add(edgeid);g.edge('USES',cid,iid,ev(r,f"database table reference {o.get('table')}",'db-contract'))
    # Correlated cross-repo edges
    for path in a.edges:
        with open(path,encoding='utf-8') as f:
            for line in f:
                if not line.strip(): continue
                e=json.loads(line);refreshed.add(e['id']);g.upsert_edge(e)
    scope={x.strip() for x in a.stale_scope.split(',') if x.strip()};stale=0
    if scope:
        for eid,e in g.edges.items():
            if eid in refreshed: continue
            evs=e.get('evidence',[])
            if evs and all(x.get('sourceRepo') in scope for x in evs):
                if e.get('validationState')!='STALE':e['validationState']='STALE';e['staleAt']=now_iso();stale+=1
    write_jsonl(np,g.nodes);write_jsonl(ep,g.edges)
    print(f'[graph-update] nodes +{g.added_nodes}/~{g.updated_nodes}, edges +{g.added_edges}/~{g.updated_edges}, STALE {stale}; total {len(g.nodes)} nodes / {len(g.edges)} edges',file=sys.stderr)
    return 0
if __name__=='__main__':sys.exit(main())
