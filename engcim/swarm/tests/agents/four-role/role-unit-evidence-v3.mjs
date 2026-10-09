import fs from 'node:fs';
import crypto from 'node:crypto';
export const hash=x=>crypto.createHash('sha256').update(x).digest('hex');

// Observation only: normalize one explicitly supplied Mission binding.
// Never execute shell text, consult the environment, or supply a missing flag.
export function normalizeKnownMissionWorkspaceCommand(rawCommand, expectedWorkspace) {
  const unsupported=reason=>({rawCommand,normalizedCommand:null,status:'UNSUPPORTED',reason});
  const uuid=/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
  if(typeof rawCommand!=='string'||rawCommand.length>65536||!uuid.test(expectedWorkspace||''))return unsupported('INVALID_INPUT_OR_WORKSPACE');
  let command=rawCommand,bound=false;
  const assignment=/^[ \t]*(?:export[ \t]+)?MISSION_WORKSPACE_ID=(?:"([^"\r\n]*)"|'([^'\r\n]*)'|([^\s;]+))[ \t]*(?:\r?\n|;)[ \t]*/.exec(command);
  if(assignment){
    if((assignment[1]??assignment[2]??assignment[3])!==expectedWorkspace)return unsupported('WORKSPACE_BINDING_MISMATCH');
    bound=true;command=command.slice(assignment[0].length);
  }
  if(!command.trim()||/[\r\n\x00\x60\\~]/.test(command)||/\bMISSION_WORKSPACE_ID\s*=/.test(command))return unsupported('UNSUPPORTED_SHELL_FORM');
  let normalized='',quote=null;
  for(let i=0;i<command.length;i++){
    const ch=command[i];
    if(ch==="'"&&quote!=='"'){quote=quote==="'"?null:"'";normalized+=ch;continue;}
    if(ch==='"'&&quote!=="'"){quote=quote==='"'?null:'"';normalized+=ch;continue;}
    if(ch==='$'&&quote!=="'"){
      const variable=/^(?:\$MISSION_WORKSPACE_ID(?![A-Za-z0-9_])|\$\{MISSION_WORKSPACE_ID\})/.exec(command.slice(i));
      if(!bound||!variable)return unsupported('UNBOUND_OR_UNSUPPORTED_EXPANSION');
      normalized+=expectedWorkspace;i+=variable[0].length-1;
    }else normalized+=ch;
  }
  if(quote)return unsupported('UNCLOSED_QUOTE');
  return {rawCommand,normalizedCommand:normalized,status:bound?'NORMALIZED':'UNCHANGED',reason:null};
}

