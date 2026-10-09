import crypto from 'node:crypto';
import {pageMatches} from './role-paged-source-read-evidence-r1.mjs';
import {checkOutputSchema} from './role-unit-output-schema-r1.mjs';
const hash=x=>crypto.createHash('sha256').update(x).digest('hex');
function lex(command){if(typeof command!=='string'||/[\n\r;$`|<>\\]/.test(command))return null;const pieces=command.split('&&');if(pieces.length>2||pieces.some(p=>p.includes('&')))return null;const groups=[];for(const piece of pieces){const tokens=[];let i=0;while(i<piece.length){while(i<piece.length&&/\s/.test(piece[i]))i++;if(i===piece.length)break;let token='';if(piece[i]==="'"||piece[i]==='"'){const q=piece[i++];while(i<piece.length&&piece[i]!==q)token+=piece[i++];if(piece[i++]!==q||i<piece.length&&!/\s/.test(piece[i]))return null;}else{while(i<piece.length&&!/\s/.test(piece[i])){if(piece[i]==="'"||piece[i]==='"')return null;token+=piece[i++];}}tokens.push(token);}if(!tokens.length)return null;groups.push(tokens);}return groups;}
function flags(tokens,allowed){const values={};for(let i=0;i<tokens.length;i++){const k=tokens[i];if(!allowed.includes(k)||k in values)return null;if(['--no-start','--roots-only','--summary','--compact'].includes(k))values[k]=true;else{const v=tokens[++i];if(v===undefined||v.startsWith('--'))return null;values[k]=v;}}return values;}
export function classifyComponentTrace(trace,grant,pages){
 const seen=new Set(),verified=new Set(),pending=new Map(),unexpected=[],unproven=[];
 const counts={get:0,listBefore:0,listAfter:0,start:0,review:0,write:0,post:0,cleanup:0};let body=null,writeAcknowledged=false,postAck=null,readbackAcknowledged=false;
 const sameComment=c=>c&&typeof c.id==='string'&&c.issue_id===grant.issueId&&c.parent_id===grant.triggerId&&c.author_type==='agent'&&c.author_id===grant.actor&&c.source_task_id===grant.runId&&c.content===body;
 const fail=(call,reason)=>{unexpected.push({...call,guardReason:reason});};
 const prepareCLI=tokens=>{
  if(tokens[0]==='rm'){if(tokens.length!==3||!['--','-f'].includes(tokens[1])||tokens[2]!==grant.bodyPath||counts.cleanup!==0)return null;return {kind:'cleanup'};}
  if(!['multica','/opt/homebrew/bin/multica'].includes(tokens.shift())||tokens.shift()!=='--workspace-id'||tokens.shift()!==grant.workspace||tokens.shift()!=='issue')return null;
  const kind=tokens.shift();if(kind==='get'){if(tokens.shift()!==grant.issueId)return null;const f=flags(tokens,['--output']);return f?.['--output']==='json'&&counts.get===0?{kind:'get'}:null;}
  if(kind==='status'){if(tokens.shift()!==grant.issueId)return null;const target=tokens.shift(),f=flags(tokens,['--output','--no-start']);if(f?.['--no-start']!==true||f?.['--output']!=='json')return null;if(target==='in_progress'&&counts.post===0&&counts.start===0)return {kind:'start'};if(target==='in_review'&&counts.review===0)return {kind:'review'};return null;}
  if(kind!=='comment')return null;const verb=tokens.shift();if(tokens.shift()!==grant.issueId)return null;
  if(verb==='list'){const f=flags(tokens,['--output','--roots-only','--summary','--compact','--thread','--tail']);if(!f||f['--output']!=='json')return null;if((f['--thread']!==undefined||f['--tail']!==undefined)&&(f['--thread']!==grant.triggerId||f['--tail']!=='30'||f['--roots-only']||f['--summary']))return null;if(counts.post===0&&counts.listBefore<2)return {kind:'listBefore'};if(counts.post===1&&counts.listAfter===0&&!f['--compact']&&!f['--summary']&&!f['--roots-only'])return {kind:'listAfter'};return null;}
  if(verb==='add'){const f=flags(tokens,['--parent','--content-file','--output']);return f?.['--parent']===grant.triggerId&&f?.['--content-file']===grant.bodyPath&&f?.['--output']==='json'&&counts.post===0?{kind:'post'}:null;}return null;
 };
 for(const call of trace){
  if(call.type==='tool_result'){
   const operation=pending.get(call.call_id);if(!operation)continue;
   const complete=call.output_truncated===false&&typeof call.output==='string';
   if(operation.kind==='page'){
    const p=pages[operation.index],rows=(complete?call.output:'').split('\n');const decoded=rows.map((line,i)=>{const m=/^(\d+)\t(.*)$/.exec(line);return m&&Number(m[1])===p.lineOffset+i?m[2]:null;});
    if(complete&&decoded.length===p.nLines&&decoded.every(x=>x!==null)&&hash(decoded.join('\n'))===p.sourceTextSha256)verified.add(operation.index);else unproven.push({seq:call.seq,operation:'source-page-result',reason:'missing/truncated/mismatched source result'});
   }else if(operation.kind==='write'){
    writeAcknowledged=complete&&call.output===`Wrote ${Buffer.byteLength(body)} bytes to ${grant.bodyPath}`;if(!writeAcknowledged)unproven.push({seq:call.seq,operation:'write',reason:'literal Write result not exposed or mismatch'});
   }else if(['post','listAfter'].includes(operation.kind)){
    let value;try{if(complete)value=JSON.parse(call.output);}catch{}
    if(operation.kind==='post'){const c=value?.comment||value;if(sameComment(c))postAck=c;else unproven.push({seq:call.seq,operation:'post-ACK',reason:'complete source-task/author/parent/exact-body ACK missing'});}
    else{const rows=Array.isArray(value)?value:value?.comments;readbackAcknowledged=!!postAck&&Array.isArray(rows)&&rows.some(c=>c.id===postAck.id&&sameComment(c));if(!readbackAcknowledged)unproven.push({seq:call.seq,operation:'readback',reason:'complete exact ACK comment absent in public tool result'});}
   }
   continue;
  }
  if(!['tool_use','tool_call'].includes(call.type))continue;
  const index=pages.findIndex(p=>pageMatches(call,p));
  if(index>=0&&!seen.has(index)){seen.add(index);pending.set(call.call_id,{kind:'page',index});continue;}
  if(['write_file','Write'].includes(call.tool)){
   const i=call.input||{},keys=Object.keys(i).sort().join(',');let valid=false;
   if((keys==='content,path'||keys==='content,mode,path'&&i.mode==='overwrite')&&i.path===grant.bodyPath&&typeof i.content==='string'&&Buffer.byteLength(i.content)<=262144&&!i.content.includes('mention://')&&verified.size===pages.length&&counts.write===0){try{JSON.parse(i.content);valid=checkOutputSchema(i.content,grant.caseIds).status==='PASS';}catch{}}
   if(valid){counts.write++;body=i.content;pending.set(call.call_id,{kind:'write'});}else fail(call,'write target/content/count or complete source-read prerequisite invalid');continue;
  }
  if(call.tool!=='terminal'||!['command','command,cwd'].includes(Object.keys(call.input||{}).sort().join(','))||(call.input?.cwd!==undefined&&call.input.cwd!==grant.publicCwd)){fail(call,'outside exact source Read / Write / terminal operation grant');continue;}
  const groups=lex(call.input.command);const prepared=groups?.map(g=>prepareCLI([...g]));if(!prepared||prepared.some(x=>!x)||prepared.length===2&&(prepared[0].kind!=='post'||prepared[1].kind!=='cleanup')){fail(call,'unsupported command/workspace/target/flags/compound');continue;}
  const first=prepared[0];let ready=true;
  if(first.kind==='post')ready=writeAcknowledged&&verified.size===pages.length;
  if(first.kind==='listAfter')ready=!!postAck;
  if(first.kind==='review')ready=!!postAck&&readbackAcknowledged;
  if(first.kind==='cleanup')ready=!!postAck;
  if(!ready){fail(call,'conditional prerequisite UNPROVEN; request counters are not success/ACK');continue;}
  for(const op of prepared)counts[op.kind]++;
  pending.set(call.call_id,first);
  // post && rm is a specifically permitted shell condition; the request never
  // establishes that either post or cleanup succeeded.
 }
 return {unexpected,counts,sourcePagesRead:seen.size,sourcePagesVerified:verified.size,writeAcknowledged,postAcknowledged:!!postAck,readbackAcknowledged,body,unproven,interpretation:'Missing public tool outputs leave ACK UNKNOWN; not proof actor fabricated or did not see an SDK result. Platform witness is graded separately; no retrospective prerequisite substitution.'};
}
