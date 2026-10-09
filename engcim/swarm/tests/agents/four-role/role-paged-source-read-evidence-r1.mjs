import fs from 'node:fs';
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
