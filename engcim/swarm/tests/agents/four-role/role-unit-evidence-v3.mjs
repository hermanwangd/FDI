import fs from 'node:fs';
import crypto from 'node:crypto';
import path from 'node:path';
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


// Redact only an actual readonly cat operand in the private-path observation view.
// This view is not authorization; all original command/scope checks still apply.
export function observeOwnedPublicContextReads(rawCommand, ownedRoots) {
  const result=(status,observedCommand=rawCommand,maskedPaths=[])=>({rawCommand,observedCommand,maskedPaths,status});
  if(typeof rawCommand!=='string'||rawCommand.length>65536||!Array.isArray(ownedRoots)||!ownedRoots.length||ownedRoots.some(x=>typeof x!=='string'||!path.isAbsolute(x))||/[\r\n\x00\x60\\]/.test(rawCommand))return result('UNSUPPORTED');
  const groups=[[]];let word='',start=null,quote=null;
  const flush=end=>{if(start!==null){groups.at(-1).push({word,start,end});word='';start=null;}};
  for(let i=0;i<rawCommand.length;i++){
    const ch=rawCommand[i];
    if(quote){if(ch===quote)quote=null;else word+=ch;continue;}
    if(ch==='#')return result('UNSUPPORTED');
    if(ch==='"'||ch==="'"){if(start===null)start=i;quote=ch;continue;}
    if(/\s/.test(ch)){flush(i);continue;}
    if([';','|','&'].includes(ch)&&!(ch==='&'&&rawCommand[i-1]==='>')){flush(i);if(rawCommand[i+1]===ch)i++;groups.push([]);continue;}
    if(start===null)start=i;word+=ch;
  }
  if(quote)return result('UNSUPPORTED');flush(rawCommand.length);
  const masks=[];
  for(const tokens of groups){
    if(tokens[0]?.word!=='cat'||tokens.length<2||tokens.length>3||tokens.length===3&&rawCommand.slice(tokens[2].start,tokens[2].end)!=='2>/dev/null')continue;
    const token=tokens[1],value=token.word;
    if(/[~$<>]/.test(value)||value.split('/').includes('..'))continue;
    for(const root of ownedRoots){const resolvedPath=path.resolve(root,value);if(['.multica/daemon_task_context.json','.multica/project/resources.json'].some(file=>resolvedPath===path.resolve(root,file))){masks.push({rawPath:rawCommand.slice(token.start,token.end),resolvedPath,start:token.start,end:token.end});break;}}
  }
  let view=rawCommand;for(const m of [...masks].reverse())view=view.slice(0,m.start)+'OWN_PUBLIC_CONTEXT'+view.slice(m.end);
  return result(masks.length?'MASKED':'UNCHANGED',view,masks);
}

