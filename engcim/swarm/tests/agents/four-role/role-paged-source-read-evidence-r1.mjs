import fs from 'node:fs';
import path from 'node:path';
import {hash,checkUnitEvidence} from './role-unit-evidence-v3.mjs';
export function pageMatches(call,page){
 const input=call.input||{};
 return call.tool==='read_file'&&JSON.stringify(Object.keys(input).sort())===JSON.stringify(['line_offset','n_lines','path'])&&input.path===page.path&&input.line_offset===page.lineOffset&&input.n_lines===page.nLines;
}
export function classifyPageCalls(calls,pages){
 const seen=new Set(),unexpected=[];
 for(const call of calls){
  const index=pages.findIndex(p=>pageMatches(call,p));
  if(call.tool!=='read_file'||index<0||seen.has(index))unexpected.push(call);else seen.add(index);
 }
 return unexpected;
}
export function checkPagedReads(trace,files,pages){
 const checks=[];const expect=(name,actual,expected)=>checks.push({name,status:JSON.stringify(actual)===JSON.stringify(expected)?'PASS':'FAIL'});
 const calls=trace.filter(m=>['tool_use','tool_call'].includes(m.type)&&!(m.tool==='sdk-host_boundary_diagnostics'&&m.input?.producer==='SDK-host'&&m.input?.boundary==='shared-readonly-preflight'));
 expect('no actor calls outside exact finite page set',classifyPageCalls(calls,pages).length,0);
 expect('all finite pages read once',calls.length,pages.length);
 for(const file of files){
  const source=fs.readFileSync(file.path);expect(file.name+' whole frozen source pin',[source.length,hash(source)],[file.bytes,file.sha256]);
  const expectedLines=source.toString('utf8').replace(/\n$/,'').split('\n');const decoded=[];
  for(const page of pages.filter(p=>p.path===file.path)){
   const actual=calls.filter(c=>pageMatches(c,page));
   const results=actual.length===1?trace.filter(m=>m.type==='tool_result'&&m.call_id===actual[0].call_id):[];
   expect(page.id+' one complete untruncated result',[actual.length,results.length,results[0]?.output_truncated,typeof results[0]?.output],[1,1,false,'string']);
   const rows=(results[0]?.output||'').split('\n');const text=rows.map((line,i)=>{const m=/^(\d+)\t(.*)$/.exec(line);return m&&Number(m[1])===page.lineOffset+i?m[2]:null});
   expect(page.id+' exact numbered source range',[text.length,text.every(x=>x!==null),hash(text.join('\n'))],[page.nLines,true,page.sourceTextSha256]);decoded.push(...text);
  }
  expect(file.name+' complete source reassembled with no gaps',[decoded.length,hash(decoded.join('\n'))],[expectedLines.length,hash(expectedLines.join('\n'))]);
 }
 return {status:checks.every(c=>c.status==='PASS')?'PASS':'FAIL',checks,actorCalls:calls.length};
}
export function checkPagedSourceEvidence(e,s,pin,files,pages){
 const strict=checkUnitEvidence(e,s,pin);const retained=strict.checks.filter(c=>c.name!=='unit actor tool requests absent');const paged=checkPagedReads(e.trace||[],files,pages);
 return {kind:'BOUNDED_PAGED_SOURCE_CONSUMPTION_COMPONENT_ONLY',status:retained.every(c=>c.status==='PASS')&&paged.status==='PASS'?'PASS':'FAIL',checks:retained.concat(paged.checks),strictZeroActorUnitStatus:strict.status,strictZeroActorUnitChecksPreserved:strict.checks,readOperations:paged.actorCalls,finalExtraction:strict.finalExtraction,initialWholeWrapper:'UNVERIFIED',automaticAssignedSkillLoading:'UNVERIFIED',semantics:'PENDING_INDEPENDENT_SCORE',limits:'Different method grant/output contract; no retrospective repair of method307 or source-fix causality. Full read evidence plus separately graded behavior required.'};
}

