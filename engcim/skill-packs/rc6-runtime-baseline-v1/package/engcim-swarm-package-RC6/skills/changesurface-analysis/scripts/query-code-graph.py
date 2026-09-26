#!/usr/bin/env python3
"""Structured Code Graph query utility for ChangeSurface analysis.

Authoritative source: pk/code-graph/nodes.jsonl + edges.jsonl.
No ProductKB issue/comment reconstruction is used.
"""
import argparse,json,os,sys
from collections import defaultdict,deque

def load(path):
    out={}
    with open(path,encoding='utf-8') as f:
        for line in f:
            if line.strip():
                r=json.loads(line);out[r['id']]=r
    return out

def match_nodes(nodes,q):
    q=q.lower()
    return [n for n in nodes.values() if q in n.get('id','').lower() or q in n.get('name','').lower()]

def edge_view(e,nodes):
    return {'type':e['type'],'from':e['from'],'fromName':nodes.get(e['from'],{}).get('name'),'to':e['to'],'toName':nodes.get(e['to'],{}).get('name'),'validationState':e.get('validationState'),'confidence':e.get('confidence'),'evidence':e.get('evidence',[])}

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--graph',default='pk/code-graph')
    sp=ap.add_subparsers(dest='cmd',required=True)
    p=sp.add_parser('realizes');p.add_argument('query');p.add_argument('--depth',type=int,default=4)
    p=sp.add_parser('dependents');p.add_argument('query');p.add_argument('--depth',type=int,default=2)
    p=sp.add_parser('neighbors');p.add_argument('query')
    p=sp.add_parser('path');p.add_argument('source');p.add_argument('target');p.add_argument('--depth',type=int,default=6)
    a=ap.parse_args();nodes=load(os.path.join(a.graph,'nodes.jsonl'));edges=load(os.path.join(a.graph,'edges.jsonl'))
    outgoing=defaultdict(list);incoming=defaultdict(list)
    for e in edges.values():outgoing[e['from']].append(e);incoming[e['to']].append(e)
    if a.cmd=='neighbors':
        seeds=match_nodes(nodes,a.query);res=[]
        for n in seeds:
            res.append({'node':n,'outgoing':[edge_view(e,nodes) for e in outgoing[n['id']]],'incoming':[edge_view(e,nodes) for e in incoming[n['id']]]})
        print(json.dumps(res,ensure_ascii=False,indent=2));return
    if a.cmd=='realizes':
        seeds=match_nodes(nodes,a.query);seen=set(n['id'] for n in seeds);q=deque((n['id'],0) for n in seeds);found=[]
        allowed={'REALIZES','IMPLEMENTED_IN','CONTAINS','EXPOSES','IMPLEMENTS','USES','CONSUMES'}
        while q:
            nid,d=q.popleft()
            if d>=a.depth:continue
            # REALIZES is Component -> Capability, so for semantics seed follow incoming; otherwise both useful directions.
            candidates=incoming[nid]+outgoing[nid]
            for e in candidates:
                if e['type'] not in allowed:continue
                other=e['from'] if e['to']==nid else e['to']
                found.append(edge_view(e,nodes))
                if other not in seen:seen.add(other);q.append((other,d+1))
        print(json.dumps({'matches':seeds,'relations':found,'nodes':[nodes[x] for x in sorted(seen) if x in nodes]},ensure_ascii=False,indent=2));return
    if a.cmd=='dependents':
        seeds=match_nodes(nodes,a.query);seen=set(n['id'] for n in seeds);q=deque((n['id'],0) for n in seeds);rels=[]
        reverse_types={'CALLS','DEPENDS_ON','CONSUMES','USES','IMPLEMENTS'}
        while q:
            nid,d=q.popleft()
            if d>=a.depth:continue
            for e in incoming[nid]:
                if e['type'] not in reverse_types:continue
                rels.append(edge_view(e,nodes));src=e['from']
                if src not in seen:seen.add(src);q.append((src,d+1))
        print(json.dumps({'targets':seeds,'dependents':[nodes[x] for x in sorted(seen) if x in nodes and x not in {n['id'] for n in seeds}],'relations':rels},ensure_ascii=False,indent=2));return
    if a.cmd=='path':
        ss=match_nodes(nodes,a.source);ts=match_nodes(nodes,a.target);targets={n['id'] for n in ts};prev={};q=deque((n['id'],0) for n in ss);seen={n['id'] for n in ss};end=None
        while q and end is None:
            nid,d=q.popleft()
            if nid in targets:end=nid;break
            if d>=a.depth:continue
            for e in outgoing[nid]+incoming[nid]:
                other=e['to'] if e['from']==nid else e['from']
                if other in seen:continue
                seen.add(other);prev[other]=(nid,e);q.append((other,d+1))
        if end is None:print(json.dumps({'path':None,'sourceMatches':ss,'targetMatches':ts},ensure_ascii=False,indent=2));return
        steps=[];cur=end
        while cur not in {n['id'] for n in ss}:
            p,e=prev[cur];steps.append(edge_view(e,nodes));cur=p
        steps.reverse();print(json.dumps({'path':steps,'source':nodes.get(cur),'target':nodes.get(end)},ensure_ascii=False,indent=2))
if __name__=='__main__':main()
