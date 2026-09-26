#!/usr/bin/env python3
"""Cross-repository semantic correlation for Graphify-level observations.

Resolves package/import dependencies, HTTP consumers, shared event contracts and
cross-repo code calls. Every edge requires provenance and starts PROVISIONAL.
"""
import argparse,json,re,sys
from collections import defaultdict
AUTO='auto-observation';PROV='PROVISIONAL'
def slug(t): return re.sub(r'[^a-z0-9]+','-',(t or '').lower()).strip('-') or 'api'
def norm(s): return (s or '').lower().replace('_','-')
def package_matches(value,repo,identities):
    def n(x): return re.sub(r'[-_.]+','-',(x or '').lower())
    v=n(value); r=n(repo)
    if not v:return False
    if v==r or r in v.split('/') or v.endswith('/'+r) or v.startswith(r+'-'): return True
    return any(v==n(x) or v.startswith(n(x)+'-') or n(x) in v for x in identities.get(repo,set()))
def path_match(a,b):
    if not a or not b:return False
    def canon(x):return re.sub(r'\{[^}]+\}|:[A-Za-z_]\w*','{}',x.split('?',1)[0].rstrip('/') or '/')
    return canon(a)==canon(b)
def host_matches(h,repo):
    h=(h or '').lower();r=repo.lower();return h==r or h.split('.')[0]==r or h.startswith(r+'.')

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--observations',nargs='+',required=True);ap.add_argument('--out',required=True);a=ap.parse_args()
    by=defaultdict(list)
    for p in a.observations:
        with open(p,encoding='utf-8') as f:
            for n,line in enumerate(f,1):
                if not line.strip():continue
                try:r=json.loads(line)
                except Exception as e:print(f'[correlate] skip {p}:{n}: {e}',file=sys.stderr);continue
                by[r.get('repo','?')].append(r)
    repos=sorted(by);ids=defaultdict(set);apis=defaultdict(list);symbols=defaultdict(lambda:defaultdict(list));schemas=defaultdict(dict)
    for repo in repos:
        for r in by[repo]:
            t=r['observationType'];o=r['observation']
            if t=='package-identity' and o.get('name'):ids[repo].add(o['name'])
            elif t=='openapi-server':
                title=o.get('title') or r.get('file','api');apis[repo].append({'id':f'Interface:{repo}/{slug(title)}','title':title,'paths':o.get('paths') or [],'servers':o.get('servers') or [],'rec':r})
            elif t=='endpoint-definition':apis[repo].append({'id':o['interfaceId'],'title':f"{o.get('method')} {o.get('path')}",'paths':[o.get('path')],'servers':[],'method':o.get('method'),'rec':r})
            elif t=='code-entity':symbols[repo][o.get('name')].append(o)
            elif t=='event-schema':
                for n in o.get('names',[]):schemas[repo].setdefault(n,r)
    edges={}
    def evidence(kind,detail,r):return {'evidenceType':kind,'detail':detail,'sourceRepo':r.get('repo'),'commit':r.get('commit'),'observedAt':r.get('observedAt')}
    def add(kind,src,dst,ev):
        eid=f'{kind}|{src}|{dst}'
        if eid not in edges:edges[eid]={'id':eid,'type':kind,'from':src,'to':dst,'evidence':[],'confidence':AUTO,'validationState':PROV,'governanceIssueRef':None}
        seen={(x['evidenceType'],x['detail'],x.get('commit')) for x in edges[eid]['evidence']}
        k=(ev['evidenceType'],ev['detail'],ev.get('commit'))
        if k not in seen:edges[eid]['evidence'].append(ev)
    for a_repo in repos:
        for r in by[a_repo]:
            t=r['observationType'];o=r['observation']
            for b_repo in repos:
                if a_repo==b_repo:continue
                if t=='dependency':
                    name=o.get('name','')
                    if package_matches(name,b_repo,ids):add('DEPENDS_ON',f'Component:{a_repo}',f'Component:{b_repo}',evidence('package-dependency',f"{r.get('file')}: dependency {name} matches {b_repo}",r))
                elif t=='import':
                    mod=o.get('module','')
                    if package_matches(mod,b_repo,ids):add('USES',f'Component:{a_repo}',f'Component:{b_repo}',evidence('code-analysis',f"{r.get('file')}:{r.get('line')}: import {mod} resolves to {b_repo}",r))
                elif t=='code-call' and o.get('externalModule') and o.get('callerId'):
                    if package_matches(o['externalModule'],b_repo,ids):
                        cands=symbols[b_repo].get(o.get('calleeSymbol')) or []
                        if len(cands)==1:
                            add('CALLS',o['callerId'],cands[0]['entityId'],evidence('code-analysis',f"{r.get('file')}:{r.get('line')}: {o.get('callee')} resolved via {o.get('externalModule')}",r))
                        else:
                            add('DEPENDS_ON',f'Component:{a_repo}',f'Component:{b_repo}',evidence('code-analysis',f"{r.get('file')}:{r.get('line')}: external call {o.get('callee')} via {o.get('externalModule')}",r))
                elif t in {'http-request','http-endpoint-reference'}:
                    host=o.get('host');path=o.get('path')
                    matched_specific=[]; matched_generic=[]
                    for api in apis[b_repo]:
                        path_ok=any(path_match(path,p) for p in api.get('paths',[]) if p) if path else False
                        server_ok=any(host_matches(re.sub(r'^https?://','',s).split('/')[0],b_repo) for s in api.get('servers',[]))
                        if path_ok: matched_specific.append(api)
                        elif server_ok: matched_generic.append(api)
                    if matched_specific:
                        for api in matched_specific:
                            add('CONSUMES',f'Component:{a_repo}',api['id'],evidence('api-usage',f"{r.get('file')}:{r.get('line')}: request {o.get('method') or ''} {path} matches {b_repo} {api['title']}",r))
                    elif host and host_matches(host,b_repo):
                        # Host-level evidence proves dependency but not a specific endpoint unless path matches.
                        add('DEPENDS_ON',f'Component:{a_repo}',f'Component:{b_repo}',evidence('api-usage',f"{r.get('file')}:{r.get('line')}: HTTP host {host} resolves to {b_repo}; endpoint unresolved",r))
                    elif matched_generic:
                        for api in matched_generic:
                            add('CONSUMES',f'Component:{a_repo}',api['id'],evidence('api-usage',f"{r.get('file')}:{r.get('line')}: host matches OpenAPI server for {b_repo}",r))
                elif t=='helm-value-reference' and b_repo.lower() in (o.get('value') or '').lower():
                    add('DEPENDS_ON',f'Component:{a_repo}',f'Component:{b_repo}',evidence('helm-config-reference',f"{r.get('file')}: {o.get('key')}={o.get('value')}",r))
                elif t=='event-schema':
                    for name in o.get('names',[]):
                        if name in schemas[b_repo]:add('DEPENDS_ON',f'Component:{a_repo}',f'Component:{b_repo}',evidence('message-schema',f"{r.get('file')} and {schemas[b_repo][name].get('file')} share schema {name}",r))
    with open(a.out,'w',encoding='utf-8') as f:
        for k in sorted(edges):f.write(json.dumps(edges[k],ensure_ascii=False)+'\n')
    print(f'[correlate] {len(repos)} repos -> {len(edges)} cross-repo edges -> {a.out}',file=sys.stderr);return 0
if __name__=='__main__':sys.exit(main())