// Public numbered Read output preserves source text lines, but does not encode
// the original final newline. Never synthesize that byte or claim initial loading.
export function inspectOwnedReadPages(trace, snapshot, context) {
  const base={byteDelivery:'UNVERIFIED_NUMBERED_FRAMING',automaticLoading:'UNVERIFIED',pages:[],coveredLines:0,missingRanges:[]};
  const out=(status,reason=null,extra={})=>({...base,status,reason,...extra});
  if(!Array.isArray(trace)||trace.length>2000||!snapshot||!context||typeof snapshot.content!=='string'||!Array.isArray(context.ownedPaths))return out('METHOD_UNSUPPORTED','INVALID_PAGE_INPUT');
  if(!context.run||!context.issue||!context.ownedPaths.includes(snapshot.path)||!path.isAbsolute(snapshot.path)||Buffer.byteLength(snapshot.content)>262144||snapshot.bytes!==Buffer.byteLength(snapshot.content)||snapshot.sha256!==hash(snapshot.content)||snapshot.sha256!==context.sourceSha256||!Number.isFinite(Date.parse(snapshot.at))||!Number.isFinite(Date.parse(context.startedAt))||Date.parse(snapshot.at)<Date.parse(context.startedAt))return out('SCOPE_REJECT','SOURCE_SNAPSHOT_BINDING');
  if(trace.some((t,j)=>t.seq!==j+1||t.task_id!==context.run||t.issue_id!==context.issue))return out('SCOPE_REJECT','TRACE_IDENTITY_OR_SEQUENCE');
  const calls=new Map(),results=new Map();
  for(const t of trace){
    if(t.type==='tool_use'||t.type==='tool_call'){
      if(typeof t.call_id!=='string'||!t.call_id||calls.has(t.call_id))return out('SCOPE_REJECT','DUPLICATE_OR_MISSING_CALL');
      calls.set(t.call_id,t);
      if(t.tool==='read_file'){
        const i=t.input||{},p=i.path||i.file_path;
        if(!context.ownedPaths.includes(p)||i.path&&i.file_path&&i.path!==i.file_path||Object.keys(i).some(k=>!['path','file_path','line_offset','n_lines','max_chars'].includes(k))||i.line_offset!=null&&(!Number.isSafeInteger(i.line_offset)||i.line_offset<1)||i.n_lines!=null&&(!Number.isSafeInteger(i.n_lines)||i.n_lines<1||i.n_lines>200)||i.max_chars!=null&&(!Number.isSafeInteger(i.max_chars)||i.max_chars<1||i.max_chars>65536))return out('SCOPE_REJECT','READ_PATH_OR_PAGE_GRAMMAR');
      }
    }else if(t.type==='tool_result'){
      const q=calls.get(t.call_id);
      if(!q||results.has(t.call_id)||q.tool!==t.tool||q.seq>=t.seq)return out('SCOPE_REJECT','RESULT_CALL_BINDING');
      results.set(t.call_id,t);
    }
  }
  const requests=[...calls.values()].filter(t=>t.tool==='read_file'&&(t.input.path||t.input.file_path)===snapshot.path);
  if(requests.some(t=>!results.has(t.call_id)))return out('PENDING_READ_RESULT');
  const lines=snapshot.content===''?[]:(snapshot.content.endsWith('\n')?snapshot.content.slice(0,-1):snapshot.content).split('\n');
  const covered=new Set(),pages=[];
  for(const q of requests){
    const r=results.get(q.call_id),page={requestSeq:q.seq,resultSeq:r.seq,callId:q.call_id,path:snapshot.path,output:r.output,truncated:r.output_truncated===true,verifiedLines:[],redactedLines:[],partialLine:null};
    pages.push(page);
    if(typeof r.output!=='string')return out('PENDING_READ_OUTPUT',null,{pages});
    if(Buffer.byteLength(r.output)>1048576)return out('SCOPE_REJECT','READ_OUTPUT_SIZE');
    if(r.output==='Tool output is empty.')continue;
    const raw=r.output.split('\n');let previous=(q.input.line_offset||1)-1;
    for(const [index,s] of raw.entries()){
      const m=/^([1-9][0-9]*)\t([\s\S]*)$/.exec(s);
      if(!m){
        if(page.truncated&&index===raw.length-1&&(s===''||/^[1-9][0-9]*$/.test(s)&&String(previous+1).startsWith(s))){page.partialFraming=s;break;}
        return out('SCOPE_REJECT','MALFORMED_NUMBERED_OUTPUT', {pages});
      }
      const number=Number(m[1]),text=m[2],expected=lines[number-1];
      if(!Number.isSafeInteger(number)||number!==previous+1||number>lines.length||q.input.n_lines!=null&&number>=(q.input.line_offset||1)+q.input.n_lines)return out('SCOPE_REJECT','LINE_ORDER_OR_RANGE',{pages});
      previous=number;
      if(text.includes('[REDACTED CREDENTIAL]')&&text!==expected)page.redactedLines.push(number);
      else if(text===expected){covered.add(number);page.verifiedLines.push(number);}
      else if(index===raw.length-1&&expected.startsWith(text))page.partialLine=number;
      else return out('SCOPE_REJECT','SOURCE_LINE_CONTENT_MISMATCH',{pages});
    }
  }
  const missingRanges=[];
  for(let n=1;n<=lines.length;n++)if(!covered.has(n)){const last=missingRanges.at(-1);if(last&&last[1]===n-1)last[1]=n;else missingRanges.push([n,n]);}
  const cleanCoverage=new Set(pages.filter(p=>!Object.hasOwn(p,'partialFraming')).flatMap(p=>p.verifiedLines));
  return out(pages.some(p=>p.redactedLines.length)?'PENDING_PUBLIC_REDACTION':pages.some(p=>Object.hasOwn(p,'partialFraming'))&&cleanCoverage.size<lines.length?'PENDING_PUBLIC_FRAMING':!lines.length?'PENDING_EMPTY_FILE_DELIVERY':missingRanges.length?'PENDING_TEXT_LINES':'MATCH_COMPLETE_TEXT_LINES',null,{pages,sourceLines:lines.length,coveredLines:covered.size,missingRanges,sourceSha256:snapshot.sha256,sourceBytes:snapshot.bytes,snapshotAt:snapshot.at});
}