// Workspace inspection only: caller/task binding and actual scope evidence stay separate.
// Tokens come from the existing observer tokenizer; this neither executes nor authorizes operations.
export function inspectMulticaWorkspaceScope(tokens, context) {
  const reject=reason=>({status:'REJECT',reason,args:null,explicitWorkspace:null,effectiveScope:'UNVERIFIED'});
  const uuid=/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
  if(!Array.isArray(tokens)||tokens.length>1024||tokens.some(x=>typeof x!=='string'||x.length>65536)||!['multica','/opt/homebrew/bin/multica'].includes(tokens[0])||!uuid.test(context?.expectedWorkspace||'')||!['native','external'].includes(context?.callerKind))return reject('INVALID_INVOCATION_OR_CONTEXT');
  const args=[];let explicitWorkspace=null,prefix=true;
  for(let i=1;i<tokens.length;i++){
    const token=tokens[i];
    if(/^(?:--profile|--server-url)(?:=|$)/.test(token)||/^MULTICA_[A-Z_]+=/.test(token))return reject('SCOPE_OVERRIDE');
    if(token==='--workspace-id'||token.startsWith('--workspace-id=')){
      if(explicitWorkspace!==null)return reject('DUPLICATE_WORKSPACE_FLAG');
      if(!prefix)return reject('METHOD_UNSUPPORTED_WORKSPACE_FLAG_POSITION');
      explicitWorkspace=token==='--workspace-id'?tokens[++i]:token.slice('--workspace-id='.length);
      if(!uuid.test(explicitWorkspace||'')||explicitWorkspace!==context.expectedWorkspace)return reject('EXPLICIT_WORKSPACE_MISMATCH');
    }else {prefix=false;args.push(token);}
  }
  if(context.callerKind==='native'&&context.runtimeWorkspace!==context.expectedWorkspace)return reject('NATIVE_RUNTIME_BINDING_MISSING_OR_MISMATCH');
  if(context.observedWorkspace!==undefined&&context.observedWorkspace!==null&&context.observedWorkspace!==context.expectedWorkspace)return reject('OBSERVED_WORKSPACE_MISMATCH');
  if(context.callerKind==='external'&&explicitWorkspace===null)return reject('EXTERNAL_EXPLICIT_WORKSPACE_REQUIRED');
  return {status:explicitWorkspace===null?'ALLOW_RUNTIME_CONTEXT':'ALLOW_EXPLICIT_SCOPE',reason:null,args,explicitWorkspace,effectiveScope:context.observedWorkspace===context.expectedWorkspace?'OBSERVED_MATCH':'UNVERIFIED'};
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

// Bounded public member-read classification, separate from all operation/actor/path grants.
// The bound squad comes from reviewed public evidence; member IDs never imply mentions/receipt.
export function inspectSupportedSquadMemberRead(tokens, context) {
  const workspace=inspectMulticaWorkspaceScope(tokens,context);
  if(workspace.status==='REJECT')return {status:workspace.reason==='METHOD_UNSUPPORTED_WORKSPACE_FLAG_POSITION'?'METHOD_UNSUPPORTED':'SCOPE_REJECT',reason:workspace.reason,workspace,effect:'UNVERIFIED'};
  const unsupported=reason=>({status:'METHOD_UNSUPPORTED',reason,workspace,effect:'UNVERIFIED'});
  const uuid=/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
  if(!uuid.test(context?.approvedSquad||''))return unsupported('BOUND_SQUAD_REQUIRED');
  const args=workspace.args;
  if(args[0]!=='squad'||args[1]!=='member'||args[2]!=='list')return unsupported('NOT_SUPPORTED_MEMBER_READ');
  const tail=args.slice(3);
  const help=tail.length===1&&tail[0]==='--help'||tail.length===2&&tail[0]===context.approvedSquad&&tail[1]==='--help';
  const json=tail.length===3&&tail[0]===context.approvedSquad&&tail[1]==='--output'&&tail[2]==='json';
  if(!help&&!json)return unsupported('UNCLASSIFIED_MEMBER_READ_FORM');
  return {status:'MATCH_PUBLIC_MEMBER_READ',reason:null,workspace,approvedSquad:context.approvedSquad,help,mentionBinding:'NOT_PROVIDED_BY_THIS_READ',leaderReceipt:'NOT_PROVIDED_BY_THIS_READ',effect:'UNVERIFIED'};
}

// Pure observation of the existing same-issue dispatch contract. Request
// eligibility does not fabricate an ACK; public comment/run binding comes later.
// The surrounding observer still owns paths, commands, gates and all run budgets.
export function inspectSameIssueCoderDelegation(body, sourceRun, context, evidence={}) {
  const result=(status,reason=null,extra={})=>({status,reason,execution:'UNVERIFIED',completion:'NOT_DEMONSTRATED',functionalAcceptance:'UNVERIFIED',...extra});
  const uuid=/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
  if(typeof body!=='string'||Buffer.byteLength(body)>1048576||!context||!evidence||typeof evidence!=='object'||Array.isArray(evidence)||['workspace','runtime','issue','orchestrator','coder'].some(k=>!uuid.test(context[k]||''))||!['request','bound'].includes(evidence.stage||'request'))return result('METHOD_UNSUPPORTED','INVALID_DELEGATION_INPUT');
  for(const [field,key] of [['agent_id','orchestrator'],['issue_id','issue'],['workspace_id','workspace'],['runtime_id','runtime']])if(sourceRun?.[field]!=null&&sourceRun[field]!==context[key])return result('SCOPE_REJECT','DELEGATING_'+field.toUpperCase()+'_MISMATCH');
  if(!uuid.test(sourceRun?.id||'')||['agent_id','issue_id','workspace_id','runtime_id'].some(k=>sourceRun[k]==null))return result('PENDING_REQUEST_BINDING','SOURCE_RUN_BINDING_MISSING');
  const mentions=[...body.matchAll(/mention:\/\/([^/\s]+)\/([^\s)'"\]>]+)/g)];
  if(mentions.length!==1||mentions[0][1]!=='agent'||mentions[0][2]!==context.coder)return result('SCOPE_REJECT','EXACT_SINGLE_CODER_MENTION_REQUIRED');
  if(evidence.coderRuns!==undefined){
    if(!Array.isArray(evidence.coderRuns)||evidence.coderRuns.some(r=>!uuid.test(r?.id||'')))return result('METHOD_UNSUPPORTED','CODER_RUN_LIST_UNCLASSIFIABLE');
    if(new Set(evidence.coderRuns.filter(r=>r.agent_id===context.coder).map(r=>r.id)).size>1)return result('SCOPE_REJECT','EXCESS_CODER_RUNS');
  }
  if(evidence.priorRequestKey!=null&&evidence.priorRequestKey!==evidence.requestKey)return result('SCOPE_REJECT','REPEAT_SAME_ISSUE_ACTIVATION');
  const eligible={sourceRun:sourceRun.id,issue:context.issue,coder:context.coder};
  if((evidence.stage||'request')==='request')return result('MATCH_SAME_ISSUE_DELEGATION_REQUEST',null,eligible);
  const comment=evidence.comment,run=evidence.run;
  if(!comment||!run)return result('PENDING_PUBLIC_DELEGATION_EVIDENCE','PUBLIC_COMMENT_OR_RUN_MISSING',eligible);
  for(const [object,field,expected] of [
    [comment,'issue_id',context.issue],[comment,'author_id',context.orchestrator],[comment,'author_type','agent'],[comment,'source_task_id',sourceRun.id],
    [run,'agent_id',context.coder],[run,'issue_id',context.issue],[run,'workspace_id',context.workspace],[run,'runtime_id',context.runtime],[run,'kind','comment']
  ])if(object[field]!=null&&object[field]!==expected)return result('SCOPE_REJECT','PUBLIC_'+field.toUpperCase()+'_MISMATCH',eligible);
  if(comment.content!=null&&typeof comment.content!=='string')return result('METHOD_UNSUPPORTED','PUBLIC_COMMENT_BODY_UNCLASSIFIABLE',eligible);
  if(typeof comment.content==='string'&&comment.content!==body&&comment.content!==body.replace(/\n$/,''))return result('SCOPE_REJECT','PUBLIC_COMMENT_BODY_MISMATCH',eligible);
  if(run.trigger_comment_id!=null&&comment.id!=null&&run.trigger_comment_id!==comment.id)return result('SCOPE_REJECT','PUBLIC_TRIGGER_COMMENT_MISMATCH',eligible);
  if(run.attribution?.delegated_from_task_id!=null&&run.attribution.delegated_from_task_id!==sourceRun.id)return result('SCOPE_REJECT','PUBLIC_DELEGATED_FROM_MISMATCH',eligible);
  if(run.attribution?.evidence&&(run.attribution.evidence.kind!=='comment'||run.attribution.evidence.ref_id!==comment.id))return result('SCOPE_REJECT','PUBLIC_ATTRIBUTION_EVIDENCE_MISMATCH',eligible);
  if(!uuid.test(comment.id||'')||!uuid.test(run.id||'')||run.id===sourceRun.id||['issue_id','author_id','author_type','source_task_id','content'].some(k=>comment[k]==null)||['agent_id','issue_id','workspace_id','runtime_id','kind','trigger_comment_id'].some(k=>run[k]==null)||!run.attribution?.delegated_from_task_id||!run.attribution?.evidence)return result('PENDING_PUBLIC_DELEGATION_EVIDENCE','PUBLIC_BINDING_INCOMPLETE',eligible);
  return result('MATCH_SAME_ISSUE_CODER_RUN',null,{...eligible,comment:comment.id,run:run.id,execution:'PUBLIC_BINDING_ONLY',completion:run.status==='completed'&&typeof run.started_at==='string'&&Number.isFinite(Date.parse(run.started_at))&&run.error==null?'COMPLETED_RUN_ONLY':'NOT_DEMONSTRATED'});
}

// Directory metadata is not a filesystem grant or proof of each tool's cwd.
// Squad leaders skip the in_place project assignment; workers retain it.
export function inspectNativeDirectoryBinding(run, context) {
  const out=(status,reason=null,extra={})=>({status,reason,actualToolCwd:'UNVERIFIED',filesystemAuthority:'UNCHANGED_PROJECT_ROOT_ONLY',...extra});
  const uuid=/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
  const canonical=p=>typeof p==='string'&&p.startsWith('/')&&path.posix.normalize(p)===p&&!/[~$\\\r\n]/.test(p)&&!p.split('/').some(s=>['.','..','.multica','.kimi-code','.codex','.ssh','.aws','credentials','sessions','daemon'].includes(s));
  if(!context||!run||['workspace','runtime','issue','project','resourceId','daemon','orchestrator','coder'].some(k=>!uuid.test(context[k]||''))||!canonical(context.physical)||!canonical(context.logical))return out('METHOD_UNSUPPORTED','INVALID_DIRECTORY_CONTEXT');
  for(const [field,key] of [['workspace_id','workspace'],['runtime_id','runtime'],['issue_id','issue'],['project_id','project']])if(run[field]!=null&&run[field]!==context[key])return out('SCOPE_REJECT','DIRECTORY_'+field.toUpperCase()+'_MISMATCH');
  if(run.agent_id!=null&&![context.orchestrator,context.coder].includes(run.agent_id))return out('SCOPE_REJECT','DIRECTORY_ACTOR_MISMATCH');
  if(!uuid.test(run.id||'')||['workspace_id','runtime_id','issue_id','agent_id'].some(k=>run[k]==null))return out('PENDING_DIRECTORY_EVIDENCE','RUN_IDENTITY_MISSING');
  const resource=context.resource,ref=resource?.resource_ref;
  if(!resource||!ref)return out('PENDING_DIRECTORY_EVIDENCE','RESOURCE_BINDING_MISSING');
  if(resource.id!==context.resourceId||resource.workspace_id!==context.workspace||resource.project_id!==context.project||resource.resource_type!=='local_directory'||ref.daemon_id!==context.daemon||ref.execution_mode!=='in_place'||ref.local_path!==context.physical)return out('SCOPE_REJECT','PROJECT_RESOURCE_DIRECTORY_MISMATCH');
  if(run.work_dir!=null&&run.result?.work_dir!=null&&run.work_dir!==run.result.work_dir)return out('SCOPE_REJECT','CONFLICTING_REPORTED_DIRECTORY');
  const reported=run.work_dir??run.result?.work_dir;
  if(reported==null||reported==='')return out('PENDING_DIRECTORY_EVIDENCE','PUBLIC_WORK_DIR_MISSING');
  if(!canonical(reported))return out('SCOPE_REJECT','NONCANONICAL_OR_PRIVATE_DIRECTORY');
  if(run.agent_id===context.coder){
    if(run.is_leader_task===true)return out('SCOPE_REJECT','SPECIALIST_LEADER_CONFLICT');
    if(![context.physical,context.logical].includes(reported))return out('SCOPE_REJECT','SPECIALIST_PROJECT_DIRECTORY_MISMATCH');
    return out('MATCH_PROJECT_DIRECTORY_METADATA',null,{run:run.id,reportedWorkDir:reported});
  }
  if(run.is_leader_task==null)return out('PENDING_DIRECTORY_EVIDENCE','PUBLIC_LEADER_BINDING_MISSING');
  if(run.is_leader_task!==true)return out('SCOPE_REJECT','COORDINATOR_LEADER_REQUIRED');
  const source=context.coordinatorRootSource,parent=context.coordinatorPublicParent;
  if(source&&[['agent_id','orchestrator'],['workspace_id','workspace'],['runtime_id','runtime']].some(([field,key])=>source[field]!=null&&source[field]!==context[key]))return out('SCOPE_REJECT','PUBLIC_COORDINATOR_PREFIX_PROVENANCE_MISMATCH');
  if(parent!=null&&(!canonical(parent)||!parent.endsWith('-'+context.workspace.slice(-12))))return out('SCOPE_REJECT','PUBLIC_COORDINATOR_PREFIX_PROVENANCE_MISMATCH');
  if(!source||!parent||!uuid.test(source.id||'')||!uuid.test(source.issue_id||'')||!source.identifier)return out('PENDING_DIRECTORY_EVIDENCE','PUBLIC_COORDINATOR_PREFIX_PROVENANCE_MISSING');
  if(source.agent_id!==context.orchestrator||source.workspace_id!==context.workspace||source.runtime_id!==context.runtime||source.is_leader_task!==true||!parent.endsWith('-'+context.workspace.slice(-12))||source.work_dir!==parent+'/'+source.identifier.toLowerCase()+'-'+source.id.slice(-12)+'/workdir')return out('SCOPE_REJECT','PUBLIC_COORDINATOR_PREFIX_PROVENANCE_MISMATCH');
  const rows=context.coordinatorRuns;
  if(!/^[A-Z][A-Z0-9]*-[1-9][0-9]*$/.test(context.identifier||'')||!Array.isArray(rows)||rows.length<1||rows.length>2||new Set(rows.map(r=>r?.id)).size!==rows.length||rows.some(r=>!uuid.test(r?.id||'')||r.agent_id!==context.orchestrator||r.issue_id!==context.issue||r.workspace_id!==context.workspace||r.runtime_id!==context.runtime)||!rows.some(r=>r.id===run.id))return out('SCOPE_REJECT','CURRENT_COORDINATOR_RUN_BINDING_REQUIRED');
  const allowed=rows.map(r=>parent+'/'+context.identifier.toLowerCase()+'-'+r.id.slice(-12)+'/workdir');
  if(!allowed.includes(reported))return out('SCOPE_REJECT','COORDINATOR_CASE_RUN_DIRECTORY_MISMATCH');
  return out('MATCH_COORDINATOR_DIRECTORY_METADATA',null,{run:run.id,reportedWorkDir:reported,metadataOnly:true});
}