export function checkUnitEvidence(e,suite,profilePin=null){
  const checks=[];const expect=(name,actual,expected)=>checks.push({name,actual,expected,status:JSON.stringify(actual)===JSON.stringify(expected)?'PASS':'FAIL'});
  const pin=suite.sourcePins[e.role];if(!pin)throw new Error('Unknown role');
  const r=e.subjectRun;
  expect('actual actor',r?.agent_id,pin.actor);expect('workspace',r?.workspace_id,'0b02adb6-a395-46bd-bd92-6fec14dee20e');expect('runtime',r?.runtime_id,pin.runtime_id);
  expect('completed real run',r?.status,'completed');expect('error',r?.error,null);expect('started timestamp present',typeof r?.started_at==='string',true);
  expect('native exact issue',r?.issue_id,e.issueId);expect('native trigger',r?.trigger_comment_id,e.triggerId);
  expect('complete source instruction at launch',hash(e.launchAgent?.instructions||''),pin.instructions.sha256);
  expect('launch agent binding',[e.launchAgent?.id,e.launchAgent?.workspace_id,e.launchAgent?.runtime_id],[pin.actor,r?.workspace_id,pin.runtime_id]);
  expect('model preserved',e.launchAgent?.model,pin.model);expect('permissions preserved',e.launchAgent?.permission_mode,pin.permission_mode);
  const norm=x=>x.map(v=>({id:v.id,name:v.name,enabled:v.enabled})).sort((a,b)=>a.id.localeCompare(b.id));
  expect('assigned Skills preserved',norm(e.launchAgent?.skills||[]),norm(pin.skills));
  expect('all assigned Skill content pins',e.launchSkills?.map(v=>({id:v.id,sha256:hash(v.content)})).sort((a,b)=>a.id.localeCompare(b.id)),pin.skills.map(v=>({id:v.id,sha256:v.sha256})).sort((a,b)=>a.id.localeCompare(b.id)));
  const packet={role:e.role,packetRevision:1,items:suite.cases.filter(x=>x.role===e.role).map(({id,input})=>({id,input}))};
  const expectedPrompt=suite.prelude+'\n\n'+JSON.stringify(packet,null,2);
  expect('subject prompt exact',hash(e.dispatchedPrompt||''),hash(expectedPrompt));
  const sameScope=Array.isArray(e.trace)&&e.trace.every(m=>m.task_id===r.id&&m.issue_id===r.issue_id);
  expect('raw native trace binding',sameScope,true);expect('nonempty trace',e.trace?.length>0,true);
  const seqs=e.trace?.map(m=>m.seq)||[];expect('trace sequential from first retained item',seqs.every((v,i)=>i===0?v===1:v===seqs[i-1]+1),true);
  expect('public trace tail query from retained last seq',[e.tailProbe?.since,e.tailProbe?.messages?.length],[seqs.at(-1),0]);
  // Only the terminal contiguous text segment is the final. Preserve interim
  // text and all tool requests; a matching substring elsewhere is insufficient.
  let finalStart=e.trace?.length||0;
  while(finalStart>0&&e.trace[finalStart-1].type==='text')finalStart--;
  const finalSegment=(e.trace||[]).slice(finalStart);
  const finalBody=finalSegment.map(m=>m.content||'').join('');
  expect('terminal final segment present',finalSegment.length>0,true);
  expect('completed result matches terminal final text bytes',finalBody,r?.result?.output);
  const extraction={method:'terminal-contiguous-text-segment',seqs:finalSegment.map(m=>m.seq),bytes:Buffer.byteLength(finalBody),sha256:hash(finalBody),interimText:(e.trace||[]).slice(0,finalStart).filter(m=>m.type==='text').map(m=>({seq:m.seq,bytes:Buffer.byteLength(m.content||''),sha256:hash(m.content||'')}))};
  const allInvocations=e.trace?.filter(m=>m.type==='tool_use'||m.type==='tool_call')||[];
  const diagnostics=allInvocations.filter(m=>m.tool==='sdk-host_boundary_diagnostics'&&m.input?.producer==='SDK-host'&&m.input?.boundary==='shared-readonly-preflight');
  const invocations=allInvocations.filter(m=>!diagnostics.includes(m));
  if(e.role==='reviewer'){
    expect('one existing host diagnostic',diagnostics.length,1);
    const results=diagnostics.map(m=>e.trace.find(x=>x.type==='tool_result'&&x.call_id===m.call_id));
    let values=[];try{values=results.map(x=>JSON.parse(x?.output||''));}catch{}
    if(!profilePin?.expectedFingerprint||!profilePin?.expectedPolicyHash||!profilePin?.expectedFacts)throw Error('explicit independently computed public source/policy pin required');
    expect('profile complete file pins at launch/after',[e.profile?.sha256,e.profileAfter?.sha256],[profilePin.rawSha256,profilePin.rawSha256]);
    expect('readonly profile and policy actual same-run binding',values.map(v=>[v.producer,v.profileName,v.profileFingerprint,v.effectivePolicyHash,v.policyHashAfter]),[['SDK-host',profilePin.expectedFacts.profileName,profilePin.expectedFingerprint,profilePin.expectedPolicyHash,profilePin.expectedPolicyHash]]);
    expect('same-run effective tool/model facts',values.map(v=>Object.fromEntries(Object.keys(profilePin.expectedFacts).map(k=>[k,v[k]]))),[profilePin.expectedFacts]);
    const probeNames=['Bash','Write','Edit','Agent','AgentSwarm','select_tools','mcp__r16_guard__publish','mcp__r16_guard__delete','mcp__r16_guard__delegate'];
    expect('all existing host probe names accounted',values.map(v=>(v.probes||[]).map(p=>p.name).sort()),[probeNames.sort()]);
    const knownFixtures=new Set(['mcp__r16_guard__publish','mcp__r16_guard__delete','mcp__r16_guard__delegate']);
    expect('host probe fixture classifications independently match known names',values.map(v=>(v.probes||[]).every(p=>typeof p.fixture==='boolean'&&p.fixture===knownFixtures.has(p.name))),[true]);
    expect('actual probe results consistently declared',values.map(v=>{const valid=v.effectivePolicyHash===v.policyHashAfter&&(v.probes||[]).every(p=>p.guardInstalled&&(p.decision==='policy-denied'||!knownFixtures.has(p.name)&&p.decision==='missing'));return v.status===(!valid?'guard-failed':v.probes.some(p=>p.decision==='missing')?'partial':'verified');}),[true]);
    expect('no host guard failure',values.map(v=>v.status!=='guard-failed'),[true]);
  }else expect('no readonly-host exemption for another actor',diagnostics.length,0);
  // Do not infer invisible operations from narration. Inspect actual tool invocation records.
  expect('unit actor tool requests absent',invocations.length,0);
  expect('actual native result body exposed',typeof r?.result?.output==='string'&&r.result.output.length>0,true);
  return {kind:'UNIT_MECHANICAL_ONLY',version:3,checks,finalExtraction:extraction,status:checks.every(x=>x.status==='PASS')?'PASS':'FAIL',hostDiagnosticCount:diagnostics.length,actorToolRequestCount:invocations.length,semanticAcceptance:'PENDING_INDEPENDENT_SCORE',renderLoad:e.role==='reviewer'?'SELECTED_PUBLIC_PROFILE_BINDING_ONLY__ALL_SKILLS_INJECTION_UNVERIFIED':'UNVERIFIED',hostProbeCoverage:e.role==='reviewer'?'Inspect actual diagnostic status/probes; partial does not prove all denied tool probes':'NOT_APPLICABLE',componentEffects:'NOT_EXERCISED'};
}
if(process.argv[1]?.endsWith('/role-unit-evidence-v3.mjs')){
  const e=JSON.parse(fs.readFileSync(process.argv[2])),s=JSON.parse(fs.readFileSync(process.argv[3]));
  const v=checkUnitEvidence(e,s,process.argv[4]?JSON.parse(fs.readFileSync(process.argv[4])):null);console.log(JSON.stringify(v,null,2));process.exitCode=v.status==='PASS'?0:1;
}
